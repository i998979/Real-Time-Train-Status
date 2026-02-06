package to.epac.factorycraft.realtimetrainstatus;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    public String trainId;
    public String td;
    public int currentStationCode;
    public int nextStationCode;
    public int destinationStationCode;
    public List<Car> listCars;
    public long receivedTime;
    public boolean isOpenData;
    public boolean isUp;

    // --- Roctec API ---
    public String trainType;
    public double trainSpeed;
    public int doorStatus;
    public int targetDistance;
    public int startDistance;
    public long ttl;

    // --- Next Train API ---
    public int seq;
    public int ttnt;
    public long expectedArrivalTime;
    public String timeType;
    public String route;

    /**
     * Next Train API
     */
    public Trip(int currentStationCode, int nextStationCode, int destinationStationCode,
                long expectedArrivalTime, String direction, int seq, String route, int ttnt, String timeType) {
        this.isOpenData = true;
        this.currentStationCode = currentStationCode;
        this.nextStationCode = nextStationCode;
        this.destinationStationCode = destinationStationCode;
        this.expectedArrivalTime = expectedArrivalTime;
        isUp = direction.equalsIgnoreCase("UP");
        this.seq = seq;
        this.route = (route != null) ? route : "";
        this.ttnt = ttnt;
        this.timeType = (timeType != null) ? timeType : "A";

        this.receivedTime = System.currentTimeMillis();
        this.listCars = new ArrayList<>();

        boolean isRac = this.route.equals("RAC");
        switch (destinationStationCode) {
            // Up train
            case 7:
                this.td = !isRac ? "JM000" : "JR000";
                break;
            case 9:
                this.td = !isRac ? "JD000" : "JG000";
                break;
            case 12:
                this.td = !isRac ? "JH000" : "JK000";
                break;
            case 13:
                this.td = !isRac ? "JM000" : "JN000";
                break;
            case 14:
                this.td = !isRac ? "JL000" : "JB000";
                break;
            // Dn train
            case 23:
                this.td = !isRac ? "JL000" : "JB000";
                break;
            case 1:
                this.td = !isRac ? "YL000" : "YB000";
                break;
            case 2:
                this.td = !isRac ? "EL000" : "EB000";
                break;
            // Default
            default:
                this.td = !isRac ? "JM000" : "JN000";
        }
        this.td += isUp ? "1" : "2";

        // 產生唯一 ID
        this.trainId = "API-" + currentStationCode + "-" + destinationStationCode
                + "-" + seq + "-" + direction;

        // Initialize Roctec fields
        this.trainType = "N/A";
        this.trainSpeed = this.ttnt > 1 ? 50.0 : 0.0;
        this.doorStatus = 0;
        this.targetDistance = 0;
        this.startDistance = 0;
        this.ttl = 0;
    }

    /**
     * Roctec API
     */
    public Trip(String trainId, String trainType, double trainSpeed, int currentStationCode,
                int nextStationCode, int destinationStationCode, List<Car> listCars,
                long receivedTime, long ttl, int doorStatus, String td,
                int targetDistance, int startDistance) {

        this.isOpenData = false;
        this.trainId = trainId;
        this.trainType = trainType;
        this.trainSpeed = trainSpeed;
        this.currentStationCode = currentStationCode;
        this.nextStationCode = nextStationCode;
        this.destinationStationCode = destinationStationCode;

        this.listCars = new ArrayList<>();
        if (listCars != null) this.listCars = listCars;

        this.receivedTime = receivedTime;
        this.ttl = ttl;
        this.doorStatus = doorStatus;
        this.td = td;
        this.targetDistance = targetDistance;
        this.startDistance = startDistance;

        // Initialize Next Train fields
        this.seq = 0;
        this.ttnt = 0;
        this.expectedArrivalTime = 0;
        this.timeType = "";
        this.route = "";
    }
}