package to.epac.factorycraft.realtimetrainstatus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import to.epac.factorycraft.realtimetrainstatus.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // EAL
    public List<Trip> ealTrips;
    public Map<String, Marker> ealTrainMap;
    public Map<String, Marker> ealStationMap;
    public Map<String, List<Train>> ealTrains;
    public NumberPicker stationPicker;
    public NumberPicker trainPicker;

    // TML
    public List<Trip> tmlTrips;
    public Map<String, Marker> tmlTrainMap;
    public Map<String, Marker> tmlStationMap;
    public Map<String, List<Train>> tmlTrains;

    public Map<String, Marker> stationMap;
    public Map<String, List<Train>> trains;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    MapUtils mapUtils;

    SharedPreferences pref;

    // https://stackoverflow.com/questions/69886446/aes-encryption-and-decryption-java
    // The comparison code is embedded into class

    private String code = "";
    private String link_eal = "";
    private String link_tml = "";
    private String link_roctec = "";
    // link + code = cipher
    private final String cipher_eal = "7hZOAWENNiAjv2M31HzdClndtJ6Aj9Z7LVyrMAdqy9tp+15Z859kS0PCjtVmGGdl";
    private final String cipher_tml = "RoXQs1jcAUiiHtLN35D9oV+kKWtyR0pyto5PTYW31Cjdzqf8iRV9C5PM+VjBAdZLE2LBnVl0a2sBvd1hjJ5EvnO1wBvpepansIYDKHCiPODboqQKelCXVn+WWAK1Q41+";
    private final String cipher_roctec = "cs+fuB90xVnOJAyJgOgq/JrRGaJDEN+MTd0o8a0zBpE6Az690j4ZJngdcNtAUTEK";
    // link + secret = encrypted
    private final String encrypted_eal = "wt2wS84uiY8qZrmJUO4Vl8FCiVoYnrfqmLuYCSfzQ6gYcM246hUutb6ZqSjn1x1O";
    private final String encrypted_tml = "e7gg+sJ4PRy3zatp8dI8ys1VRSj/UxdA4vWoXjah6tiS5XSGc1SSwiHPtnN5x6rlX7SCNNPSxEDWCATWOPKEwbY4pSENdveBLOXTT0EOwwFVbJmcV5C7BvbWrslWM0N2";
    private final String encrypted_roctec = "ET9b/RcMDGTXDGHGTNiE9lbaNmYJDVvk7XcR5xzxXR0RpoX2Q572hN98vuKqm201";

    public static String line = "EAL";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Initialize variables
        ealTrips = new ArrayList<>();
        ealTrainMap = new HashMap<>();
        ealStationMap = new HashMap<>();
        ealTrains = new HashMap<>();

        tmlTrips = new ArrayList<>();
        tmlTrainMap = new HashMap<>();
        tmlStationMap = new HashMap<>();
        tmlTrains = new HashMap<>();

        stationMap = new HashMap<>();
        trains = new HashMap<>();

        mapUtils = new MapUtils(getApplicationContext());
        // Code related
        pref = getSharedPreferences("RealTimeTrainStatus", MODE_PRIVATE);
        code = pref.getString("code", "default");
        link_eal = AES.decrypt(cipher_eal, code);
        link_tml = AES.decrypt(cipher_tml, code);
        link_roctec = AES.decrypt(cipher_roctec, code);

        // Station names
        String[] eal_stations = getResources().getString(R.string.erl_stations).split(" ");
        String[] eal_stations_long = getResources().getString(R.string.erl_stations_long).split(";");
        String[] tml_stations = getResources().getString(R.string.tml_stations).split(" ");
        String[] tml_stations_long = getResources().getString(R.string.tml_stations_long).split(";");
        String[] stations = Arrays.stream((getResources().getString(R.string.ktl_stations) + " "
                        + getResources().getString(R.string.ael_stations) + " "
                        + getResources().getString(R.string.drl_stations) + " "
                        + getResources().getString(R.string.isl_stations) + " "
                        + getResources().getString(R.string.tcl_stations) + " "
                        + getResources().getString(R.string.tkl_stations) + " "
                        + getResources().getString(R.string.twl_stations) + " "
                        + getResources().getString(R.string.sil_stations)).split(" "))
                .distinct().toArray(String[]::new);


        // Request permissions
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_NETWORK_STATE
        }, 1001);


        // Declare handlers and runnables
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 5000);

                Runnable ealNt = () -> {
                    for (String station : eal_stations) {
                        try {
                            String eal_data = "";

                            URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?" +
                                    "line=EAL&sta=" + station.toUpperCase() + "&lang=en");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(5000);
                            conn.connect();

                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String line;
                            while ((line = br.readLine()) != null) {
                                eal_data += line;
                            }

                            ealTrains.put(station, NextTrainUtils.getTrainData(eal_data, "eal", station));
                        } catch (Exception e) {
                        }
                    }
                };

                Runnable tmlNt = () -> {
                    for (String station : tml_stations) {
                        try {
                            String tml_data = "";

                            URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?" +
                                    "line=TML&sta=" + station.toUpperCase() + "&lang=en");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(5000);
                            conn.connect();

                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String line;
                            while ((line = br.readLine()) != null) {
                                tml_data += line;
                            }

                            tmlTrains.put(station, NextTrainUtils.getTrainData(tml_data, "tml", station));
                        } catch (Exception e) {
                        }
                    }
                };

                Runnable ealOv = () -> {
                    ealTrips.clear();
                    try {
                        String eal_data = "";

                        URL url = new URL(link_eal);
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setRequestProperty("x-api-key", "QkmjCRYvXt6o89UdZAvoXa49543NxOtU2tBhQQDQ");
                        conn.connect();

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = "";
                        while ((line = in.readLine()) != null) {
                            eal_data += line;
                        }
                        in.close();

                        ealTrips.addAll(ServerUtils.getEALTrainData(eal_data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };

                Runnable tmlOv = () -> {
                    tmlTrips.clear();
                    try {
                        String tml_data = "";

                        URL url = new URL(link_tml);
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setRequestProperty("x-api-key", "QkmjCRYvXt6o89UdZAvoXa49543NxOtU2tBhQQDQ");
                        conn.connect();

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = "";
                        while ((line = in.readLine()) != null) {
                            tml_data += line;
                        }
                        in.close();

                        tmlTrips.addAll(ServerUtils.getTMLTrainData(tml_data, mapUtils));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };

                CompletableFuture.runAsync(ealNt);
                CompletableFuture.runAsync(tmlNt);

                CompletableFuture.allOf(CompletableFuture.runAsync(ealOv), CompletableFuture.runAsync(tmlOv))
                        .thenRun(() -> {
                            load();
                        });
            }
        };


        // Check user's code input
        if (AES.encrypt(link_eal) == null || !AES.encrypt(link_eal).equals(encrypted_eal)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.layout_code, null);

            EditText editText = view.findViewById(R.id.code);
            builder.setView(view)
                    .setTitle("Code:")
                    .setPositiveButton("OK", (dialog, which) -> {
                        code = editText.getText().toString();
                        pref.edit().putString("code", code).apply();

                        link_eal = AES.decrypt(cipher_eal, code);
                        link_tml = AES.decrypt(cipher_tml, code);

                        handler.post(runnable);
                        for (String station : stations) {
                            fetchRoctec(station);
                        }
                    }).show();
        } else {
            handler.post(runnable);
            for (String station : stations) {
                fetchRoctec(station);
            }
        }


        // StationPicker and TrainPicker
        stationPicker = findViewById(R.id.stationPicker);
        stationPicker.setSaveFromParentEnabled(false);
        stationPicker.setSaveEnabled(false);
        stationPicker.setWrapSelectorWheel(false);
        stationPicker.setDisplayedValues(new String[]{"0000000000000000000000000000000000000000"});
        stationPicker.setTextColor(Color.GRAY);

        trainPicker = findViewById(R.id.trainPicker);
        trainPicker.setSaveFromParentEnabled(false);
        trainPicker.setSaveEnabled(false);
        trainPicker.setWrapSelectorWheel(false);
        trainPicker.setDisplayedValues(new String[]{"0000000000000000000000000000000000000000"});
        trainPicker.setTextColor(Color.GRAY);

        stationPicker.setDisplayedValues(null);
        stationPicker.setMinValue(0);
        if (line.equals("EAL")) {
            stationPicker.setMaxValue(Math.max(eal_stations.length - 1, 0));
            stationPicker.setDisplayedValues(eal_stations_long);
        }
        if (line.equals("TML")) {
            stationPicker.setMaxValue(Math.max(tml_stations.length - 1, 0));
            stationPicker.setDisplayedValues(tml_stations_long);
        }


        findViewById(R.id.stationsLayout).setOnClickListener(v -> {
            stationPicker.setDisplayedValues(null);
            stationPicker.setMinValue(0);
            if (line.equals("EAL")) {
                stationPicker.setMaxValue(Math.max(eal_stations.length - 1, 0));
                stationPicker.setDisplayedValues(eal_stations_long);
            }
            if (line.equals("TML")) {
                stationPicker.setMaxValue(Math.max(tml_stations.length - 1, 0));
                stationPicker.setDisplayedValues(tml_stations_long);
            }

            if (stationPicker.getVisibility() == View.VISIBLE) {
                stationPicker.setValue(0);
                Marker marker = null;
                if (line.equals("EAL")) marker = ealStationMap.get(eal_stations[0]);
                if (line.equals("TML")) marker = tmlStationMap.get(tml_stations[0]);

                if (marker != null) {
                    CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f);
                    mMap.animateCamera(cu, 1000, null);
                }
            } else {
                stationPicker.setVisibility(View.VISIBLE);
                trainPicker.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.trainsLayout).setOnClickListener(v -> {
            List<String> eal_trains = ealTrips.stream()
                    .map(trip -> trip.trainId + "(T" + (Integer.parseInt(trip.trainId) / 3) + ") " + trip.td + " "
                            + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                            + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : ""))
                    .sorted().collect(Collectors.toList());

            List<String> tml_trains = tmlTrips.stream()
                    .map(trip -> trip.trainId + " "
                            + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                            + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : ""))
                    .sorted().collect(Collectors.toList());


            if (line.equals("EAL")) {
                trainPicker.setDisplayedValues(null);
                trainPicker.setMinValue(0);
                trainPicker.setMaxValue(Math.max(eal_trains.size() - 1, 0));
                if (!eal_trains.isEmpty())
                    trainPicker.setDisplayedValues(eal_trains.toArray(new String[]{}));
            }
            if (line.equals("TML")) {
                trainPicker.setDisplayedValues(null);
                trainPicker.setMinValue(0);
                trainPicker.setMaxValue(Math.max(tml_trains.size() - 1, 0));
                if (!tml_trains.isEmpty())
                    trainPicker.setDisplayedValues(tml_trains.toArray(new String[]{}));
            }


            if (trainPicker.getVisibility() == View.VISIBLE) {
                trainPicker.setValue(0);

                List<String> ealIdList = ealTrips.stream()
                        .map(trip -> trip.trainId)
                        .sorted()
                        .collect(Collectors.toList());

                List<String> tmlIdList = tmlTrips.stream()
                        .map(trip -> trip.trainId)
                        .sorted()
                        .collect(Collectors.toList());


                Marker marker = null;
                if (line.equals("EAL"))
                    if (!ealIdList.isEmpty())
                        marker = ealTrainMap.get(ealIdList.get(0));

                if (line.equals("TML"))
                    if (!tmlIdList.isEmpty())
                        marker = tmlTrainMap.get(tmlIdList.get(0));


                if (marker != null) {
                    CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f);
                    mMap.animateCamera(cu, 1000, null);
                }
            } else {
                stationPicker.setVisibility(View.GONE);
                trainPicker.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.eal).setOnClickListener(v -> {
            line = "EAL";
            stationPicker.setVisibility(View.VISIBLE);
            trainPicker.setVisibility(View.GONE);

            stationPicker.setDisplayedValues(null);
            stationPicker.setMinValue(0);
            stationPicker.setMaxValue(Math.max(eal_stations.length - 1, 0));
            stationPicker.setDisplayedValues(eal_stations_long);
        });
        findViewById(R.id.tml).setOnClickListener(v -> {
            line = "TML";
            stationPicker.setVisibility(View.VISIBLE);
            trainPicker.setVisibility(View.GONE);

            stationPicker.setDisplayedValues(null);
            stationPicker.setMinValue(0);
            stationPicker.setMaxValue(Math.max(tml_stations.length - 1, 0));
            stationPicker.setDisplayedValues(tml_stations_long);
        });


        stationPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            Marker marker = null;
            if (line.equals("EAL")) marker = ealStationMap.get(eal_stations[newVal]);
            if (line.equals("TML")) marker = tmlStationMap.get(tml_stations[newVal]);

            if (marker != null) {
                CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f);
                mMap.animateCamera(cu, 1000, null);
            }
        });

        trainPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            List<String> ealIdList = ealTrips.stream()
                    .map(trip -> trip.trainId)
                    .sorted().collect(Collectors.toList());

            List<String> tmlIdList = tmlTrips.stream()
                    .map(trip -> trip.trainId)
                    .sorted().collect(Collectors.toList());


            Marker marker = null;
            if (line.equals("EAL"))
                if (newVal < ealIdList.size())
                    marker = ealTrainMap.get(ealIdList.get(newVal));

            if (line.equals("TML"))
                if (newVal < tmlIdList.size())
                    marker = tmlTrainMap.get(tmlIdList.get(newVal));


            if (marker != null) {
                CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f);
                mMap.animateCamera(cu, 1000, null);
            }
        });
    }

    public CompletableFuture<Void> fetchRoctec(String station) {
        Runnable roctecRunnable = () -> {
            try {
                String data = "";
                URL url = new URL("https://408tq84duh.execute-api.ap-east-1.amazonaws.com/api/service/GetNextTrainData");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("content-type", "application/json");
                conn.setRequestProperty("data", "{\"stationcode\":\"" + station.toUpperCase() + "\"}");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("{\"stationcode\":\"" + station.toUpperCase() + "\"}").getBytes());
                }
                conn.setConnectTimeout(5000);
                conn.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    data += line;
                }
                //trains.get(station).clear();
                trains.put(station, NextTrainUtils.getRoctecTrainData(data, station));
            } catch (Exception e) {
            }
        };
        return CompletableFuture.runAsync(roctecRunnable);
    }

    public void load() {
        runOnUiThread(() -> {
            updateStations();
        });

        for (Trip trip : ealTrips) {
            CompletableFuture.supplyAsync(() -> {
                return mapUtils.getTrainAt(trip, "EAL");
            }).thenAccept(latLng -> {
                runOnUiThread(() -> {
                    if (latLng != null) {
                        // Create a new marker if not exist, or reuse old one if exist
                        Marker train = ealTrainMap.get(trip.trainId);
                        if (train == null) {
                            // Default position is required to add a new marker
                            train = mMap.addMarker(new MarkerOptions().position(latLng).zIndex(50).anchor(0.5f, 0.5f));
                            train.setTag("train");
                        }

                        // Reset marker opacity
                        train.setAlpha(1.0f);
                        train.setZIndex(50);

                        // Icon
                        if (trip.currentStationCode == 0 || System.currentTimeMillis() - trip.receivedTime > 60000) {
                            train.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.r_train_unknown));
                            train.setAlpha(0.5f);
                            train.setZIndex(0);
                        } else if (!Utils.isPassengerTrain(trip.td))
                            train.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.r_train_nis));
                        else {
                            if (Integer.parseInt(trip.td.substring(2)) % 2 != 0)
                                train.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.r_train_up));
                            else
                                train.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.r_train_dn));
                        }

                        // Pass data to adapter
                        String snippet = trip.trainId + "(T" + (Integer.parseInt(trip.trainId) / 3) + ") " + trip.td + " "
                                + Utils.mapStation(trip.currentStationCode, "EAL") + " to " + Utils.mapStation(trip.nextStationCode, "EAL")
                                + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, "EAL") + ")" : "")
                                + " " + trip.trainSpeed + "km/h;";
                        for (Car car : trip.listCars) {
                            snippet += car.carName + "," + car.passengerCount + "," + trip.td + ";";
                        }

                        train.setSnippet(snippet);

                        // Position
                        train.setPosition(latLng);

                        ealTrainMap.put(trip.trainId, train);
                    }
                });
            });
        }

        // TML
        for (Trip trip : tmlTrips) {
            CompletableFuture.supplyAsync(() -> {
                return mapUtils.getTrainAt(trip, "TML");
            }).thenAccept(latLng -> {
                runOnUiThread(() -> {
                    if (latLng != null) {
                        // Create a new marker if not exist, or reuse old one if exist
                        Marker train = tmlTrainMap.get(trip.trainId);
                        if (train == null) {
                            // Default position is required to add a new marker
                            train = mMap.addMarker(new MarkerOptions().position(latLng).zIndex(50).anchor(0.5f, 0.5f));
                            train.setTag("train");
                        }

                        // Reset marker opacity
                        train.setAlpha(1.0f);
                        train.setZIndex(50);

                        // Icon
                        if (trip.currentStationCode == 0 || System.currentTimeMillis() - trip.receivedTime > 60000) {
                            train.setIcon(BitmapDescriptorFactory.fromResource(trip.trainType.equals("SP1900") ? R.drawable.sp1900_unknown : R.drawable.t1141a_unknown));
                            train.setAlpha(0.5f);
                            train.setZIndex(0);
                        } /*else if (!Utils.isPassengerTrain(trip.td))
                            train.setIcon(BitmapDescriptorFactory.fromResource(trip.trainType.equals("SP1900") ? R.drawable.r_train_nis : R.drawable.t1141a_nis));*/ else {
                            if (Integer.parseInt(trip.td.substring(2)) % 2 == 0)
                                train.setIcon(BitmapDescriptorFactory.fromResource(trip.trainType.equals("SP1900") ? R.drawable.sp1900_up : R.drawable.t1141a_up));
                            else
                                train.setIcon(BitmapDescriptorFactory.fromResource(trip.trainType.equals("SP1900") ? R.drawable.sp1900_dn : R.drawable.t1141a_dn));
                        }

                        // Pass data to adapter
                        String snippet = trip.trainId + " "
                                + Utils.mapStation(trip.currentStationCode, "TML") + " to " + Utils.mapStation(trip.nextStationCode, "TML")
                                + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, "TML") + ")" : "")
                                + " " + trip.trainSpeed + "km/h;";
                        for (Car car : trip.listCars) {
                            snippet += car.carName + "," + car.passengerCount + "," + trip.td + ";";
                        }

                        train.setSnippet(snippet);


                        // Position
                        train.setPosition(latLng);

                        tmlTrainMap.put(trip.trainId, train);
                    }
                });
            });
        }


        List<String> eal_trains = ealTrips.stream()
                .map(trip -> trip.trainId + "(T" + (Integer.parseInt(trip.trainId) / 3) + ") " + trip.td + " "
                        + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                        + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : ""))
                .sorted().collect(Collectors.toList());

        List<String> tml_trains = tmlTrips.stream()
                .map(trip -> trip.trainId + " " + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                        + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : ""))
                .sorted().collect(Collectors.toList());


        if (line.equals("EAL")) {
            trainPicker.setMinValue(0);
            trainPicker.setMaxValue(Math.max(eal_trains.size() - 1, 0));
            if (!eal_trains.isEmpty())
                trainPicker.setDisplayedValues(eal_trains.toArray(new String[]{}));
        }
        if (line.equals("TML")) {
            trainPicker.setMinValue(0);
            trainPicker.setMaxValue(Math.max(tml_trains.size() - 1, 0));
            if (!tml_trains.isEmpty())
                trainPicker.setDisplayedValues(tml_trains.toArray(new String[]{}));
        }
    }


    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        drawLines();
        addStations();

        getWindowManager().getDefaultDisplay().getMetrics(new DisplayMetrics());

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(new LatLngBounds.Builder()
                        .include(Utils.getLatLng(getResources().getString(R.string.adm)))
                        .include(Utils.getLatLng(getResources().getString(R.string.uni)))
                        .include(Utils.getLatLng(getResources().getString(R.string.low)))
                        .include(Utils.getLatLng(getResources().getString(R.string.lmc)))
                        .include(Utils.getLatLng(getResources().getString(R.string.lop)))
                        .include(Utils.getLatLng(getResources().getString(R.string.tum)))
                        .include(Utils.getLatLng(getResources().getString(R.string.wks)))
                        .build(),
                Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels,
                250);
        mMap.moveCamera(cu);


        TrainInfoAdapter trainInfoAdapter = new TrainInfoAdapter(this);
        mMap.setInfoWindowAdapter(trainInfoAdapter);

        mMap.setOnMarkerClickListener(marker -> {
            // Apply NextTrain data
            updateStation(marker);

            marker.showInfoWindow();

            return false;
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != 1001) return;

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) return;
        }

        if (mMap != null)
            mMap.setMyLocationEnabled(true);
    }


    public void drawLines() {
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.erl_main)), "#5eb7e8");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.erl_rac)), "#5eb7e8");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.erl_lmc)), "#5eb7e8");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.tml_main)), "#9c2e00");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.ktl_main)), "#00a040");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.ael_main)), "#00888e");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.drl_main)), "#eb6ea5");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.isl_main)), "#0075c2");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.tcl_main)), "#f3982d");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.tkl_main)), "#7e3c93");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.twl_main)), "#e60012");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.sil_main)), "#cbd300");
    }

    public void addStations() {
        for (String station : getResources().getString(R.string.erl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:eal");
            ealStationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.tml_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tml");
            tmlStationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.ktl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:ktl");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.ael_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:ael");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.drl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:drl");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.isl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:isl");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.tcl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tcl");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.tkl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tkl");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.twl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:twl");
            stationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.sil_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:sil");
            stationMap.put(station, marker);
        }
    }

    public void updateStation(Marker marker) {
        if (!ealStationMap.containsValue(marker) && !tmlStationMap.containsValue(marker) && !stationMap.containsValue(marker))
            return;

        Marker mar;
        // EAL
        if (ealStationMap.containsValue(marker)) {
            Map.Entry<String, Marker> entry = ealStationMap.entrySet().stream()
                    .filter(ent -> ent.getValue().equals(marker)).findFirst().get();
            String station = entry.getKey();
            mar = entry.getValue();

            if (!ealTrains.containsKey(station)) return;


            String snippet = "";

            for (Train train : ealTrains.get(station)) {
                snippet += Utils.getStationName(this, train.dest) + (train.route.equals("RAC") ? " via Racecourse " : " ")
                        + "," + train.plat + "," + train.ttnt + ";";
            }

            if (snippet.endsWith(";"))
                snippet = snippet.substring(0, snippet.length() - 1);

            mar.setSnippet(snippet);
        }
        // TML
        else if (tmlStationMap.containsValue(marker)) {
            Map.Entry<String, Marker> entry = tmlStationMap.entrySet().stream()
                    .filter(ent -> ent.getValue().equals(marker)).findFirst().get();
            String station = entry.getKey();
            mar = entry.getValue();

            if (!tmlTrains.containsKey(station)) return;

            String snippet = "";

            for (Train train : tmlTrains.get(station)) {
                snippet += Utils.getStationName(this, train.dest) + " ," + train.plat + "," + train.ttnt + ";";
            }

            if (snippet.endsWith(";"))
                snippet = snippet.substring(0, snippet.length() - 1);

            mar.setSnippet(snippet);
        }
        // DUAT
        else {
            Map.Entry<String, Marker> entry = stationMap.entrySet().stream()
                    .filter(ent -> ent.getValue().equals(marker)).findFirst().get();
            String station = entry.getKey();
            mar = entry.getValue();

            if (!trains.containsKey(station)) return;

            fetchRoctec(station).thenRun(() -> {
                String snippet = "";

                for (Train train : trains.get(station)) {
                    snippet += Utils.getStationName(this, train.dest) + "," + train.route
                            + "," + train.plat + "," + train.ttnt + ";";
                }

                if (snippet.endsWith(";"))
                    snippet = snippet.substring(0, snippet.length() - 1);

                mar.setSnippet(snippet);
            });
        }
    }

    public void updateStations() {
        for (Map.Entry<String, Marker> entry : ealStationMap.entrySet()) {
            String station = entry.getKey();
            Marker mar = entry.getValue();

            if (!ealTrains.containsKey(station)) continue;

            String snippet = "";

            for (Train train : ealTrains.get(station)) {
                snippet += Utils.getStationName(this, train.dest) + (train.route.equals("RAC") ? " via Racecourse " : " ")
                        + "," + train.plat + "," + train.ttnt + ";";
            }

            if (snippet.endsWith(";"))
                snippet = snippet.substring(0, snippet.length() - 1);

            mar.setSnippet(snippet);
        }

        for (Map.Entry<String, Marker> entry : tmlStationMap.entrySet()) {
            String station = entry.getKey();
            Marker mar = entry.getValue();

            if (!tmlTrains.containsKey(station)) continue;

            String snippet = "";

            for (Train train : tmlTrains.get(station)) {
                snippet += Utils.getStationName(this, train.dest) + " ," + train.plat + "," + train.ttnt + ";";
            }

            if (snippet.endsWith(";"))
                snippet = snippet.substring(0, snippet.length() - 1);

            mar.setSnippet(snippet);
        }

        for (Map.Entry<String, Marker> entry : stationMap.entrySet()) {
            String station = entry.getKey();
            Marker mar = entry.getValue();

            if (!trains.containsKey(station)) continue;

            String snippet = "";

            for (Train train : trains.get(station)) {
                snippet += Utils.getStationName(this, train.dest) + "," + train.route
                        + "," + train.plat + "," + train.ttnt + ";";
            }

            if (snippet.endsWith(";"))
                snippet = snippet.substring(0, snippet.length() - 1);

            mar.setSnippet(snippet);
        }
    }
}