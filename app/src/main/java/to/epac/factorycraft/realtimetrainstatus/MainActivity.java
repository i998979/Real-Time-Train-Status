package to.epac.factorycraft.realtimetrainstatus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    List<Trip> trips;

    public LinearLayout mainLayout;
    public TableLayout tableLayout;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trips = new ArrayList<>();

        mainLayout = findViewById(R.id.mainLayout);
        tableLayout = findViewById(R.id.tableLayout);


        requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_NETWORK_STATE
        }, 1001);


        WebView webView = new WebView(MainActivity.this);
        webView.setVisibility(View.GONE);
        mainLayout.addView(webView);


        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        });

        Map<String, String> headers = new HashMap<>();
        // webView.loadUrl("https://d30c8uozaghdca.cloudfront.net/", headers);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        });
    }

    private class JavaScriptInterface {
        @JavascriptInterface
        public void processHTML(String html) {
            try {
                html = html.substring(html.indexOf("["), html.lastIndexOf("]") + 1);

                // Format JSON to variables
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
                        JSONObject object1 = listCars.getJSONObject(j);
                        int carLoad = object1.getInt("carLoad");
                        int passengerCount = object1.getInt("passengerCount");
                        String carName = object1.getString("carName");
                        int passengerLoad = object1.getInt("passengerLoad");

                        Car car = new Car(carLoad, passengerCount, carName, passengerLoad);
                        cars.add(car);
                    }

                    long receivedTime = object0.getLong("receivedTime");
                    long ttl = object0.getLong("ttl");
                    int doorStatus = Integer.parseInt(object0.getString("doorStatus"));
                    String td = object0.getString("td");
                    int targetDistance = Integer.parseInt(object0.getString("targetDistance"));
                    int startDistance = Integer.parseInt(object0.getString("startDistance"));

                    Trip trip = new Trip(trainId, "", trainSpeed, currentStationCode, nextStationCode, destinationStationCode,
                            cars, receivedTime, ttl, doorStatus, td, targetDistance, startDistance);

                    trips.add(trip);

                    runOnUiThread(() -> {
                        trip.addToLayout(MainActivity.this, tableLayout);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}