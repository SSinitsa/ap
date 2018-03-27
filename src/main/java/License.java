import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class License {

    static boolean isExpired() {
        URL url = null;
        HttpURLConnection connection = null;
        String license = "1";
        Gson gson = new Gson();
        try {
            url = new URL("https://sheets.googleapis.com/v4/spreadsheets/1INRb6L0y77Z_6ofc4A_zjz9AEus_RRCjuLSPSXNAsEc/values/B1?key=AIzaSyANdj0Zv4bXIn-Hj-L7XR2kCoFWdtSFg_I");
        } catch (MalformedURLException e) {
        	return false;
        }
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
        	return false;
        }
        try {
            if(connection.getResponseCode()!=200) return false;
        } catch (IOException e) {
        }
        try {
            license = getResponse(connection.getInputStream());
        } catch (IOException e) {
        	return false;
        }
        JsonObject resp = gson.fromJson(license, JsonObject.class).getAsJsonObject();
        if (resp == null) return false;
        if (resp.toString().contains("expired")) return true;
        return false;
    }

    private static String getResponse(InputStream is) {
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

}
