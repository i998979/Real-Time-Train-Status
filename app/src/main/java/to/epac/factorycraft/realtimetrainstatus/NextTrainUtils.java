package to.epac.factorycraft.realtimetrainstatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NextTrainUtils {

    public static List<Train> getTrainData(String data, String line, String station) {
        List<Train> trains = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(data);
            String status = jsonObject.getString("status");
            String message = jsonObject.getString("message");
            String url0 = "";
            try {
                url0 = jsonObject.getString("url");
            } catch (Exception e) {
            }
            String curr_time = jsonObject.getString("curr_time");
            String sys_time = jsonObject.getString("sys_time");
            String isdelay = jsonObject.getString("isdelay");

            JSONObject jsonObject2 = jsonObject.getJSONObject("data");

            JSONObject jsonObject3 = jsonObject2.getJSONObject(line.toUpperCase() + "-" + station.toUpperCase());

            String curr_time2 = jsonObject3.getString("curr_time");
            String sys_time2 = jsonObject3.getString("sys_time");

            JSONArray DIR = null;
            for (int k = 0; k < 2; k++) {
                try {
                    // HUH has no UP trains
                    // TUM has no DN trains, this will result a JSONException and will be caught in catch block
                    if (k == 0) DIR = jsonObject3.getJSONArray("UP");
                    if (k == 1) DIR = jsonObject3.getJSONArray("DOWN");

                    for (int j = 0; j < DIR.length(); j++) {
                        JSONObject jsonObject4 = DIR.getJSONObject(j);
                        String ttnt = jsonObject4.getString("ttnt");
                        String valid = jsonObject4.getString("valid");
                        String plat = jsonObject4.getString("plat");
                        String time = jsonObject4.getString("time");
                        String source = jsonObject4.getString("source");
                        String dest = jsonObject4.getString("dest");
                        String seq = jsonObject4.getString("seq");
                        String timetype = "";
                        try {
                            timetype = jsonObject4.getString("timetype");
                        } catch (Exception e) {
                        }
                        String route = "";
                        try {
                            route = jsonObject4.getString("route");
                        } catch (Exception e) {
                        }

                        Train train = new Train(line, k == 0 ? "UP" : "DN", station, seq, time, dest, plat, ttnt, timetype, route, curr_time);
                        trains.add(train);
                    }
                } catch (JSONException e) {
                }
            }
        } catch (JSONException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trains;
    }

    public static List<Train> getRoctecTrainData(String data, String station) {
        List<Train> trains = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(data);
            String station0 = jsonObject.getString("station");
            String gen_time = jsonObject.getString("gen_time");

            JSONObject jsonObject2 = jsonObject.getJSONObject("line");

            for (Iterator<String> it = jsonObject2.keys(); it.hasNext(); ) {
                String line0 = it.next();

                if (!line0.endsWith("L")) continue;

                JSONObject jsonObject3 = jsonObject2.getJSONObject(line0);

                for (Iterator<String> it0 = jsonObject3.keys(); it0.hasNext(); ) {
                    String plat = it0.next();

                    JSONArray jsonArray = jsonObject3.getJSONArray(plat);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject4 = jsonArray.getJSONObject(i);
                        String destination = jsonObject4.getString("destination");
                        String td = jsonObject4.getString("td");
                        String ttnt = jsonObject4.getString("ttnt");
                        String tta = jsonObject4.getString("tta");
                        String ttd = jsonObject4.getString("ttd");
                        String seq = jsonObject4.getString("seq");
                        String curr_time = jsonObject4.getString("curr_time");

                        Train train = new Train(line0, "UP", station, seq, curr_time, destination, plat, ttnt, "", td, gen_time);
                        trains.add(train);
                    }
                }
            }
        } catch (JSONException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trains;
    }
}
