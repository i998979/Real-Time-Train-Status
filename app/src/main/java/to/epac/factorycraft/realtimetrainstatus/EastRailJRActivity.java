package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EastRailJRActivity extends AppCompatActivity {

    private RecyclerView rvEastRailLine;
    private JRLineAdapter adapter;
    private List<Trip> activeTrips = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Low Wu -> Admiralty, no Racecourse
    private final int[] EAL_STATION_CODES = {13, 12, 11, 10, 9, 8, 6, 5, 4, 3, 2, 21, 22, 23};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_east_rail_jractivity);

        rvEastRailLine = findViewById(R.id.rv_east_rail_line);
        rvEastRailLine.setLayoutManager(new LinearLayoutManager(this));

        adapter = new JRLineAdapter(this, EAL_STATION_CODES, activeTrips);
        rvEastRailLine.setAdapter(adapter);

        rvEastRailLine.setClipChildren(false);
        rvEastRailLine.setClipToPadding(false);

        startRefreshLoop();
    }

    private void startRefreshLoop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                fetchDataInBackground();
                mainHandler.postDelayed(this, 10000);
            }
        });
    }

    private void fetchDataInBackground() {
        new Thread(() -> {
            try {
                URL url = new URL(/* Link */);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                List<Trip> trips = parseJson(result.toString());

                mainHandler.post(() -> {
                    activeTrips.clear();
                    activeTrips.addAll(trips);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e("JR_LOG", "Fetch Error: " + e.getMessage());
            }
        }).start();
    }

    private List<Trip> parseJson(String json) throws Exception {
        List<Trip> list = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            List<Car> cars = new ArrayList<>();
            if (obj.has("listCars")) {
                JSONArray carArray = obj.getJSONArray("listCars");
                for (int j = 0; j < carArray.length(); j++) {
                    JSONObject c = carArray.getJSONObject(j);
                    cars.add(new Car(c.optInt("carLoad"), c.optInt("passengerCount"),
                            c.optString("carName"), c.optInt("passengerLoad")));
                }
            }

            list.add(new Trip(
                    obj.optString("trainId"), "", obj.optDouble("trainSpeed"),
                    obj.optInt("currentStationCode"), obj.optInt("nextStationCode"),
                    obj.optInt("destinationStationCode"), cars, obj.optLong("receivedTime"),
                    obj.optLong("ttl"), obj.optInt("doorStatus"), obj.optString("td"),
                    obj.optInt("targetDistance"), obj.optInt("startDistance")
            ));
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }
}