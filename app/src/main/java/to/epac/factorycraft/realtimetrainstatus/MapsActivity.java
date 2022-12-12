package to.epac.factorycraft.realtimetrainstatus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import to.epac.factorycraft.realtimetrainstatus.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // EAL
    public List<Trip> ealTrips;
    public Map<String, Marker> ealTrainMap;
    public Map<String, Marker> ealStationMap;
    public WebView ealWebView;
    public Map<String, List<Train>> ealTrains;
    public NumberPicker stationPicker;
    public NumberPicker trainPicker;

    // TML
    public List<Trip> tmlTrips;
    public Map<String, Marker> tmlTrainMap;
    public Map<String, Marker> tmlStationMap;
    public WebView tmlWebView;
    public Map<String, List<Train>> tmlTrains;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    MapUtils mapUtils;

    SharedPreferences pref;
    // https://stackoverflow.com/questions/69886446/aes-encryption-and-decryption-java
    // The comparison code is embedded into class
    private final String cipher_eal = "7hZOAWENNiAjv2M31HzdClndtJ6Aj9Z7LVyrMAdqy9tp+15Z859kS0PCjtVmGGdl";
    private final String cipher_tml = "RoXQs1jcAUiiHtLN35D9oV+kKWtyR0pyto5PTYW31Cjdzqf8iRV9C5PM+VjBAdZLE2LBnVl0a2sBvd1hjJ5EvnO1wBvpepansIYDKHCiPODboqQKelCXVn+WWAK1Q41+";
    private String code = "";
    private String link_eal = "";
    private String link_tml = "";
    private final String encrypted_eal = "wt2wS84uiY8qZrmJUO4Vl8FCiVoYnrfqmLuYCSfzQ6gYcM246hUutb6ZqSjn1x1O";
    private final String encrypted_tml = "e7gg+sJ4PRy3zatp8dI8ys1VRSj/UxdA4vWoXjah6tiS5XSGc1SSwiHPtnN5x6rlX7SCNNPSxEDWCATWOPKEwbY4pSENdveBLOXTT0EOwwFVbJmcV5C7BvbWrslWM0N2";

    public static String line = "EAL";


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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

        mapUtils = new MapUtils(getApplicationContext());
        // Code related
        pref = getSharedPreferences("RealTimeTrainStatus", MODE_PRIVATE);
        code = pref.getString("code", "default");
        link_eal = AES.decrypt(cipher_eal, code);

        // Station names
        String[] eal_stations = getResources().getString(R.string.erl_stations).split(" ");
        String[] eal_stations_long = getResources().getString(R.string.erl_stations_long).split(";");
        String[] tml_stations = getResources().getString(R.string.tml_stations).split(" ");
        String[] tml_stations_long = getResources().getString(R.string.tml_stations_long).split(";");


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
                Map<String, String> headers = new HashMap<>();
                headers.put("x-api-key", "QkmjCRYvXt6o89UdZAvoXa49543NxOtU2tBhQQDQ");
                ealWebView.loadUrl(AES.decrypt(cipher_eal, code));
                tmlWebView.loadUrl(AES.decrypt(cipher_tml, code), headers);
                handler.postDelayed(this, 5000);

                CompletableFuture.runAsync(() -> {
                    for (String station : eal_stations) {
                        try {
                            String eal_data = "";

                            URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?" +
                                    "line=EAL&sta=" + station.toUpperCase() + "&lang=en");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            InputStream is = conn.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));

                            String line;
                            while ((line = br.readLine()) != null) {
                                eal_data += line;
                            }

                            ealTrains.put(station, NextTrainUtils.getTrainData(eal_data, "eal", station));
                        } catch (Exception e) {
                        }
                    }

                    for (String station : tml_stations) {
                        try {
                            String tml_data = "";

                            URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?" +
                                    "line=TML&sta=" + station.toUpperCase() + "&lang=en");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            InputStream is = conn.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));

                            String line;
                            while ((line = br.readLine()) != null) {
                                tml_data += line;
                            }

                            tmlTrains.put(station, NextTrainUtils.getTrainData(tml_data, "tml", station));
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        handler.post(runnable);


        // Check user's code input
        if (AES.encrypt(link_eal) == null || !AES.encrypt(link_eal).equals(encrypted_eal)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.layout_code, null);

            EditText editText = view.findViewById(R.id.code);
            builder.setTitle("Code:")
                    .setView(view)
                    .setPositiveButton("OK", (dialog, which) -> {
                        code = editText.getText().toString();
                        pref.edit().putString("code", code).apply();
                        link_eal = AES.decrypt(cipher_eal, code);
                        link_tml = AES.decrypt(cipher_tml, code);
                        handler.post(runnable);
                    });
            builder.show();
        }


        // WebView
        ealWebView = new WebView(MapsActivity.this);
        ealWebView.setVisibility(View.GONE);
        ealWebView.getSettings().setJavaScriptEnabled(true);
        ealWebView.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");
        ealWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                ealWebView.loadUrl("javascript:window.HTMLOUT.processEAL('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        });
        ealWebView.loadUrl(AES.decrypt(cipher_eal, code));

        tmlWebView = new WebView(MapsActivity.this);
        tmlWebView.setVisibility(View.GONE);
        tmlWebView.getSettings().setJavaScriptEnabled(true);
        tmlWebView.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");
        tmlWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                tmlWebView.loadUrl("javascript:window.HTMLOUT.processTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        });
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", "QkmjCRYvXt6o89UdZAvoXa49543NxOtU2tBhQQDQ");
        tmlWebView.loadUrl(AES.decrypt(cipher_tml, code), headers);

        RelativeLayout relativeLayout = findViewById(R.id.relativeLayout);
        relativeLayout.addView(ealWebView);
        relativeLayout.addView(tmlWebView);


        // StationPicker and TrainPicker
        stationPicker = findViewById(R.id.stationPicker);
        stationPicker.setSaveFromParentEnabled(false);
        stationPicker.setSaveEnabled(false);
        stationPicker.setWrapSelectorWheel(false);
        stationPicker.setDisplayedValues(new String[]{"0000000000000000000000000000000000000000"});
        trainPicker = findViewById(R.id.trainPicker);
        trainPicker.setSaveFromParentEnabled(false);
        trainPicker.setSaveEnabled(false);
        trainPicker.setWrapSelectorWheel(false);
        trainPicker.setDisplayedValues(new String[]{"0000000000000000000000000000000000000000"});

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
                    .sorted()
                    .collect(Collectors.toList());

            List<String> tml_trains = tmlTrips.stream()
                    .map(trip -> trip.trainId + " "
                            + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                            + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : ""))
                    .sorted()
                    .collect(Collectors.toList());


            if (line.equals("EAL")) {
                trainPicker.setDisplayedValues(null);
                trainPicker.setMinValue(0);
                trainPicker.setMaxValue(Math.max(eal_trains.size() - 1, 0));
                if (eal_trains.size() > 0)
                    trainPicker.setDisplayedValues(eal_trains.toArray(new String[]{}));
            }
            if (line.equals("TML")) {
                trainPicker.setDisplayedValues(null);
                trainPicker.setMinValue(0);
                trainPicker.setMaxValue(Math.max(tml_trains.size() - 1, 0));
                if (tml_trains.size() > 0)
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
                    .sorted()
                    .collect(Collectors.toList());

            List<String> tmlIdList = tmlTrips.stream()
                    .map(trip -> trip.trainId)
                    .sorted()
                    .collect(Collectors.toList());


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


    private class JavaScriptInterface {
        @JavascriptInterface
        public void processEAL(String html) {
            ealTrips.clear();

            try {
                html = html.substring(html.indexOf("["), html.lastIndexOf("]") + 1);

                JSONArray array = new JSONArray(html);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object0 = array.getJSONObject(i);

                    String trainId = object0.getString("trainId");
                    double trainSpeed = Double.parseDouble(object0.getString("trainSpeed"));
                    int currentStationCode = Integer.parseInt(object0.getString("currentStationCode"));
                    int nextStationCode = Integer.parseInt(object0.getString("nextStationCode"));
                    int destinationStationCode = Integer.parseInt(object0.getString("destinationStationCode"));

                    JSONArray listCars = object0.getJSONArray("listCars");
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

                    long receivedTime = object0.getLong("receivedTime");
                    long ttl = object0.getLong("ttl");
                    int doorStatus = Integer.parseInt(object0.getString("doorStatus"));
                    String td = object0.getString("td");
                    int targetDistance = Integer.parseInt(object0.getString("targetDistance"));
                    int startDistance = Integer.parseInt(object0.getString("startDistance"));

                    Trip trip = new Trip(trainId, "", trainSpeed, currentStationCode, nextStationCode, destinationStationCode,
                            cars, receivedTime, ttl, doorStatus, td, targetDistance, startDistance);

                    ealTrips.add(trip);
                }

                load();
            } catch (Exception e) {
            }
        }

        @JavascriptInterface
        public void processTML(String html) {
            tmlTrips.clear();

            try {
                html = html.substring(html.indexOf("["), html.lastIndexOf("]") + 1);

                // Handle TML data
                JSONArray array = new JSONArray(html);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object0 = array.getJSONObject(i);

                    int trainSpeed = object0.getInt("trainSpeed");
                    // int carNo = object0.getInt("carNo");
                    int nextStationCode = object0.getInt("nextStationCode");
                    // String headSpares = object0.getString("headSpares");
                    long ttl = object0.getLong("ttl");
                    // boolean isTimeDateValid = object0.getBoolean("isTimeDateValid");
                    // boolean isCarNoValid = object0.getBoolean("isCarNoValid");
                    // boolean isCarTypeValid = object0.getBoolean("isCarTypeValid");
                    // int carType = object0.getInt("carType");
                    int distanceFromCurrentStation = object0.getInt("distanceFromCurrentStation");
                    // int second = object0.getInt("second");
                    boolean isDoorOpen = object0.getBoolean("isDoorOpen");
                    // int hour = object0.getInt("hour");
                    boolean isInService = object0.getBoolean("isInService");
                    // int trainLength = object0.getInt("trainLength");
                    int currentStationCode = object0.getInt("currentStationCode");
                    String trainType = object0.getString("train_type");
                    // boolean comPortConnection = object0.getBoolean("comPortConnection");
                    //  String ipAddr = object0.getString("ipAddr");
                    String trainId = object0.getString("trainId");
                    int destinationStationCode = object0.getInt("destinationStationCode");

                    JSONArray listCars = object0.getJSONArray("listCars");
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

                    // String commandReceived = object0.getString("commandReceived");
                    // boolean isCarNumberValid = object0.getBoolean("isCarNumberValid");
                    // int headByte = object0.getInt("headByte");
                    long receivedTime = object0.getLong("receivedTime");
                    // int minute = object0.getInt("minute");
                    // boolean isTimeDataAdjustment = object0.getBoolean("isTimeDataAdjustment");
                    // int month = object0.getInt("month");
                    // int year = object0.getInt("year");
                    // boolean panStatusPM1 = object0.getBoolean("panStatusPM1");
                    // boolean panStatusPM2 = object0.getBoolean("panStatusPM2");
                    // int day = object0.getInt("day");
                    // String tailSpares = object0.getString("tailSpares");

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
                    tmlTrips.add(trip);
                }

                load();
            } catch (Exception e) {
            }
        }
    }

    public void load() {
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
                            if (Integer.parseInt(trip.td.substring(2)) % 2 != 0)
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


        List<String> eal_trains = new ArrayList<>();
        for (Trip trip : ealTrips) {
            String s = trip.trainId + "(T" + (Integer.parseInt(trip.trainId) / 3) + ") " + trip.td + " "
                    + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                    + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : "");
            eal_trains.add(s);
        }
        Collections.sort(eal_trains);

        List<String> tml_trains = new ArrayList<>();
        for (Trip trip : tmlTrips) {
            String s = trip.trainId + " "
                    + Utils.mapStation(trip.currentStationCode, line) + " to " + Utils.mapStation(trip.nextStationCode, line)
                    + (trip.destinationStationCode > 0 ? "(" + Utils.mapStation(trip.destinationStationCode, line) + ")" : "");
            tml_trains.add(s);
        }
        Collections.sort(tml_trains);


        if (line.equals("EAL")) {
            trainPicker.setMinValue(0);
            trainPicker.setMaxValue(Math.max(eal_trains.size() - 1, 0));
            if (eal_trains.size() > 0)
                trainPicker.setDisplayedValues(eal_trains.toArray(new String[]{}));
        }
        if (line.equals("TML")) {
            trainPicker.setMinValue(0);
            trainPicker.setMaxValue(Math.max(tml_trains.size() - 1, 0));
            if (tml_trains.size() > 0)
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

        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.erl_main)), "#5eb7e8");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.erl_rac)), "#5eb7e8");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.erl_lmc)), "#5eb7e8");
        mapUtils.drawPolylines(mMap, Utils.getLatLngs(getResources().getString(R.string.tml_main)), "#9c2e00");

        for (String station : getResources().getString(R.string.erl_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station");
            ealStationMap.put(station, marker);
        }

        for (String station : getResources().getString(R.string.tml_stations).split(" ")) {
            String latLng = getResources().getString(getResources().getIdentifier(station, "string", getPackageName()));
            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.station))
                    .anchor(0.5f, 0.5f)
                    .zIndex(100)
                    .position(new LatLng(Double.parseDouble(latLng.split(",")[1]), Double.parseDouble(latLng.split(",")[0]))));
            marker.setTag("station");
            tmlStationMap.put(station, marker);
        }


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
            for (Map.Entry<String, Marker> entry : ealStationMap.entrySet()) {
                String station = entry.getKey();
                Marker mar = entry.getValue();

                if (!marker.getId().equals(mar.getId())) continue;
                if (!ealTrains.containsKey(station)) continue;

                String snippet = "";

                for (Train train : ealTrains.get(station)) {
                    snippet += Utils.getStationName(this, train.dest) + (train.route.equals("RAC") ? " via Racecourse " : " ")
                            + "," + train.plat + "," + train.ttnt + ";";
                }

                if (snippet.endsWith(";"))
                    snippet = snippet.substring(0, snippet.length() - 1);

                marker.setSnippet(snippet);
            }

            for (Map.Entry<String, Marker> entry : tmlStationMap.entrySet()) {
                String station = entry.getKey();
                Marker mar = entry.getValue();

                if (!marker.getId().equals(mar.getId())) continue;
                if (!tmlTrains.containsKey(station)) continue;

                String snippet = "";

                for (Train train : tmlTrains.get(station)) {
                    snippet += Utils.getStationName(this, train.dest) + " ," + train.plat + "," + train.ttnt + ";";
                }

                if (snippet.endsWith(";"))
                    snippet = snippet.substring(0, snippet.length() - 1);

                marker.setSnippet(snippet);
            }


            marker.showInfoWindow();

            return false;
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            boolean grantedAll = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    grantedAll = false;
                    break;
                }
            }

            if (grantedAll) mMap.setMyLocationEnabled(true);
        }
    }
}