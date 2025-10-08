package to.epac.factorycraft.realtimetrainstatus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.HashBasedTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import to.epac.factorycraft.realtimetrainstatus.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // EAL
    public List<Trip> ealTrips;
    // Train ID : Marker
    public Map<String, Marker> ealTrainMarkers;
    // EAL station markers, used by menu to navigate positions
    public Map<String, Marker> ealStationMarkers;

    // TML
    public List<Trip> tmlTrips;
    public Map<String, Marker> tmlTrainMarkers;
    public Map<String, Marker> tmlStationMarkers;

    public NumberPicker stationPicker;
    public NumberPicker trainPicker;

    // Line : Stations
    public Map<String, String[]> stations;
    // Station : Marker
    public Map<Marker, String> stationMarkers;
    public Map<String, List<Train>> roctecTrains;
    public HashBasedTable<String, String, List<Train>> trains;
    public HashBasedTable<String, String, String> trainNos;

    private Handler tripHandler;
    private Runnable tripRunnable;
    private Handler trainNoHandler;
    private Runnable trainNoRunnable;
    private Handler hkoHandler;
    private Runnable hkoRunnable;
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
    private String link_ktl = "";
    private String link_isl = "";
    private String link_twl = "";
    private String link_roctec = "";
    private String link_nexttrain = "";

    // link + code = cipher
    private String cipher_eal = "";
    private String cipher_tml = "";
    private String cipher_ktl = "";
    private String cipher_isl = "";
    private String cipher_twl = "";
    private String cipher_roctec = "";
    private String cipher_nexttrain = "";

    // link + secret = encrypted
    private String encrypted_eal = "";
    private String encrypted_tml = "";
    private String encrypted_ktl = "";
    private String encrypted_isl = "";
    private String encrypted_twl = "";
    private String encrypted_roctec = "";
    private String encrypted_nexttrain = "";

    private List<Integer> weatherIcons;
    private int temperature;

    public static String line = "EAL";

    public enum ServerType {
        ROCTEC,
        NEXT_TRAIN
    }


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
        trainNos = HashBasedTable.create();
        roctecTrains = new HashMap<>();

        infoHandler = new Handler(getMainLooper());

        mapUtils = new MapUtils(getApplicationContext());
        // Code related
        pref = getSharedPreferences("RealTimeTrainStatus", MODE_PRIVATE);
        code = pref.getString("code", "default");

        weatherIcons = new ArrayList<>();

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
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CHANGE_NETWORK_STATE}, 1001);


        // StationPicker and TrainPicker
        stationPicker = findViewById(R.id.stationPicker);
        stationPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        stationPicker.setSaveFromParentEnabled(false);
        stationPicker.setSaveEnabled(false);
        stationPicker.setWrapSelectorWheel(false);
        stationPicker.setDisplayedValues(new String[]{"0000000000000000000000000000000000000000"});
        stationPicker.setTextColor(Color.GRAY);

        trainPicker = findViewById(R.id.trainPicker);
        trainPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
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

        findViewById(R.id.trainsLayout).setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.layout_trip, null);

            EditText editText = view.findViewById(R.id.trainNoList);
            builder.setView(view)
                    .setTitle("Train No. List Importer")
                    .setPositiveButton("OK", (dialog, which) -> {

                    }).show();

            return false;
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


        // Declare handlers and runnables
        ExecutorService tripExecutor = Executors.newFixedThreadPool(2);
        tripHandler = new Handler(Looper.getMainLooper());
        tripRunnable = new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> ealOvFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running ealOvFuture");
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

                                ealTrips.clear();
                                ealTrips.addAll(ServerUtils.getEALTripData(eal_data));

                                trainNos.row("EAL").clear();
                                trainNos.putAll(TrainNoUtils.getEALTrainNos(eal_data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, tripExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS);

                CompletableFuture<Void> tmlOvFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running tmlOvFuture");
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

                                tmlTrips.clear();
                                tmlTrips.addAll(ServerUtils.getTMLTripData(tml_data, mapUtils));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, tripExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS);


                CompletableFuture.allOf(ealOvFuture, tmlOvFuture)
                        .thenRunAsync(() -> {
                            updateTrainTrips();
                        });

                tripHandler.postDelayed(this, 5000);
            }
        };


        ExecutorService trainNoExecutor = Executors.newFixedThreadPool(3);
        trainNoHandler = new Handler(Looper.getMainLooper());
        trainNoRunnable = new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> ktlTrainNoFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running ktlTrainNo");
                            try {
                                String ktl_data = "";

                                URL url = new URL(link_ktl);
                                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                conn.connect();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String line = "";
                                while ((line = in.readLine()) != null) {
                                    ktl_data += line;
                                }
                                in.close();

                                trainNos.row("KTL").clear();
                                trainNos.putAll(TrainNoUtils.getKTLTrainNos(ktl_data));
                            } catch (Exception e) {
                            }
                        }, trainNoExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS)
                        .exceptionally(e -> {
                            e.printStackTrace();
                            return null;
                        });

                CompletableFuture<Void> islTrainNoFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running islTrainNo");
                            try {
                                String isl_data = "";

                                URL url = new URL(link_isl);
                                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                conn.setRequestProperty("x-api-key", "gRSyLCpSg97wxGIAhaovD4bN0fY4Z0jYa5xeoEn9");
                                conn.connect();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String line = "";
                                while ((line = in.readLine()) != null) {
                                    isl_data += line;
                                }
                                in.close();

                                trainNos.row("ISL").clear();
                                trainNos.putAll(TrainNoUtils.getISLTrainNos(isl_data));
                            } catch (Exception e) {
                            }
                        }, trainNoExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS)
                        .exceptionally(e -> {
                            e.printStackTrace();
                            return null;
                        });

                CompletableFuture<Void> twlTrainNoFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running twlTrainNo");
                            try {
                                String twl_data = "";

                                Log.d("tagg", link_twl);
                                URL url = new URL(link_twl);
                                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                conn.setRequestProperty("x-api-key", "cWEnQqRK0taxxMVCMpNHK3kqQgcTB28tv3lPJRvb");
                                conn.connect();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String line = "";
                                while ((line = in.readLine()) != null) {
                                    twl_data += line;
                                }
                                in.close();

                                trainNos.row("TWL").clear();
                                trainNos.putAll(TrainNoUtils.getTWLTrainNos(twl_data));
                            } catch (Exception e) {
                            }
                        }, trainNoExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS)
                        .exceptionally(e -> {
                            e.printStackTrace();
                            return null;
                        });

                trainNoHandler.postDelayed(this, 5000);
            }
        };


        // Fetch HKO weather data
        ExecutorService hkoExecutor = Executors.newFixedThreadPool(2);
        hkoHandler = new Handler(Looper.getMainLooper());
        hkoRunnable = new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> rhrreadFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running rhrreadFuture");
                            try {
                                String data = "";

                                URL url = new URL("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=tc");
                                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                conn.connect();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String line = "";
                                while ((line = in.readLine()) != null) {
                                    data += line;
                                }
                                in.close();

                                updateIconAndTemperature(data);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, hkoExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS);

                CompletableFuture<Void> warnsumFuture = CompletableFuture.runAsync(() -> {
                            Log.d("tagg", Thread.currentThread() + " Running warnsumFuture");
                            try {
                                String data = "";

                                URL url = new URL("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=tc");
                                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                conn.connect();

                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String line = "";
                                while ((line = in.readLine()) != null) {
                                    data += line;
                                }
                                in.close();

                                updateWeatherWarnings(data);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, hkoExecutor)
                        .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS);

                CompletableFuture.allOf(warnsumFuture, rhrreadFuture);

                hkoHandler.postDelayed(this, 5000 /*60000*/);
            }
        };


        // Fetch cipher from GitHub
        CompletableFuture.supplyAsync(() -> {
                    String json = "";
                    try {
                        URL url = new URL("https://raw.githubusercontent.com/i998979/Real-Time-Train-Status-Private/refs/heads/main/Real-Time-Train-Status.json");
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
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
                        cipher_ktl = cipher.getString("ktl");
                        cipher_isl = cipher.getString("isl");
                        cipher_twl = cipher.getString("twl");
                        cipher_roctec = cipher.getString("roctec");
                        cipher_nexttrain = cipher.getString("nexttrain");
                        JSONObject encrypted = jsonObject.getJSONObject("encrypted");
                        encrypted_eal = encrypted.getString("eal");
                        encrypted_tml = encrypted.getString("tml");
                        encrypted_ktl = encrypted.getString("ktl");
                        encrypted_isl = encrypted.getString("isl");
                        encrypted_twl = encrypted.getString("twl");
                        encrypted_roctec = encrypted.getString("roctec");
                        encrypted_nexttrain = encrypted.getString("nexttrain");

                        link_eal = AES.decrypt(cipher_eal, code);
                        link_tml = AES.decrypt(cipher_tml, code);
                        link_ktl = AES.decrypt(cipher_ktl, code);
                        link_isl = AES.decrypt(cipher_isl, code);
                        link_twl = AES.decrypt(cipher_twl, code);
                        link_roctec = AES.decrypt(cipher_roctec, code);
                        link_nexttrain = AES.decrypt(cipher_nexttrain, code);
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
                                    link_ktl = AES.decrypt(cipher_ktl, code);
                                    link_isl = AES.decrypt(cipher_isl, code);
                                    link_twl = AES.decrypt(cipher_twl, code);
                                    link_roctec = AES.decrypt(cipher_roctec, code);
                                    link_nexttrain = AES.decrypt(cipher_nexttrain, code);

                                    tripHandler.post(tripRunnable);
                                    trainNoHandler.post(trainNoRunnable);
                                    hkoHandler.post(hkoRunnable);
                                });
                        runOnUiThread(() -> {
                            builder.show();
                        });
                    }
                    // Already checked
                    else {
                        tripHandler.post(tripRunnable);
                        trainNoHandler.post(trainNoRunnable);
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (tripHandler != null && tripHandler.hasCallbacks(tripRunnable))
            tripHandler.removeCallbacks(tripRunnable);
        if (trainNoHandler != null && trainNoHandler.hasCallbacks(trainNoRunnable))
            trainNoHandler.removeCallbacks(trainNoRunnable);
        if (hkoHandler != null && hkoHandler.hasCallbacks(hkoRunnable))
            hkoHandler.removeCallbacks(hkoRunnable);
        if (infoHandler != null && infoHandler.hasCallbacks(infoRunnable))
            infoHandler.removeCallbacks(infoRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (tripHandler != null && !tripHandler.hasCallbacks(tripRunnable))
            tripHandler.post(tripRunnable);
        if (trainNoHandler != null && !trainNoHandler.hasCallbacks(trainNoRunnable))
            trainNoHandler.post(trainNoRunnable);
        if (hkoHandler != null && !hkoHandler.hasCallbacks(hkoRunnable))
            hkoHandler.post(hkoRunnable);
        if (infoHandler != null && !infoHandler.hasCallbacks(infoRunnable))
            infoHandler.post(infoRunnable);
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

    public Runnable getRoctecRunnable(String station) {
        Log.d("tagg", "Roctec start");

        Runnable runnable = () -> {
            Log.d("tagg", "Running fetchRoctec " + Thread.currentThread());
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
                e.printStackTrace();
            }
        };
        return runnable;
    }

    public Runnable getNextTrainRunnable(String line0, String station) {
        Log.d("tagg", "NextTrain start");

        Runnable runnable = () -> {
            Log.d("tagg", "Running fetchNextTrain " + Thread.currentThread());
            try {
                String data = "";

                URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?" +
                        "line=" + line0 + "&sta=" + station.toUpperCase() + "&lang=en");
                // URL url = new URL(link_nexttrain + "?sta=" + station.toUpperCase());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("api-key", "f14209ac1c6e412f9bbd006470a40d39");
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


    public void updateIconAndTemperature(String data) {
        try {
            JSONObject root = new JSONObject(data);

            // Extract all icons into int[]
            JSONArray iconArray = root.getJSONArray("icon");
            weatherIcons.clear();
            for (int i = 0; i < iconArray.length(); i++) {
                int resId = getResources().getIdentifier("pic" + iconArray.getInt(i), "drawable", getPackageName());
                weatherIcons.add(resId);
            }

            // Find temperature for "香港天文台"
            JSONObject temperatureObj = root.getJSONObject("temperature");
            JSONArray tempData = temperatureObj.getJSONArray("data");
            for (int i = 0; i < tempData.length(); i++) {
                JSONObject entry = tempData.getJSONObject(i);
                if (entry.getString("place").equals("香港天文台")) {
                    temperature = entry.getInt("value");
                    break;
                }
            }
        } catch (JSONException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateWeatherWarnings(String data) {
        try {
            JSONObject root = new JSONObject(data);

            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject warning = root.getJSONObject(key);

                if (warning.has("actionCode")) {
                    String actionCode = warning.getString("actionCode");
                    if ("CANCEL".equalsIgnoreCase(actionCode)) continue;
                }

                if (warning.has("code")) {
                    String code = warning.getString("code");

                    int resId = getResources().getIdentifier(code.toLowerCase(), "drawable", getPackageName());
                    weatherIcons.add(resId);
                }
            }
        } catch (JSONException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        mMap.setInfoWindowAdapter(new TrainInfoAdapter(this));


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


        mMap.setOnMarkerClickListener(marker -> {
            if (!stationMarkers.containsKey(marker)) {
                marker.showInfoWindow();
                return false;
            } else {
                infoHandler.removeCallbacks(infoRunnable);

                updateStation(marker);
                createDialog(marker, true);


                infoRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("tagg", "Running infoRunnable " + Thread.currentThread());
                        String station = stationMarkers.get(marker);
                        List<CompletableFuture<?>> futures = new ArrayList<>();
                        List<Runnable> runnables = new ArrayList<>();


                        for (Map.Entry<String, String[]> entry1 : stations.entrySet()) {
                            if (Arrays.stream(entry1.getValue()).anyMatch(s -> s.equalsIgnoreCase(station)))
                                runnables.add(getNextTrainRunnable(entry1.getKey(), station));
                        }
                        runnables.add(getRoctecRunnable(station));


                        ExecutorService executor = Executors.newFixedThreadPool(runnables.size());
                        for (Runnable runnable : runnables) {
                            futures.add(CompletableFuture.runAsync(runnable, executor)
                                    .completeOnTimeout(null, 5000, TimeUnit.MILLISECONDS));
                        }


                        infoFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}))
                                .thenRunAsync(() -> {
                                    updateStation(marker);
                                    runOnUiThread(() -> {
                                        createDialog(marker, false);
                                    });

                                    infoHandler.postDelayed(this, 5000);
                                }).thenRun(() -> {
                                    executor.shutdown();
                                });
                    }
                };
                infoHandler.post(infoRunnable);


                return true;
            }
        });
    }


    View view = null;
    AlertDialog dialog = null;

    public void createDialog(Marker marker, boolean create) {
        Log.d("tagg", "createDialog " + marker.getSnippet());
        Log.d("tagg", "createDialog1 " + marker.getTag());

        if (create) {
            if (dialog != null)
                dialog.cancel();
            view = null;
        }

        if (marker.getTag() == null) return;

        String tag = marker.getTag().toString();
        String[] datas = new String[]{};
        if (marker.getSnippet() != null)
            datas = marker.getSnippet().split(";");

        // Station layout
        if (tag.startsWith("station")) {
            String line = tag.split(":")[1].toUpperCase();
            String station = tag.split(":")[2];

            if (create) {
                if (ServerType.valueOf(pref.getString("type", ServerType.NEXT_TRAIN.name())) == ServerType.NEXT_TRAIN)
                    view = getLayoutInflater().inflate(R.layout.layout_info, null);
                else
                    view = getLayoutInflater().inflate(R.layout.layout_roctec, null);
            }

            LinearLayout infoLayout = view.findViewById(R.id.infoLayout);
            LinearLayout weatherLayout = view.findViewById(R.id.weatherLayout);
            TableLayout stationLayout = view.findViewById(R.id.stationLayout);
            TextView lastUpdateTv = infoLayout.findViewById(R.id.lastUpdate);

            if (!create) {
                if (stationLayout.getChildCount() > 1)
                    stationLayout.removeViews(1, stationLayout.getChildCount() - 1);
            }


            // Set weather layout
            weatherLayout.removeAllViews();
            for (int resId : weatherIcons) {
                ImageView imageView = new ImageView(this);
                imageView.setImageResource(resId);
                int widthDp = 20;
                int heightDp = 20;
                int paddingDp = 2;

                float scale = getResources().getDisplayMetrics().density;
                int widthPx = (int) (widthDp * scale + 0.5f);
                int heightPx = (int) (heightDp * scale + 0.5f);
                int paddingPx = (int) (paddingDp * scale + 0.5f);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthPx, heightPx);
                imageView.setLayoutParams(params);
                imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                weatherLayout.addView(imageView);
            }
            TextView tempTv = new TextView(this);
            tempTv.setText(temperature + "°C");
            tempTv.setTextColor(Color.WHITE);
            weatherLayout.addView(tempTv);


            // Set station name and background color
            TextView stationTv = view.findViewById(R.id.station);
            stationTv.setText(Utils.getStationName(this, station));
            stationTv.setBackgroundColor(Color.GRAY);


            String serverLine = "";
            String roctecLine = "";
            int i = 0;
            for (String snippet : datas) {
                String[] data = snippet.split(",");

                if (data.length <= 1) continue;

                TableRow trainRow = new TableRow(this);
                trainRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));

                // NextTrain
                if (ServerType.valueOf(pref.getString("type", ServerType.NEXT_TRAIN.name())) == ServerType.NEXT_TRAIN) {
                    if (serverLine.isEmpty() || !serverLine.equalsIgnoreCase(data[0])) {
                        serverLine = data[0];

                        TableRow lineRow = new TableRow(this);

                        TextView lineTv = new TextView(this);
                        lineTv.setBackgroundColor(Color.parseColor(Utils.getColor(this, serverLine)));
                        lineTv.setTextColor(Color.WHITE);

                        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1);
                        params.span = 3;
                        lineTv.setLayoutParams(params);
                        lineTv.setTypeface(null, Typeface.BOLD);
                        lineTv.setText(Utils.getLineName(serverLine));

                        lineRow.addView(lineTv);
                        stationLayout.addView(lineRow);

                        i = 0;
                    }

                    if (i % 2 != 0) trainRow.setBackgroundColor(Color.parseColor("#C5D9E4"));

                    TextView dest = new TextView(this);
                    dest.setTextColor(Color.BLACK);
                    dest.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
                    dest.setText(Utils.getStationName(this, data[1]) + (data[2].equals("RAC") ? " via Racecourse " : " "));

                    TextView plat = new TextView(this);
                    plat.setTextColor(Color.BLACK);
                    plat.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
                    plat.setText(data[3]);

                    TextView ttnt = new TextView(this);
                    ttnt.setTextColor(Color.BLACK);
                    ttnt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
                    ttnt.setText(data[4]);

                    lastUpdateTv.setText(data.length >= 6 ? data[5] : "Never");

                    trainRow.addView(dest);
                    trainRow.addView(plat);
                    trainRow.addView(ttnt);

                    stationLayout.addView(trainRow);
                }
                // Roctec
                else {
                    // Get the first row
                    TableRow firstRow = (TableRow) stationLayout.getChildAt(0);

                    // Measure the widths of cells in the first row
                    int columnCount = firstRow.getChildCount();
                    int[] columnWidths = new int[columnCount];
                    for (int j = 0; j < columnCount; j++) {
                        View cell = firstRow.getChildAt(j);
                        cell.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        columnWidths[j] = cell.getMeasuredWidth();
                    }


                    if (roctecLine.isEmpty() || !roctecLine.equalsIgnoreCase(data[0])) {
                        roctecLine = data[0];

                        TableRow lineRow = new TableRow(this);

                        TextView lineTv = new TextView(this);
                        lineTv.setBackgroundColor(Color.parseColor(Utils.getColor(this, roctecLine)));
                        lineTv.setTextColor(Color.WHITE);

                        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1);
                        params.span = 4;
                        lineTv.setLayoutParams(params);
                        lineTv.setTypeface(null, Typeface.BOLD);
                        lineTv.setText(Utils.getLineName(roctecLine));

                        lineRow.addView(lineTv);
                        stationLayout.addView(lineRow);

                        i = 0;
                    }

                    if (i % 2 != 0) trainRow.setBackgroundColor(Color.parseColor("#C5D9E4"));

                    TextView dest = new TextView(this);
                    // TODO: Make color GREY if the train is NIS
                    dest.setTextColor(Color.BLACK);
                    TableRow.LayoutParams destParam = new TableRow.LayoutParams(columnWidths[0], TableRow.LayoutParams.WRAP_CONTENT);
                    destParam.setMargins(10, 0, 10, 0);
                    dest.setLayoutParams(destParam);
                    dest.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    dest.setSelected(true);
                    dest.setSingleLine(true);
                    dest.setMarqueeRepeatLimit(-1);
                    String dest0 = Utils.getStationName(this, data[1]) + " ";
                    if (roctecLine.equals("EAL") || roctecLine.equals("NSL")) {
                        try {
                            if (data[2].matches(".*[BGKN].*")) dest0 += "via Racecourse ";
                        } catch (Exception e) {
                        }
                    }
                    dest.setText(dest0);

                    TextView td = new TextView(this);
                    td.setTextColor(Color.BLACK);
                    td.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    td.setText(data[2]);

                    TextView train = new TextView(this);
                    train.setTextColor(Color.BLACK);
                    TableRow.LayoutParams trainParam = new TableRow.LayoutParams(columnWidths[2], TableRow.LayoutParams.WRAP_CONTENT);
                    trainParam.setMargins(10, 0, 10, 0);
                    train.setLayoutParams(trainParam);
                    train.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    train.setSelected(true);
                    train.setSingleLine(true);
                    train.setMarqueeRepeatLimit(-1);
                    if (roctecLine.equals("KTL") || roctecLine.equals("TWL") || roctecLine.equals("ISL") || roctecLine.equals("TKL")) {
                        String td0;
                        try {
                            td0 = Integer.parseInt(data[2].substring(2)) + "";

                            if (trainNos.contains(roctecLine, data[2]))
                                train.setText(trainNos.get(roctecLine, data[2]));
                            else if (trainNos.contains(roctecLine, td0))
                                train.setText(trainNos.get(roctecLine, td0));
                            else if (trainNos.row(roctecLine).keySet().stream().anyMatch(s -> s.endsWith(td0)))
                                train.setText(trainNos.get(roctecLine, trainNos.row(roctecLine).keySet().stream().filter(s -> s.endsWith(td0)).findFirst().get()));
                            else
                                train.setText("-");
                        } catch (NumberFormatException e) {
                            train.setText("-");
                        }
                    } else {
                        if (trainNos.contains(roctecLine, data[2]))
                            train.setText(trainNos.get(roctecLine, data[2]));
                        else
                            train.setText("-");
                    }

                    TextView plat = new TextView(this);
                    plat.setTextColor(Color.BLACK);
                    plat.setLayoutParams(new TableRow.LayoutParams(columnWidths[3], TableRow.LayoutParams.WRAP_CONTENT));
                    plat.setText(data[3]);

                    TextView ttnt = new TextView(this);
                    ttnt.setTextColor(Color.BLACK);
                    ttnt.setLayoutParams(new TableRow.LayoutParams(columnWidths[4], TableRow.LayoutParams.WRAP_CONTENT));
                    ttnt.setText(data[4]);

                    lastUpdateTv.setText(data.length >= 6 ? data[5] : "Never");


                    trainRow.addView(dest);
                    trainRow.addView(td);
                    trainRow.addView(train);
                    trainRow.addView(plat);
                    trainRow.addView(ttnt);

                    stationLayout.addView(trainRow);
                }

                i++;
            }
        }

        if (create) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(view)
                    .setNegativeButton("Cancel", (dialog1, which) -> {
                        dialog1.cancel();
                    })
                    .setPositiveButton(ServerType.valueOf(pref.getString("type", ServerType.NEXT_TRAIN.name())) == ServerType.NEXT_TRAIN
                            ? "Roctec" : "Next Train", (dialog, which) -> {
                        if (ServerType.valueOf(pref.getString("type", ServerType.NEXT_TRAIN.name())) == ServerType.NEXT_TRAIN)
                            pref.edit().putString("type", ServerType.ROCTEC.name()).apply();
                        else
                            pref.edit().putString("type", ServerType.NEXT_TRAIN.name()).apply();

                        infoHandler.post(infoRunnable);
                        updateStation(marker);
                        createDialog(marker, true);
                    })
                    .setOnCancelListener(dialog1 -> {
                        infoHandler.removeCallbacks(infoRunnable);
                    });
            dialog = builder.create();
            dialog.show();
        }
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


        String station = stationMarkers.get(marker);

        if (ServerType.valueOf(pref.getString("type", ServerType.NEXT_TRAIN.name())) == ServerType.NEXT_TRAIN) {
            if (!trains.containsColumn(station)) return;
        } else {
            if (!roctecTrains.containsKey(station)) return;
        }

        String snippet = "";

        List<Train> list = new ArrayList<>();
        if (ServerType.valueOf(pref.getString("type", ServerType.NEXT_TRAIN.name())) == ServerType.NEXT_TRAIN) {
            for (Map.Entry<String, List<Train>> entry : trains.column(station).entrySet()) {
                String line = entry.getKey();

                list.addAll(trains.get(line, station));
            }
        } else {
            list.addAll(roctecTrains.get(station));
        }

        for (Train train : list.stream().sorted(Comparator.comparing(train -> Utils.getLineName(train.line))).collect(Collectors.toList())) {
            snippet += train.line + "," + train.dest + "," + train.route + "," + train.plat + "," + train.ttnt + "," + train.currtime + ";";
        }

        String snippet0 = snippet;
        runOnUiThread(() -> {
            marker.setSnippet(snippet0);
        });
    }

    public void drawLines() {
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_main)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_rac)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_low)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.eal_lmc)), Utils.getColor(this, "eal"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tml_main)), Utils.getColor(this, "tml"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.ktl_main)), Utils.getColor(this, "ktl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.ael_main)), Utils.getColor(this, "ael"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.drl_main)), Utils.getColor(this, "drl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.isl_main)), Utils.getColor(this, "isl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tcl_main)), Utils.getColor(this, "tcl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tkl_main)), Utils.getColor(this, "tkl"));
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getString(R.string.tkl_lhp)), Utils.getColor(this, "tkl"));
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
            marker.setTag("station:eal:" + station);
            ealStationMarkers.put(station, marker);

            if (stationMarkers.containsValue(station)) continue;
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.tml_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tml:" + station);
            tmlStationMarkers.put(station, marker);

            if (stationMarkers.containsValue(station)) continue;
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.ktl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:ktl:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.ael_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:ael:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.drl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:drl:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.isl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:isl:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.tcl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tcl:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.tkl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:tkl:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.twl_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:twl:" + station);
            stationMarkers.put(marker, station);
        }

        for (String station : getResources().getString(R.string.sil_stations).split(" ")) {
            if (stationMarkers.containsValue(station)) continue;

            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.mtr))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station:sil:" + station);
            stationMarkers.put(marker, station);
        }
    }
}