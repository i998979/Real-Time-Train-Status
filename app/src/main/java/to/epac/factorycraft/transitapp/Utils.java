package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Utils {

    private static Context context;

    public static void init(Context context) {
        Utils.context = context.getApplicationContext();
    }


    /**
     * Convert internal ID into station codes, e.g. 14 to LMC, 7 to RAC, 131 to LHP
     *
     * @param line Line of the station
     * @param id   ID to convert
     * @return Code of the station
     */
    public static String idToCode(String line, int id) {
        LineConfig config = LineConfig.get(context, line);

        if (line.equalsIgnoreCase("eal")) {
            if (id == 14) return "LMC";
            if (id == 7) return "RAC";
        }
        if (line.equalsIgnoreCase("tkl")) {
            if (id == 131) return "LHP";
        }

        for (int i = 0; i < config.stationIDs.length; i++) {
            if (config.stationIDs[i] == id)
                return config.stationCodes[i];
        }

        return String.valueOf(id);
    }

    /**
     * Convert station codes to internal ID, e.g. LMC to 14, RAC to 7, LHP to 131
     *
     * @param line Line of the station
     * @param code Code to convert
     * @return ID of the station
     */
    public static int codeToId(String line, String code) {
        LineConfig config = LineConfig.get(context, line);

        if (line.equalsIgnoreCase("eal")) {
            if (code.equalsIgnoreCase("LMC")) return 14;
            if (code.equalsIgnoreCase("RAC")) return 7;
        }
        if (line.equalsIgnoreCase("tkl")) {
            if (code.equalsIgnoreCase("LHP")) return 131;
        }

        for (int i = 0; i < config.stationCodes.length; i++) {
            if (config.stationCodes[i].equalsIgnoreCase(code))
                return config.stationIDs[i];
        }

        return -1;
    }

    public static String getStationName(String code) {
        return getStationName(code, false);
    }

    public static String getStationName(String code, boolean zhhk) {
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

    public static long convertTimestampToMillis(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

            Date date = sdf.parse(timeStr);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    public static int getThemeColor(Context ctx, int attr) {
        TypedValue typedValue = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, typedValue, true);

        return typedValue.data;
    }

    public static int dpToPx(int dp) {
        return Math.round((float) dp * context.getResources().getDisplayMetrics().density);
    }
}
