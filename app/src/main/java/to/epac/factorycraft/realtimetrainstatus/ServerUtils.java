package to.epac.factorycraft.realtimetrainstatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ServerUtils {

    public static List<Trip> getEALTripData(String data) {
        List<Trip> trips = new ArrayList<>();

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

                Trip trip = new Trip(trainId, "R-Train", trainSpeed, currentStationCode, nextStationCode, destinationStationCode,
                        cars, receivedTime, ttl, doorStatus, td, targetDistance, startDistance);

                trips.add(trip);
            }
        } catch (Exception e) {
        }

        return trips;
    }

    public static List<Trip> getTMLTripData(String data, MapUtils mapUtils) {
        List<Trip> trips = new ArrayList<>();

        try {
            data = data.substring(data.indexOf("["), data.lastIndexOf("]") + 1);

            // Handle TML data
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);

                int trainSpeed = object.getInt("trainSpeed");
                // int carNo = object.getInt("carNo");
                int nextStationCode = object.getInt("nextStationCode");
                // String headSpares = object.getString("headSpares");
                long ttl = object.getLong("ttl");
                // boolean isTimeDateValid = object.getBoolean("isTimeDateValid");
                // boolean isCarNoValid = object.getBoolean("isCarNoValid");
                // boolean isCarTypeValid = object.getBoolean("isCarTypeValid");
                // int carType = object.getInt("carType");
                int distanceFromCurrentStation = object.getInt("distanceFromCurrentStation");
                // int second = object.getInt("second");
                boolean isDoorOpen = object.getBoolean("isDoorOpen");
                // int hour = object.getInt("hour");
                boolean isInService = object.getBoolean("isInService");
                // int trainLength = object.getInt("trainLength");
                int currentStationCode = object.getInt("currentStationCode");
                String trainType = object.getString("train_type");
                // boolean comPortConnection = object.getBoolean("comPortConnection");
                // String ipAddr = object.getString("ipAddr");
                String trainId = object.getString("trainId");
                int destinationStationCode = object.getInt("destinationStationCode");

                JSONArray listCars = object.getJSONArray("listCars");
                List<Car> cars = new ArrayList<>();
                for (int j = 0; j < listCars.length(); j++) {
                    try {
                        JSONObject object1 = listCars.getJSONObject(j);

                        boolean isCarLoadValid = object1.getBoolean("isCarLoadValid");
                        int carLoad = object1.getInt("carLoad");
                        int passengerLoad = object1.getInt("passengerLoad");
                        boolean isPaxLoadValid = object1.getBoolean("isPaxLoadValid");
                        String carName = object1.getString("carName");
                        int passengerCount = object1.getInt("passengerCount");
                        boolean isPanAvailable = object1.getBoolean("isPanAvailable");
                        int panBit = object1.getInt("panBit");
                        int offsetCarLoad = object1.getInt("offsetCarLoad");
                        int initialLoad = object1.getInt("initialLoad");
                        boolean panStatus = object1.getBoolean("panStatus");

                        Car car = new Car(carLoad, passengerCount, carName, passengerLoad);
                        cars.add(car);
                    } catch (Exception e) {
                    }
                }

                // String commandReceived = object.getString("commandReceived");
                // boolean isCarNumberValid = object.getBoolean("isCarNumberValid");
                // int headByte = object.getInt("headByte");
                long receivedTime = object.getLong("receivedTime");
                // int minute = object.getInt("minute");
                // boolean isTimeDataAdjustment = object.getBoolean("isTimeDataAdjustment");
                // int month = object.getInt("month");
                // int year = object.getInt("year");
                // boolean panStatusPM1 = object.getBoolean("panStatusPM1");
                // boolean panStatusPM2 = object.getBoolean("panStatusPM2");
                // int day = object.getInt("day");
                // String tailSpares = object.getString("tailSpares");

                String td0 = "";
                if (isInService) td0 += "AA";
                else td0 += "TT";
                if (Utils.covertStationOrder(currentStationCode) > Utils.covertStationOrder(destinationStationCode))
                    td0 += "0001";
                else td0 += "0002";

                Trip trip = new Trip(trainId, trainType, trainSpeed, currentStationCode, nextStationCode, destinationStationCode,
                        cars, receivedTime, ttl, (isDoorOpen ? 0 : 1), td0,
                        mapUtils.getDistance(currentStationCode, nextStationCode, distanceFromCurrentStation),
                        distanceFromCurrentStation);

                trips.add(trip);
            }
        } catch (Exception e) {
        }

        return trips;
    }
}
