package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HRConfig {
    private static HRConfig instance;

    private final Map<String, Line> lineAliasMap = new HashMap<>();
    private final Map<Integer, Line> lineMap = new HashMap<>();
    private final Map<String, Station> aliasMap = new HashMap<>();
    private final Map<Integer, Station> idMap = new HashMap<>();

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