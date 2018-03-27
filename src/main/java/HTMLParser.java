import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class HTMLParser extends Thread {

    private static String adminTableId;
    private static final String BASE_URI = "https://www.avito.ru";
    private static final String MOBILE_URI = "https://m.avito.ru";
    private static final String PATH_TO_RECOURCES = "src/main/resources";
    private long start;
    private static Properties headers;
    private SpreedsheetService service;

    public HTMLParser(Properties config) {
        headers = new Properties();
        try {
            headers.load(new FileInputStream(PATH_TO_RECOURCES + "/headers"));
        } catch (IOException e) {
            System.out.println(e + " /load properties");
        }
        String adminTableUri = config.getProperty("admin.table");
        adminTableId = adminTableUri.substring(adminTableUri.indexOf("spreadsheets/d/") + 15, adminTableUri.indexOf("/edit"));
        service = new SpreedsheetService();
    }

    public void parse() {
        System.out.println("\nApplication is running. Please do not close this window until the end of work!".toUpperCase());
        start = new Date().getTime();
        ConcurrentHashMap<String, String> categoriesTables = new ConcurrentHashMap<>();
        try {
            categoriesTables = service.getCategories(adminTableId);
        } catch (IOException e) {
            connectTimeout();
            parse();
        }
        if (categoriesTables == null) {
            System.out.println("NO DATA FOR PARSING");
            return;
        }
        for (ConcurrentHashMap.Entry<String, String> entry : categoriesTables.entrySet()) {
            getLinks(entry.getKey(), entry.getValue());
        }
    }

    private void getLinks(String category, String tableUri) {
        if (!category.startsWith("http") || !tableUri.startsWith("http")) return;
        category = category.replace("#/", "/");
        System.out.println("\n");
        String tableId = tableUri.substring(tableUri.indexOf("spreadsheets/d/") + 15, tableUri.indexOf("/edit"));
        List<List<Object>> currentValues = null;
        try {
            currentValues = service.getAllValues(tableId);
            if (currentValues==null) currentValues = new ArrayList<>();
        } catch (IOException e) {
        	connectTimeout();
        	getLinks(category, tableUri);
        }
        Document rawDoc = getDocument(category);
        if (rawDoc.toString().contains("firewall-container")) {
            System.out.println("Access to avito.ru is temporarily blocked");
            return;
        }
        System.out.println(category);
        int last = 1;
        Elements paginationPage = rawDoc.select(".pagination-page");
        	if (paginationPage.last()!=null) {
        		String lastPage = paginationPage.last().attr("href");
        		int from = lastPage.indexOf("?p=")+3;
        		last = Integer.parseInt(lastPage.substring(from, lastPage.indexOf("&", from)));
        		}
        int added = 0;
        int updated = 0;
        int ignored = 0;
        
        for (int i = last; i > 0; i--) {
            Document doc = getDocument(String.format(category + "?p=%d", i));
            Elements links = doc.getElementsByClass("item-description-title-link");
            
            for (int k = links.size() - 1; k >= 0; k--) {
                boolean eq = false;
                List<Object> parsedRow = parse(links.get(k).attr("href"));
                
                for (int pi = 0; pi < currentValues.size(); pi++) {
                    if (currentValues.get(pi).isEmpty()) {currentValues.remove(pi);continue;}
                    if (currentValues.get(pi).get(3).equals(parsedRow.get(3))) {
                        if (currentValues.get(pi).get(4).equals(parsedRow.get(4))) {
                            ignored++;
                            eq = true;
                            break;
                        }
                        currentValues.set(pi, parsedRow);
                        updated++;
                        eq = true;
                        break;
                    }
                }
                if (!eq) {
                    currentValues.add(parsedRow);
                    added++;
                }
                System.out.print("\r");
                System.out.print("Added " + added + " records / ");
                System.out.print("Updated " + updated + " records / ");
                System.out.print("Ignored " + ignored + " records ");
            }
        }
        System.out.println("\n");
        saveResult(tableId, currentValues);
        clearTable(tableId, currentValues.size());
    }

    private boolean clearTable(String tableId, int size) {
        try {
            service.clearTable(tableId, size);
        } catch (IOException e) {
            connectTimeout();
            return clearTable(tableId, size);
        }
        return true;
    }

    private boolean saveResult(String tableId, List<List<Object>> currentValues) {
		try {
//            service.appendingValues(tableId, "A2", currentValues, "RAW");
            service.writeSingleRange(tableId, "A2", currentValues, "RAW");
            System.out.print("SUCCESS! \n New table size " + currentValues.size() + " records ");
            System.out.println(new Date().getTime() - start + " milliseconds\n");
        } catch (IOException e) {
            System.out.println(e + "\nERROR SAVING TO SPREEDSHEET");
            connectTimeout();
            return saveResult(tableId, currentValues);
        }
        return true;
	}

    private List<Object> parse(String link) {
        Document doc = getDocument(MOBILE_URI + link);
        if (doc.text().contains("firewall-container")) {
            System.out.println("Access to avito.ru is temporarily blocked");
            Thread.currentThread().interrupt();
        }
        List<Object> row = new ArrayList<>();
//        row.add(convertDate(doc.select(".item-add-date").text()));
//        row.add(doc.select(".text.text-main").text() + doc.select(".single-item-header").text());
        row.add(BASE_URI + link);
        row.add(doc.select(".avito-address-text").text());
        String price = doc.select(".info-price").text();
        if (price != null) {
        	price = convertOnlyDigit(price);
        }
        row.add(price);
//        row.add(doc.select(".description-preview").text());
        String phoneRef = doc.select(".action-show-number").attr("href");
        String phone = "";
        if (phoneRef != null && phoneRef.contains("phone")) {
            phone = getPhone(link, phoneRef.substring(phoneRef.indexOf("phone") + 6));
            phone = convertOnlyDigit(phone).replaceFirst("7", "8");
        } else {
            System.out.println("An error occurred while parsing the phone number");
        }
        row.add(phone);
        row.add(convertDate(doc.select(".item-add-date").text()));
        row.add(doc.select(".description-preview").text());
        return row;
    }

    private Document getDocument(String link) {
        Document document;
        try {
            document = Jsoup.connect(link)
            		.header("Upgrade-Insecure-Requests", headers.getProperty("Upgrade-Insecure-Requests"))
                    .header("User-Agent", headers.getProperty("User-Agent"))
                    .header("Accept", headers.getProperty("Accept"))
                    .header("Referer", headers.getProperty("Referer")).header("Accept-Encoding", headers.getProperty("Accept-Encoding"))
                    .header("Accept-Language", headers.getProperty("Accept-Language")).header("Cookie", headers.getProperty("Cookie"))
                    .get();
        } catch (IOException e) {
            connectTimeout();
            return getDocument(link);
        }
        return document;
    }


    private String getResponse(InputStream is) {
        try {
            try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                return result.toString("UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        parse();
    }

    private String getPhone(String ref, String phoneId) {
        String uri = MOBILE_URI + ref;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(uri + "/phone/" + phoneId + "?async").openConnection();
        } catch (IOException e) {
            connectTimeout();
            return getPhone(ref, phoneId);
        }
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            System.out.println(e);
        }
        connection.addRequestProperty("Upgrade-Insecure-Requests", headers.getProperty("Upgrade-Insecure-Requests"));
        connection.addRequestProperty("User-Agent", headers.getProperty("User-Agent"));
        connection.addRequestProperty("Accept", headers.getProperty("Accept"));
        connection.addRequestProperty("Referer", headers.getProperty("Referer"));
        connection.addRequestProperty("Accept-Language", headers.getProperty("Accept-Language"));
        connection.addRequestProperty("Cookie", headers.getProperty("Cookie"));
        String json = null;
        try {
            json = getResponse(connection.getInputStream());
        } catch (IOException e) {
            System.out.println("\n" + uri + " /getPhone()");
        }
        if (json == null || !json.contains("phone"))
            return null;
        return new Gson().fromJson(json, JsonElement.class).getAsJsonObject().get("phone").toString();
    }

    private String convertDate(String rawDate) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        return rawDate.replace("Размещено ", "")
                .replace("сегодня", String.format("%02d", (today.get(Calendar.DATE))) + "." + String.format("%02d", (today.get(Calendar.MONTH) + 1)))
                .replace("вчера", String.format("%02d", (yesterday.get(Calendar.DATE))) + "." + String.format("%02d", (yesterday.get(Calendar.MONTH) + 1)))
                .replace(" в", "")
                .replace(" января", ".01 ")
                .replace(" февраля", ".02 ")
                .replace(" марта", ".03 ")
                .replace(" апреля", ".04 ")
                .replace(" мая", ".05 ")
                .replace(" июня", ".06 ")
                .replace(" июля", ".07 ")
                .replace(" августа", ".08 ")
                .replace(" сентября", ".09 ")
                .replace(" октября", ".10 ")
                .replace(" ноября", ".11 ")
                .replace(" декабря", ".12 ")
                .trim();
    }
    
    private static String convertOnlyDigit( CharSequence input ) {
	    if (input == null)
	        return null;
	    if ( input.length() == 0 )
	        return "";
	    char[] result = new char[input.length()];
	    int cursor = 0;
	    CharBuffer buffer = CharBuffer.wrap( input );
	    while ( buffer.hasRemaining() ) {
	        char chr = buffer.get();
	        if ( chr > 47 && chr < 58 )
	            result[cursor++] = chr;
	    }
	    return new String( result, 0, cursor );
	}
    
	private void connectTimeout() {
		for (int i = 30; i >= 0; i--) {
			System.out.print("\r");
			System.out.print("Could not connect. Retrying through ");
			System.out.print(i);
			System.out.print(" seconds                            ");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		System.out.println("");
	}
}
