package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static String idToCode(Context context, String line, int id) {
        LineConfig config = LineConfig.get(context, line);

        if (line.equalsIgnoreCase("eal")) {
            if (id == 14) return "LMC";
            if (id == 7) return "RAC";
        }

        if (config.stationIDs == null) return String.valueOf(id);

        for (int i = 0; i < config.stationIDs.length; i++) {
            if (config.stationIDs[i] == id) {
                return config.stationCodes[i];
            }
        }
        return String.valueOf(id);
    }

    public static int codeToId(Context context, String line, String code) {
        LineConfig config = LineConfig.get(context, line);

        if (config.stationCodes == null || code == null) return -1;

        for (int i = 0; i < config.stationCodes.length; i++) {
            if (config.stationCodes[i].equalsIgnoreCase(code)) {
                return config.stationIDs[i];
            }
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


    public static int getThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static int dpToPx(Context context, int dp) {
        return Math.round((float) dp * context.getResources().getDisplayMetrics().density);
    }
}
