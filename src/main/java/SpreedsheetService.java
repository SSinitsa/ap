import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SpreedsheetService {

    private static Sheets service;

    static {
        try {
            service = SheetsServiceUtil.getSheets();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public ValueRange readSingleRange(String spreadsheetId, String range) throws IOException {
    	return service.spreadsheets().values().get(spreadsheetId, range).execute();
    }

    public BatchGetValuesResponse readMultipleRanges(String spreadsheetId, List<String> ranges) throws IOException {
        return service.spreadsheets().values().batchGet(spreadsheetId)
                .setRanges(ranges).execute();
    }

    public UpdateValuesResponse writeSingleRange(String spreadsheetId, String range, List<List<Object>> values, String valueInputOption) throws IOException {
        ValueRange body = new ValueRange().setValues(values);
        UpdateValuesResponse response = service.spreadsheets().values().update(spreadsheetId, range, body)
                .setValueInputOption(valueInputOption)
                .execute();
        return response;
    }

    public BatchUpdateValuesResponse writeMultipleRanges(String spreadsheetId, List<ValueRange> data, String valueInputOption) throws IOException {
        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption(valueInputOption)
                .setData(data);
        return service.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
    }

    public AppendValuesResponse appendingValues(String spreadsheetId, String range, List<List<Object>> values, String valueInputOption) throws IOException {
        ValueRange body = new ValueRange()
                .setValues(values);
        return service.spreadsheets().values().append(spreadsheetId, range, body)
                .setValueInputOption(valueInputOption)
                .execute();
    }


    public TreeMap<String, String> getCategories(String adminTableId) throws IOException {
    	TreeMap<String, String> result = new TreeMap<>();
        List<List<Object>> lists = readSingleRange(adminTableId, "A2:B").getValues();
        for (List<Object> list : lists) {
            result.put((String) list.get(1), (String) list.get(0));
        }
        return result;
    }

    public List<List<Object>> getAllValues(String tableId) throws IOException {
        ValueRange values = readSingleRange(tableId, "A2:F");
        return values.getValues();
    }

    public void clearTable (String tableId, int tableSize) throws IOException {
        try {
			service.spreadsheets().values().clear(tableId, "A"+(tableSize+2)+":F", new ClearValuesRequest()).execute();
		} catch (IOException e) {
			System.out.println("Append " + tableSize*2 + " rows...");
			appendingValues(tableId, "A"+(tableSize+2)+":F", Collections.nCopies(tableSize*2, Collections.nCopies(6, "")), "RAW");
			clearTable(tableId, tableSize);
		}
    }
}
