package to.epac.factorycraft.transitapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

public class LineConfig {
    private static final HashMap<String, LineConfig> cache = new HashMap<>();

    public int[] stationIDs = new int[0];
    public String[] stationCodes = new String[0];

    public String apiUrl = "";
    public String apiKey = "";

    public HashMap<Integer, Long> runTimeUpMap = new HashMap<>();
    public HashMap<Integer, Long> runTimeDnMap = new HashMap<>();
    public HashMap<Integer, Long> dwellTimeUpMap = new HashMap<>();
    public HashMap<Integer, Long> dwellTimeDnMap = new HashMap<>();

    public static LineConfig get(Context context, String lineCode) {
        String key = lineCode.toLowerCase();

        if (cache.containsKey(key)) return cache.get(key);

        LineConfig config = new LineConfig();
        try {
            InputStream is = context.getResources().openRawResource(R.raw.line_config);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            JSONObject root = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            if (root.has(key)) {
                JSONObject lineJson = root.getJSONObject(key);

                JSONArray idsArray = lineJson.getJSONArray("StationIDs");
                JSONArray codesArray = lineJson.getJSONArray("StationCodes");
                config.stationIDs = new int[idsArray.length()];
                config.stationCodes = new String[codesArray.length()];

                for (int i = 0; i < idsArray.length(); i++) {
                    config.stationIDs[i] = idsArray.getInt(i);
                    config.stationCodes[i] = codesArray.getString(i);
                }

                config.apiUrl = lineJson.optString("ApiUrl", "");
                config.apiKey = lineJson.optString("ApiKey", "");

                parseMap(lineJson, "RunTimeUp", config.runTimeUpMap);
                parseMap(lineJson, "DwellTimeUp", config.dwellTimeUpMap);
                parseMap(lineJson, "RunTimeDn", config.runTimeDnMap);
                parseMap(lineJson, "DwellTimeDn", config.dwellTimeDnMap);

                cache.put(key, config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return config;
    }

    private static void parseMap(JSONObject json, String key, HashMap<Integer, Long> targetMap) {
        if (!json.has(key)) return;
        try {
            JSONObject mapJson = json.getJSONObject(key);
            Iterator<String> keys = mapJson.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                targetMap.put(Integer.parseInt(k), mapJson.getLong(k));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}