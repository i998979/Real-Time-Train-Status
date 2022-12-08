package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

public class Trip {

    public String trainId;
    public String trainType;
    public double trainSpeed;
    public int currentStationCode;
    public int nextStationCode;
    public int destinationStationCode;
    public List<Car> listCars;
    public long receivedTime;
    public long ttl;
    public int doorStatus;
    public String td;
    public int targetDistance;
    public int startDistance;

    public Trip(String trainId, String trainType, double trainSpeed, int currentStationCode, int nextStationCode, int destinationStationCode,
                List<Car> listCars, long receivedTime, long ttl, int doorStatus, String td, int targetDistance, int startDistance) {
        this.trainId = trainId;
        this.trainType = trainType;
        this.trainSpeed = trainSpeed;
        this.currentStationCode = currentStationCode;
        this.nextStationCode = nextStationCode;
        this.destinationStationCode = destinationStationCode;
        this.listCars = listCars;
        this.receivedTime = receivedTime;
        this.ttl = ttl;
        this.doorStatus = doorStatus;
        this.td = td;
        this.targetDistance = targetDistance;
        this.startDistance = startDistance;
    }

    public void addToLayout(Context context, ViewGroup viewGroup) {
        // 1: UP    0: DN
        int direction = 1;
        try {
            direction = Integer.parseInt(td.substring(2)) % 2;
        } catch (Exception e) {
        }

        /*LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));*/

        TableRow tableRow = new TableRow(context);
        tableRow.setOrientation(TableRow.HORIZONTAL);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));


        TextView tvtrainId = new TextView(context);
        TextView tvtrainSpeed = new TextView(context);
        TextView tvcurrentStationCode = new TextView(context);
        TextView tvnextStationCode = new TextView(context);
        TextView tvdestinationStationCode = new TextView(context);
        TextView tvlistCars = new TextView(context);
        TextView tvreceivedTime = new TextView(context);
        TextView tvttl = new TextView(context);
        TextView tvdoorStatus = new TextView(context);
        TextView tvtd = new TextView(context);
        TextView tvtargetDistance = new TextView(context);
        TextView tvstartDistance = new TextView(context);

        // tvtrainId.setText(trainId);
        // tvtrainSpeed.setText(trainSpeed + " km/h");
        tvcurrentStationCode.setText(Utils.mapStation(currentStationCode, "EAL"));
        tvnextStationCode.setText(Utils.mapStation(nextStationCode, "EAL"));
        // tvdestinationStationCode.setText(Utils.mapStation(destinationStationCode));
        // tvlistCars.setText(listCars + "");
        tvreceivedTime.setText(receivedTime + "");
        tvttl.setText(ttl + "");
        // tvdoorStatus.setText(doorStatus.equals("0") ? "O" : "C");
        tvtd.setText(td);
        tvtargetDistance.setText(targetDistance);
        if (startDistance != 0) {
            DecimalFormat df = new DecimalFormat("#.##");
            double progress = ((double) startDistance / ((double) startDistance + (double) targetDistance)) * 100;
            // tvtargetDistance.setText(df.format(direction == 0 ? progress : 100 - progress) + "");

        }

        // tvstartDistance.setText(startDistance);
        // tvstartDistance.setText(startDistance);


        tableRow.addView(tvtrainId);
        tableRow.addView(tvtrainSpeed);
        tableRow.addView(tvcurrentStationCode);
        tableRow.addView(tvnextStationCode);
        tableRow.addView(tvdestinationStationCode);
        // tableRow.addView(tvlistCars);
        // Last update
        // tableRow.addView(tvreceivedTime);
        // tableRow.addView(tvttl);
        tableRow.addView(tvdoorStatus);
        tableRow.addView(tvtd);
        tableRow.addView(tvtargetDistance);
        tableRow.addView(tvstartDistance);

        viewGroup.addView(tableRow);
    }
}
