package to.epac.factorycraft.realtimetrainstatus;

import com.google.common.collect.HashBasedTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TrainNoUtils {

    public static HashBasedTable<String, String, String> getTrainNos(String line, String data) {
        HashBasedTable<String, String, String> table = HashBasedTable.create();

        try {
            data = data.substring(data.indexOf("["), data.lastIndexOf("]") + 1);

            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);

                double trainSpeed = object.has("trainSpeed") ? object.getDouble("trainSpeed") : 0.0;
                String nextStationCode = object.getString("nextStationCode");
                // netWeight
                String trainId = object.getString("trainId");
                long ttl = object.getLong("ttl");
                String destinationStationCode = object.getString("destinationStationCode");
                // jsonContent
                String trainType = object.getString("trainType");
                String td = Utils.isInteger(object.getString("td")) ? Integer.parseInt(object.getString("td")) + "" : object.getString("td");
                // line
                long lambdaDateTime = object.has("lambdaDateTime") ? object.getLong("lambdaDateTime") : 0;
                // carLoads
                long updatedTime = object.has("updatedTime") ? object.getLong("updatedTime") : 0;
                boolean doorStatus = object.has("doorStatus") && object.getBoolean("doorStatus");
                String trainConsist = object.getString("trainConsist");
                String currentStationCode = object.getString("currentStationCode");


                if (System.currentTimeMillis() / 1000 - lambdaDateTime > (line.equals("TCL") ? 600 : 60))
                    continue;

                if (Utils.isInteger(td) && Integer.parseInt(td) > 0 || !(Utils.isInteger(td)))
                    table.put(line, td, line.equals("TKL") ? trainId : trainConsist);
            }
        } catch (JSONException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return table;
    }

    public static HashBasedTable<String, String, String> getEALTrainNos(String data) {
        HashBasedTable<String, String, String> eal = HashBasedTable.create();

        try {
            data = data.substring(data.indexOf("["), data.lastIndexOf("]") + 1);

            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);

                String trainId = object.getString("trainId");
                double trainSpeed = Double.parseDouble(object.getString("trainSpeed"));
                int currentStationCode = Integer.parseInt(object.getString("currentStationCode"));
                int nextStationCode = Integer.parseInt(object.getString("nextStationCode"));
                int destinationStationCode = Integer.parseInt(object.getString("destinationStationCode"));

                JSONArray listCars = object.getJSONArray("listCars");
                List<Car> cars = new ArrayList<>();
                for (int j = 0; j < listCars.length(); j++) {
                    try {
                        JSONObject object1 = listCars.getJSONObject(j);
                        int carLoad = object1.getInt("carLoad");
                        int passengerCount = object1.getInt("passengerCount");
                        String carName = object1.getString("carName");
                        int passengerLoad = object1.getInt("passengerLoad");

                        Car car = new Car(carLoad, passengerCount, carName, passengerLoad);
                        cars.add(car);
                    } catch (Exception e) {
                    }
                }

                long receivedTime = object.getLong("receivedTime");
                long ttl = object.getLong("ttl");
                int doorStatus = Integer.parseInt(object.getString("doorStatus"));
                String td = object.getString("td");
                int targetDistance = Integer.parseInt(object.getString("targetDistance"));
                int startDistance = Integer.parseInt(object.getString("startDistance"));

                if (System.currentTimeMillis() / 1000 - receivedTime > 60) continue;

                eal.put("NSL", td, trainId);
            }
        } catch (Exception e) {
        }

        return eal;
    }
}