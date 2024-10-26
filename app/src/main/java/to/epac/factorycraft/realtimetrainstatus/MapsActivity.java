package to.epac.factorycraft.realtimetrainstatus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.HashBasedTable;

import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import to.epac.factorycraft.realtimetrainstatus.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    /* EAL */
    public List<Trip> ealTrips;
    // Train ID : Marker
    public Map<String, Marker> ealTrainMarkers;
    // EAL station markers, used by menu to navigate positions
    public Map<String, Marker> ealStationMarkers;
    public NumberPicker stationPicker;
    public NumberPicker trainPicker;

    // TML
    public List<Trip> tmlTrips;
    public Map<String, Marker> tmlTrainMarkers;
    public Map<String, Marker> tmlStationMarkers;

    // Line : Stations
    public Map<String, String[]> stations;
    // Station : Marker
    public Map<Marker, String> stationMarkers;
    public Map<String, List<Train>> roctecTrains;
    public HashBasedTable<String, String, List<Train>> trains;

    private Handler infoHandler;
    private Runnable infoRunnable;

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
    private String cipher_eal = "";
    private String cipher_tml = "";
    private String cipher_roctec = "";

    // link + secret = encrypted
    private String encrypted_eal = "";
    private String encrypted_tml = "";
    private String encrypted_roctec = "";

    public static String line = "EAL";

    public enum ServerType {
        ROCTEC,
        NEXT_TRAIN
    }

    public static ServerType type = ServerType.NEXT_TRAIN;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Initialize variables
        ealTrips = new ArrayList<>();
        ealTrainMarkers = new HashMap<>();
        ealStationMarkers = new HashMap<>();

        tmlTrips = new ArrayList<>();
        tmlTrainMarkers = new HashMap<>();
        tmlStationMarkers = new HashMap<>();

        stations = new HashMap<>();
        stationMarkers = new HashMap<>();
        trains = HashBasedTable.create();
        roctecTrains = new HashMap<>();

        infoHandler = new Handler();

        mapUtils = new MapUtils(getApplicationContext());
        // Code related
        pref = getSharedPreferences("RealTimeTrainStatus", MODE_PRIVATE);
        code = pref.getString("code", "default");

        // Station names
        String[] eal_stations = getResources().getString(R.string.eal_stations).split(" ");
        String[] eal_stations_long = getResources().getString(R.string.eal_stations_long).split(";");
        String[] tml_stations = getResources().getString(R.string.tml_stations).split(" ");
        String[] tml_stations_long = getResources().getString(R.string.tml_stations_long).split(";");

        stations.put("EAL", getResources().getString(R.string.eal_stations).split(" "));
        stations.put("TML", getResources().getString(R.string.tml_stations).split(" "));
        stations.put("KTL", getResources().getString(R.string.ktl_stations).split(" "));
        stations.put("AEL", getResources().getString(R.string.ael_stations).split(" "));
        stations.put("DRL", getResources().getString(R.string.drl_stations).split(" "));
        stations.put("ISL", getResources().getString(R.string.isl_stations).split(" "));
        stations.put("TCL", getResources().getString(R.string.tcl_stations).split(" "));
        stations.put("TKL", getResources().getString(R.string.tkl_stations).split(" "));
        stations.put("TWL", getResources().getString(R.string.twl_stations).split(" "));
        stations.put("SIL", getResources().getString(R.string.sil_stations).split(" "));


        // Request permissions
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_NETWORK_STATE
        }, 1001);


        // Declare handlers and runnables
        Handler handler = new Handler();
        Runnable overview = () -> {
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

                    ealTrips.addAll(ServerUtils.getEALTripData(eal_data));
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

                    tmlTrips.addAll(ServerUtils.getTMLTripData(tml_data, mapUtils));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            CompletableFuture.allOf(CompletableFuture.runAsync(ealOv), CompletableFuture.runAsync(tmlOv))
                    .thenRunAsync(() -> {
                        updateTrainTrips();
                    });
        };


        // Fetch cipher from GitHub
        CompletableFuture.supplyAsync(() -> {
                    String json = "";
                    try {
                        URL url = new URL("https://raw.githubusercontent.com/i998979/Real-Time-Train-Status-Private/refs/heads/main/Real-Time-Train-Status.json");
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.connect();

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = "";
                        while ((line = in.readLine()) != null) {
                            json += line;
                        }
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return json;
                })
                .thenAccept(json -> {
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject cipher = jsonObject.getJSONObject("cipher");
                        cipher_eal = cipher.getString("eal");
                        cipher_tml = cipher.getString("tml");
                        cipher_roctec = cipher.getString("roctec");
                        JSONObject encrypted = jsonObject.getJSONObject("encrypted");
                        encrypted_eal = encrypted.getString("eal");
                        encrypted_tml = encrypted.getString("tml");
                        encrypted_roctec = encrypted.getString("roctec");

                        link_eal = AES.decrypt(cipher_eal, code);
                        link_tml = AES.decrypt(cipher_tml, code);
                        link_roctec = AES.decrypt(cipher_roctec, code);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .thenRun(() -> {
                    // Check user's code input
                    if (link_eal == null || AES.encrypt(link_eal) == null || !AES.encrypt(link_eal).equals(encrypted_eal)) {
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
                                    link_roctec = AES.decrypt(cipher_roctec, code);

                                    handler.post(overview);
                                });
                        runOnUiThread(() -> {
                            builder.show();
                        });
                    }
                    // Already checked
                    else {
                        handler.post(overview);
                    }
                });


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
                if (line.equals("EAL")) marker = ealStationMarkers.get(eal_stations[0]);
                if (line.equals("TML")) marker = tmlStationMarkers.get(tml_stations[0]);

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
                        marker = ealTrainMarkers.get(ealIdList.get(0));

                if (line.equals("TML"))
                    if (!tmlIdList.isEmpty())
                        marker = tmlTrainMarkers.get(tmlIdList.get(0));


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
            if (line.equals("EAL")) marker = ealStationMarkers.get(eal_stations[newVal]);
            if (line.equals("TML")) marker = tmlStationMarkers.get(tml_stations[newVal]);

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
                    marker = ealTrainMarkers.get(ealIdList.get(newVal));

            if (line.equals("TML"))
                if (newVal < tmlIdList.size())
                    marker = tmlTrainMarkers.get(tmlIdList.get(newVal));


            if (marker != null) {
                CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f);
                mMap.animateCamera(cu, 1000, null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (infoHandler != null) infoHandler.post(infoRunnable);
    }


    public void updateTrainTrips() {
        // EAL
        for (Trip trip : ealTrips) {
            CompletableFuture.supplyAsync(() -> {
                return mapUtils.getTrainAt(trip, "EAL");
            }).thenAccept(latLng -> {
                runOnUiThread(() -> {
                    if (latLng != null) {
                        // Create a new marker if not exist, or reuse old one if exist
                        Marker train = ealTrainMarkers.get(trip.trainId);
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

                        ealTrainMarkers.put(trip.trainId, train);
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
                        Marker train = tmlTrainMarkers.get(trip.trainId);
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

                        tmlTrainMarkers.put(trip.trainId, train);
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

    public Runnable fetchRoctec(String station) {
        Runnable runnable = () -> {
            try {
                String data = "";
                URL url = new URL(link_roctec);
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

                Log.d("tagg", "Roctec complete");
                roctecTrains.put(station, NextTrainUtils.getRoctecTrainData(data, station));
            } catch (Exception e) {
            }
        };
        return runnable;
    }

    public Runnable fetchNextTrain(String line0, String station) {
        Runnable runnable = () -> {
            try {
                String data = "";

                URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?" +
                        "line=" + line0 + "&sta=" + station.toUpperCase() + "&lang=en");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    data += line;
                }

                Log.d("tagg", "NextTrain complete");
                trains.put(line0, station, NextTrainUtils.getTrainData(data, line0, station));
            } catch (Exception e) {
            }
        };
        return runnable;
    }


    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setMapToolbarEnabled(false);

        drawLines();
        addStations();


        CameraUpdate cu;
        if (!pref.contains("zoom")) {
            cu = CameraUpdateFactory.newLatLngBounds(new LatLngBounds.Builder()
                    .include(Utils.getLatLng(getResources().getString(R.string.adm)))
                    .include(Utils.getLatLng(getResources().getString(R.string.uni)))
                    .include(Utils.getLatLng(getResources().getString(R.string.low)))
                    .include(Utils.getLatLng(getResources().getString(R.string.lmc)))
                    .include(Utils.getLatLng(getResources().getString(R.string.lop)))
                    .include(Utils.getLatLng(getResources().getString(R.string.tum)))
                    .include(Utils.getLatLng(getResources().getString(R.string.wks)))
                    .build(), Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels, 250);
        } else {
            cu = CameraUpdateFactory.newLatLngZoom(
                    new LatLng(Double.parseDouble(pref.getString("lat", "0")), Double.parseDouble(pref.getString("lng", "0"))),
                    pref.getFloat("zoom", 0));
        }
        mMap.moveCamera(cu);

        mMap.setOnCameraMoveListener(() -> {
            CameraPosition position = mMap.getCameraPosition();

            pref.edit().putString("lat", position.target.latitude + "")
                    .putString("lng", position.target.longitude + "")
                    .putFloat("zoom", position.zoom).apply();
        });


        mMap.setInfoWindowAdapter(new TrainInfoAdapter(this));


        mMap.setOnMarkerClickListener(marker -> {
            Projection projection = mMap.getProjection();
            LatLng markerPosition = marker.getPosition();
            Point markerPoint = projection.toScreenLocation(markerPosition);
            Point targetPoint = new Point(markerPoint.x, (int) (markerPoint.y - findViewById(android.R.id.content).getHeight() / 3));
            LatLng targetPosition = projection.fromScreenLocation(targetPoint);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(targetPosition), 500, null);

            infoHandler.removeCallbacks(infoRunnable);

            updateStation(marker);
            marker.showInfoWindow();

            infoRunnable = new Runnable() {
                @Override
                public void run() {
                    String station = stationMarkers.get(marker);
                    List<CompletableFuture<?>> futures = new ArrayList<>();

                    for (Map.Entry<String, String[]> entry1 : stations.entrySet()) {
                        if (Arrays.stream(entry1.getValue()).anyMatch(s -> s.equalsIgnoreCase(station))) {
                            futures.add(CompletableFuture
                                    .runAsync(fetchNextTrain(entry1.getKey(), station))
                                    .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS));
                        }
                    }
                    futures.add(CompletableFuture
                            .runAsync(fetchRoctec(station))
                            .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS));

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}))
                            .thenRunAsync(() -> {
                                updateStation(marker);
                                MapsActivity.this.runOnUiThread(() -> {
                                    marker.showInfoWindow();
                                });

                                infoHandler.postDelayed(this, 5000);
                            });
                }
            };
            infoHandler.post(infoRunnable);


            mMap.setOnInfoWindowCloseListener(mar -> {
                if (!stationMarkers.containsKey(mar)) return;
                infoHandler.removeCallbacks(infoRunnable);
            });


            return true;
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            if (!stationMarkers.containsKey(marker)) return;

            String tag = (String) marker.getTag();

            if (tag.contains(ServerType.NEXT_TRAIN.name())) {
                tag = tag.replaceFirst(ServerType.NEXT_TRAIN.name(), ServerType.ROCTEC.name());
                type = ServerType.ROCTEC;
            } else if (tag.contains(ServerType.ROCTEC.name())) {
                tag = tag.replaceFirst(ServerType.ROCTEC.name(), ServerType.NEXT_TRAIN.name());
                type = ServerType.NEXT_TRAIN;
            }

            marker.setTag(tag);

            updateStation(marker);
            marker.showInfoWindow();
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


    /**
     * Update specified station marker with existing data
     *
     * @param marker Marker to update
     */
    public void updateStation(Marker marker) {
        Log.d("tagg", "updateStation");

        if (!stationMarkers.containsKey(marker))
            return;

        if (type == ServerType.NEXT_TRAIN) {
            String station = stationMarkers.get(marker);

            if (!trains.containsColumn(station)) return;


            String snippet = "";
            for (Map.Entry<String, List<Train>> entry1 : trains.column(station).entrySet()) {
                String line = entry1.getKey();

                for (Train train : trains.get(line, station)) {
                    snippet += train.line + "," + train.dest + "," + train.route + "," + train.plat + "," + train.ttnt + "," + train.currtime + ";";
                }
            }

            String snippet0 = snippet;
            runOnUiThread(() -> {
                marker.setSnippet(snippet0);
            });
        } else {
            String station = stationMarkers.get(marker);

            if (!roctecTrains.containsKey(station)) return;


            String snippet = "";
            for (Train train : roctecTrains.get(station)) {
                snippet += train.line + "," + train.dest + "," + train.route + "," + train.plat + "," + train.ttnt + "," + train.currtime + ";";
            }

            String snippet0 = snippet;
            runOnUiThread(() -> {
                marker.setSnippet(snippet0);
            });
        }
    }

    public void drawLines() {
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_main)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_rac)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_lmc)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tml_main)), Utils.getColor(this, "tml"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.ktl_main)), Utils.getColor(this, "ktl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.ael_main)), Utils.getColor(this, "ael"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.drl_main)), Utils.getColor(this, "drl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.isl_main)), Utils.getColor(this, "isl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tcl_main)), Utils.getColor(this, "tcl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tkl_main)), Utils.getColor(this, "tkl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.twl_main)), Utils.getColor(this, "twl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.sil_main)), Utils.getColor(this, "sil"));
    }

    public void addStations() {
        for (String station : getResources().getString(R.string.eal_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:eal:" + station + ":" + type);
            ealStationMarkers.put(station, marker);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.tml_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tml:" + station + ":" + type);
            tmlStationMarkers.put(station, marker);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.ktl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:ktl:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.ael_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:ael:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.drl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:drl:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.isl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:isl:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.tcl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tcl:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.tkl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tkl:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.twl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:twl:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.sil_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:sil:" + station + ":" + type);
            stationMarkers.put(marker, station);
        }
    }
}