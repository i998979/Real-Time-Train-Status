package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HRConfig {
    private static HRConfig instance;

    private final Map<String, Line> lineAliasMap = new LinkedHashMap<>();
    private final Map<Integer, Line> lineMap = new LinkedHashMap<>();
    private final Map<String, Station> aliasMap = new LinkedHashMap<>();
    private final Map<Integer, Station> idMap = new LinkedHashMap<>();

    public static class Line {
        public final int id;
        public final String alias;
        public final String name;
        public final String nameEN;
        public final String color;

        public Line(int id, String alias, String name, String nameEN, String color) {
            this.id = id;
            this.alias = alias;
            this.name = name;
            this.nameEN = nameEN;
            this.color = color;
        }
    }

    public static class Station {
        public final int id;
        public final String alias;
        public final String name;
        public final String nameEN;
        public final String nameSC;
        public final List<Line> lines = new ArrayList<>();

        public Station(int id, String alias, String name, String nameEN, String nameSC) {
            this.id = id;
            this.alias = alias;
            this.name = name;
            this.nameEN = nameEN;
            this.nameSC = nameSC;
        }
    }

    private HRConfig(Context context) {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.hr);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);

            JSONArray linesArray = root.getJSONArray("lines");
            for (int i = 0; i < linesArray.length(); i++) {
                JSONObject l = linesArray.getJSONObject(i);
                int lineId = l.getInt("ID");
                String lineAlias = l.getString("alias");
                Line line = new Line(
                        lineId,
                        lineAlias,
                        l.getString("name"),
                        l.getString("nameEN"),
                        l.getString("color")
                );
                lineMap.put(lineId, line);
                lineAliasMap.put(lineAlias.toUpperCase(), line);
            }

            JSONArray stations = root.getJSONArray("stations");

            for (int i = 0; i < stations.length(); i++) {
                JSONObject s = stations.getJSONObject(i);
                int id = s.getInt("ID");
                String alias = s.getString("alias");

                Station station = new Station(
                        id,
                        alias,
                        s.getString("name"),
                        s.getString("nameEN"),
                        s.getString("nameSC")
                );

                JSONArray lineIDs = s.getJSONArray("lineIDs");
                for (int j = 0; j < lineIDs.length(); j++) {
                    Line line = lineMap.get(lineIDs.getInt(j));
                    if (line != null) {
                        station.lines.add(line);
                    }
                }

                aliasMap.put(alias.toUpperCase(), station);
                idMap.put(id, station);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized HRConfig getInstance(Context context) {
        if (instance == null) {
            instance = new HRConfig(context.getApplicationContext());
        }
        return instance;
    }


    public boolean isTerminus(String lineAlias, int stationId) {
        String alias = getStationAlias(stationId);
        switch (lineAlias) {
            case "EAL": // 東鐵綫：金鐘、羅湖、落馬洲
                return alias.equals("ADM") || alias.equals("LOW") || alias.equals("LMC");
            case "TWL": // 荃灣綫：中環、荃灣
                return alias.equals("CEN") || alias.equals("TSW");
            case "ISL": // 港島綫：堅尼地城、柴灣
                return alias.equals("KET") || alias.equals("CHW");
            case "TML": // 屯馬綫：屯門、烏溪沙
                return alias.equals("TUM") || alias.equals("WKS");
            case "TKL": // 將軍澳綫：北角、寶琳、康城
                return alias.equals("NOP") || alias.equals("POA") || alias.equals("LHP");
            case "TCL": // 東涌綫：香港、東涌
                return alias.equals("HOK") || alias.equals("TUC");
            case "AEL": // 機場快綫：香港、博覽館
                return alias.equals("AEL") || alias.equals("AWE");
            case "SIL": // 南港島綫：金鐘、海怡半島
                return alias.equals("ADM") || alias.equals("SOH");
            case "DRL": // 迪士尼綫：欣澳、迪士尼
                return alias.equals("SUN") || alias.equals("DIS");
            case "KTL": // 觀塘綫：黃埔、調景嶺
                return alias.equals("WHA") || alias.equals("TIK");
            default:
                return false;
        }
    }


    public Map<String, Line> getLineAliasMap() {
        return lineAliasMap;
    }

    public Map<Integer, Line> getLineMap() {
        return lineMap;
    }

    public Map<String, Station> getAliasMap() {
        return aliasMap;
    }

    public Map<Integer, Station> getIdMap() {
        return idMap;
    }

    public List<Line> getAllLines() {
        return new ArrayList<>(lineMap.values());
    }


    public List<Line> getLinesByStationAlias(String alias) {
        Station sta = getStationByAlias(alias);
        return sta != null ? sta.lines : new ArrayList<>();
    }

    public Line getLineByAlias(String alias) {
        return alias == null ? null : lineAliasMap.get(alias.toUpperCase());
    }

    public Line getLineById(int id) {
        return lineMap.get(id);
    }

    public Station getStationByAlias(String alias) {
        return aliasMap.get(alias.toUpperCase());
    }

    public Station getStationById(int id) {
        return idMap.get(id);
    }


    public int getStationId(String alias) {
        Station sta = getStationByAlias(alias);
        return sta != null ? sta.id : 1;
    }

    public String getStationName(String alias) {
        Station sta = getStationByAlias(alias);
        return sta != null ? sta.name : "中環";
    }

    public String getStationNameEN(String alias) {
        Station sta = getStationByAlias(alias);
        return sta != null ? sta.nameEN : "Central";
    }

    public String getStationNameSC(String alias) {
        Station sta = getStationByAlias(alias);
        return sta != null ? sta.nameSC : "中环";
    }


    public String getStationAlias(int id) {
        Station sta = getStationById(id);
        return sta != null ? sta.alias : "CEN";
    }

    public String getStationName(int id) {
        Station sta = getStationById(id);
        return sta != null ? sta.name : "中環";
    }

    public String getStationNameEN(int id) {
        Station sta = getStationById(id);
        return sta != null ? sta.nameEN : "Central";
    }

    public String getStationNameSC(int id) {
        Station sta = getStationById(id);
        return sta != null ? sta.nameSC : "中环";
    }
}