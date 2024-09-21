package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class TrainInfoAdapter implements GoogleMap.InfoWindowAdapter {

    private Activity context;

    public TrainInfoAdapter(Activity context) {
        this.context = context;
    }


    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        View view = null;

        if (marker.getSnippet() == null) return null;
        if (marker.getTag() == null) return null;

        String tag = marker.getTag().toString();
        String[] datas = marker.getSnippet().split(";");

        // Station layout
        if (tag.startsWith("station")) {
            String line = tag.split(":")[1];
            String station = tag.split(":")[2];

            view = context.getLayoutInflater().inflate((line.equals("eal") || line.equals("tml")) ? R.layout.layout_info : R.layout.layout_roctec, null);
            TableLayout infoLayout = view.findViewById(R.id.infoLayout);
            TableRow stationRow = view.findViewById(R.id.stationRow);
            TextView stationTv = view.findViewById(R.id.station);

            stationRow.setBackgroundColor(Color.parseColor(Utils.getColor(context, line)));
            stationTv.setText(Utils.getStationName(context, station));


            for (int i = 0; i < datas.length; i++) {
                String snippet = datas[i];
                String[] data = snippet.split(",");

                if (data.length <= 1) continue;

                TableRow tableRow = new TableRow(context);
                tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                if (i % 2 != 0) tableRow.setBackgroundColor(Color.parseColor("#C5D9E4"));

                if (line.equals("eal") || line.equals("tml")) {
                    TextView dest = new TextView(context);
                    dest.setTextColor(Color.BLACK);
                    dest.setText(data[0]);

                    TextView plat = new TextView(context);
                    plat.setTextColor(Color.BLACK);
                    plat.setText(data[1]);

                    TextView ttnt = new TextView(context);
                    ttnt.setTextColor(Color.BLACK);
                    ttnt.setText(data[2]);

                    tableRow.addView(dest);
                    tableRow.addView(plat);
                    tableRow.addView(ttnt);
                } else {
                    TextView dest = new TextView(context);
                    dest.setTextColor(Color.BLACK);
                    dest.setText(data[0]);

                    TextView td = new TextView(context);
                    td.setTextColor(Color.BLACK);
                    td.setText(data[1]);

                    TextView plat = new TextView(context);
                    plat.setTextColor(Color.BLACK);
                    plat.setText(data[2]);

                    TextView ttnt = new TextView(context);
                    ttnt.setTextColor(Color.BLACK);
                    ttnt.setText(data[3]);

                    tableRow.addView(dest);
                    tableRow.addView(td);
                    tableRow.addView(plat);
                    tableRow.addView(ttnt);
                }

                infoLayout.addView(tableRow);
            }
        }

        // Train layout
        else if (tag.startsWith("train")) {
            view = context.getLayoutInflater().inflate(R.layout.layout_train, null);

            LinearLayout trainLayout = view.findViewById(R.id.trainLayout);

            TextView title = view.findViewById(R.id.trainTitle);
            title.setText(datas[0]);

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params0 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            linearLayout.setLayoutParams(params0);

            TextView direction = new TextView(context);
            direction.setTextColor(Color.GRAY);
            direction.setText("◀");
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params1.setMargins(2, 2, 2, 2);
            direction.setLayoutParams(params1);
            linearLayout.addView(direction);

            boolean isUp = true;

            for (int i = 1; i < datas.length; i++) {
                String[] carData = datas[i].split(",");

                try {
                    isUp = Integer.parseInt(carData[2].substring(carData[2].length() - 1)) % 2 != 0;
                } catch (Exception e) {
                }

                // Reorder if DN train
                if (!isUp) carData = datas[datas.length - i].split(",");

                boolean has1st = datas.length == 10;


                TextView passengerCount = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) (30 * context.getResources().getDisplayMetrics().density), LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(2, 2, 2, 2);
                passengerCount.setGravity(Gravity.CENTER);
                passengerCount.setLayoutParams(params);


                int first = isUp ? 4 : 6;
                // If the train has 9 compartments and is 1st class
                if (has1st && i == first) {
                    if (Integer.parseInt(carData[1]) < 70)
                        passengerCount.setBackgroundColor(Color.parseColor("#FF4CAF50"));
                    else if (Integer.parseInt(carData[1]) < 150)
                        passengerCount.setBackgroundColor(Color.parseColor("#FFCDDC39"));
                    else
                        passengerCount.setBackgroundColor(Color.parseColor("#FFF44336"));
                } else {
                    if (Integer.parseInt(carData[1]) < 110)
                        passengerCount.setBackgroundColor(Color.parseColor("#FF4CAF50"));
                    else if (Integer.parseInt(carData[1]) < 250)
                        passengerCount.setBackgroundColor(Color.parseColor("#FFCDDC39"));
                    else
                        passengerCount.setBackgroundColor(Color.parseColor("#FFF44336"));
                }


                passengerCount.setTextColor(has1st && i == first ? Color.parseColor("#FF880015") : Color.WHITE);
                passengerCount.setText(carData[1]);
                linearLayout.addView(passengerCount);
            }

            trainLayout.addView(linearLayout);
        }


        return view;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }
}
