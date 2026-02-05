package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static String idToCode(Context context, int code, String line) {
        try {
            int idRes = context.getResources().getIdentifier(line.toLowerCase() + "_station_id", "string", context.getPackageName());
            int codeRes = context.getResources().getIdentifier(line.toLowerCase() + "_station_code", "string", context.getPackageName());

            String[] ids = context.getString(idRes).split("\\s+");
            String[] codes = context.getString(codeRes).split("\\s+");

            for (int i = 0; i < ids.length; i++) {
                if (Integer.parseInt(ids[i]) == code) {
                    return codes[i];
                }
            }
        } catch (Exception e) {
        }
        return String.valueOf(code);
    }

    public static int codeToId(Context context, String line, String code) {
        try {
            int idRes = context.getResources().getIdentifier(line.toLowerCase() + "_station_id", "string", context.getPackageName());
            int codeRes = context.getResources().getIdentifier(line.toLowerCase() + "_station_code", "string", context.getPackageName());

            String[] ids = context.getString(idRes).split("\\s+");
            String[] codes = context.getString(codeRes).split("\\s+");

            for (int i = 0; i < codes.length; i++) {
                if (codes[i].equalsIgnoreCase(code)) {
                    return Integer.parseInt(ids[i]);
                }
            }
        } catch (Exception e) {
        }
        return -1;
    }

    public static String getStationName(Context context, String code) {
        return getStationName(context, code, false);
    }

    public static String getStationName(Context context, String code, boolean zhhk) {
        if (code == null || code.isEmpty()) return "";

        int[] shortRes = {R.string.eal_stations, R.string.tml_stations, R.string.ktl_stations, R.string.ael_stations, R.string.drl_stations, R.string.isl_stations, R.string.tcl_stations, R.string.tkl_stations, R.string.twl_stations, R.string.sil_stations};
        int[] longRes = zhhk ? new int[]{R.string.eal_stations_long_zh, R.string.tml_stations_long_zh, R.string.ktl_stations_long_zh, R.string.ael_stations_long_zh, R.string.drl_stations_long_zh, R.string.isl_stations_long_zh, R.string.tcl_stations_long_zh, R.string.tkl_stations_long_zh, R.string.twl_stations_long_zh, R.string.sil_stations_long_zh}
                : new int[]{R.string.eal_stations_long, R.string.tml_stations_long, R.string.ktl_stations_long, R.string.ael_stations_long, R.string.drl_stations_long, R.string.isl_stations_long, R.string.tcl_stations_long, R.string.tkl_stations_long, R.string.twl_stations_long, R.string.sil_stations_long};

        Map<String, String> nameMap = new HashMap<>();
        for (int i = 0; i < shortRes.length; i++) {
            String[] codes = context.getString(shortRes[i]).split(" ");
            String[] names = context.getString(longRes[i]).split(";");

            for (int j = 0; j < Math.min(codes.length, names.length); j++) {
                nameMap.put(codes[j].toUpperCase(), names[j]);
            }
        }

        return nameMap.getOrDefault(code.toUpperCase(), code);
    }

    public static String getLineName(String code) {
        return getLineName(code, false);
    }

    public static String getLineName(String code, boolean zhhk) {
        switch (code.toLowerCase()) {
            case "eal":
            case "nsl":
            case "erl":
                return zhhk ? "東鐵綫" : "East Rail Line";
            case "tml":
            case "ewl":
                return zhhk ? "屯馬綫" : "Tuen Ma Line";
            case "ktl":
                return zhhk ? "觀塘綫" : "Kwun Tong Line";
            case "ael":
                return zhhk ? "機場快綫" : "Airport Express Line";
            case "drl":
                return zhhk ? "迪士尼綫" : "Disneyland Resort Line";
            case "isl":
                return zhhk ? "港島綫" : "Island Line";
            case "tcl":
                return zhhk ? "東涌綫" : "Tung Chung Line";
            case "tkl":
                return zhhk ? "將軍澳綫" : "Tseung Kwan O Line";
            case "twl":
                return zhhk ? "荃灣綫" : "Tsuen Wan Line";
            case "sil":
                return zhhk ? "南港島綫" : "South Island Line";
            default:
                return code.toUpperCase();
        }
    }

    public static String getColor(Context context, String line) {
        if (line.equalsIgnoreCase("nsl") || line.equalsIgnoreCase("erl")) line = "eal";
        if (line.equalsIgnoreCase("ewl")) line = "tml";

        int code = context.getResources().getIdentifier(line.toLowerCase(), "color", context.getPackageName());
        return "#" + Integer.toHexString(ContextCompat.getColor(context, code));
    }

    public static long convertTimestampToMillis(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;
        try {
            // Open Data 格式: "2026-02-04 23:51:02"
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

            // 關鍵：強制指定為 GMT+8 (香港時間)
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));

            java.util.Date date = sdf.parse(timeStr);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            Log.e("Utils", "Time parse error: " + timeStr);
            return 0;
        }
    }

    public static int covertStationOrder(int code) {
        switch (code) {
            case 1:
                return 16;
            case 14:
                return 17;
            case 21:
                return 9;
            case 22:
                return 8;
            case 23:
                return 7;
            case 24:
                return 6;
            case 25:
                return 5;
            case 26:
                return 4;
            case 27:
                return 3;
            case 28:
                return 2;
            case 29:
                return 1;
            case 41:
                return 19;
            case 42:
                return 20;
            case 43:
                return 21;
            case 44:
                return 22;
            case 45:
                return 23;
            case 46:
                return 24;
            case 47:
                return 25;
            case 48:
                return 26;
            case 49:
                return 27;
            case 50:
                return 18;
            case 61:
                return 15;
            case 62:
                return 14;
            case 63:
                return 13;
            case 64:
                return 12;
            case 65:
                return 11;
            case 66:
                return 10;
            default:
                return 0;
        }
    }

    public static List<LatLng> getLatLngs(String s) {
        List<LatLng> latLngs = new ArrayList<>();

        String[] data = s.split(" ");
        for (String s1 : data) {
            latLngs.add(getLatLng(s1));
        }

        return latLngs;
    }

    public static LatLng getLatLng(String s) {
        double lng = Double.parseDouble(s.split(",")[0]);
        double lat = Double.parseDouble(s.split(",")[1]);
        return new LatLng(lat, lng);
    }

    public static boolean isPassengerTrain(String td) {
        List<String> headCode = new ArrayList<>(Arrays.asList("FF", "SS", "VL", "VT", "VV", "VW", "DP", "XX", "TT"));

        return !(headCode.contains(td.substring(0, 2)) || td.equals("UNKNOWN"));
    }
}
