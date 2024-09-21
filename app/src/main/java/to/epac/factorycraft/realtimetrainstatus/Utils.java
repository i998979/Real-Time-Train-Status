package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Utils {

    public static String mapStation(int code, String line) {
        // TML
        if (line.equals("TML")) {
            switch (code) {
                case 1:
                    return "HUH";
                case 14:
                    return "ETS";
                case 21:
                    return "TAW";
                case 22:
                    return "CKT";
                case 23:
                    return "STW";
                case 24:
                    return "CIO";
                case 25:
                    return "SHM";
                case 26:
                    return "TSH";
                case 27:
                    return "HEO";
                case 28:
                    return "MOS";
                case 29:
                    return "WKS";
                case 41:
                    return "NAC";
                case 42:
                    return "MEF";
                case 43:
                    return "TWW";
                case 44:
                    return "KSR";
                case 45:
                    return "YUL";
                case 46:
                    return "LOP";
                case 47:
                    return "TIS";
                case 48:
                    return "SIH";
                case 49:
                    return "TUM";
                case 50:
                    return "AUS";
                case 61:
                    return "HOM";
                case 62:
                    return "TKW";
                case 63:
                    return "SUW";
                case 64:
                    return "KAT";
                case 65:
                    return "DIH";
                case 66:
                    return "HIK";
                case 91:
                    return "DEP";
                case 92:
                    return "OUT";
                case 93:
                    return "SPC";
                case 94:
                    return "TES";
                case 95:
                    return "WR";
                case 97:
                    return "MOL";
                default:
                    return String.valueOf(code);
            }
        }
        // EAL
        else if (line.equals("EAL")) {
            switch (code) {
                case 0:
                    return "HTD";
                case 15:
                    return "SSG_S";
                case 151:
                    return "SSG_N";
                case 16:
                    return "LWM_S";
                case 161:
                    return "LWM_N";
                case 17:
                    return "S1";
                case 701:
                    return "HTD";
                case 901:
                    return "HTD_N";
                case 24:
                    return "ADT";
                case 241:
                    return "ADT_1";

                case 1:
                    return "HUH_1";
                case 2:
                    return "MKK";
                case 3:
                    return "KOT";
                case 4:
                    return "TAW";
                case 5:
                    return "SHT";
                case 6:
                    return "FOT";
                case 7:
                    return "RAC";
                case 8:
                    return "UNI";
                case 9:
                    return "TAP";
                case 10:
                    return "TWO";
                case 11:
                    return "FAN";
                case 12:
                    return "SHS";
                case 13:
                    return "LOW";
                case 14:
                    return "LMC";
                case 21:
                    return "HUH";
                case 22:
                    return "EXC";
                case 23:
                    return "ADM";
                case 91:
                    return "HTD";
                case 92:
                    return "HTD_1";
                case 81:
                    return "TEST";
                case 82:
                    return "TEST_1";
                default:
                    return String.valueOf(code);
            }
        }
        return String.valueOf(code);
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

    public static String getStationName(Activity context, String code) {
        String[] stations = Arrays.stream((context.getResources().getString(R.string.eal_stations) + " "
                        + context.getResources().getString(R.string.tml_stations) + " "
                        + context.getResources().getString(R.string.ktl_stations) + " "
                        + context.getResources().getString(R.string.ael_stations) + " "
                        + context.getResources().getString(R.string.drl_stations) + " "
                        + context.getResources().getString(R.string.isl_stations) + " "
                        + context.getResources().getString(R.string.tcl_stations) + " "
                        + context.getResources().getString(R.string.tkl_stations) + " "
                        + context.getResources().getString(R.string.twl_stations) + " "
                        + context.getResources().getString(R.string.sil_stations)).split(" "))
                .distinct().toArray(String[]::new);

        String[] stations_long = Arrays.stream((context.getResources().getString(R.string.eal_stations_long) + ";"
                        + context.getResources().getString(R.string.tml_stations_long) + ";"
                        + context.getResources().getString(R.string.ktl_stations_long) + ";"
                        + context.getResources().getString(R.string.ael_stations_long) + ";"
                        + context.getResources().getString(R.string.drl_stations_long) + ";"
                        + context.getResources().getString(R.string.isl_stations_long) + ";"
                        + context.getResources().getString(R.string.tcl_stations_long) + ";"
                        + context.getResources().getString(R.string.tkl_stations_long) + ";"
                        + context.getResources().getString(R.string.twl_stations_long) + ";"
                        + context.getResources().getString(R.string.sil_stations_long)).split(";"))
                .distinct().toArray(String[]::new);

        int index = IntStream.range(0, stations.length)
                .filter(i -> stations[i].equalsIgnoreCase(code))
                .findFirst().orElse(Integer.MAX_VALUE);

        return Arrays.stream(stations_long).skip(index).findFirst().orElse(code);
    }

    public static String getColor(Activity context, String line) {
        int code = context.getResources().getIdentifier(line, "color", context.getPackageName());
        return "#" + Integer.toHexString(ContextCompat.getColor(context, code));
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
