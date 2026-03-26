package to.epac.factorycraft.transitapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private LocationManager lm;

    public static final int TYPE_STATION = 0;
    public static final int TYPE_LINE = 1;

    private int currentMode = TYPE_STATION;
    private boolean search_location = false;
    private String title = "請輸入車站";

    private HistoryManager historyManager;
    private HRConfig hrConfig;

    private final List<Integer> filteredIds = new ArrayList<>();
    private final List<String> filteredNames = new ArrayList<>();
    private final List<String> filteredCodes = new ArrayList<>();

    private final List<Integer> nearbyIds = new ArrayList<>();
    private final List<String> nearbyNames = new ArrayList<>();
    private final List<String> nearbyCodes = new ArrayList<>();

    private LinearLayout searchLayout;
    private EditText etSearch;
    private MaterialButton btnClose;

    private LinearLayout locationLayout;
    private TextView tvDeleteLoc;

    private ChipGroup chipStations;
    private Chip chipLoc;

    private TextView tvNoNearby;
    private ProgressBar pbLoading;

    private LinearLayout nearbyLayout;
    private RecyclerView rvNearby;
    private RecyclerView.Adapter historyAdapter;
    private TextView tvMore;

    private LinearLayout historyLayout;
    private TextView tvDelete;
    private RecyclerView rvHistory;
    private SearchStationAdapter stationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        currentMode = getIntent().getIntExtra("search_type", TYPE_STATION);
        search_location = getIntent().getBooleanExtra("search_location", false);
        title = getIntent().getStringExtra("search_title");

        historyManager = HistoryManager.getInstance(this);
        hrConfig = HRConfig.getInstance(this);


        searchLayout = findViewById(R.id.layout_search);
        etSearch = findViewById(R.id.et_station_search);
        etSearch.setHint(title);
        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });

        locationLayout = findViewById(R.id.layout_location);
        if (!search_location)
            locationLayout.setVisibility(View.GONE);
        tvDeleteLoc = findViewById(R.id.tv_delete_loc);
        tvDeleteLoc.setOnClickListener(v -> {
            DeleteSavedStationsSheet sheet = new DeleteSavedStationsSheet();
            sheet.setOnDismissListener(() -> {
                List<String> updatedFavs = getFavorites();

                if (historyAdapter instanceof SearchStationAdapter) {
                    ((SearchStationAdapter) historyAdapter).updateFavorites(updatedFavs);
                }

                loadFavoriteChips();
            });
            sheet.show(getSupportFragmentManager(), "DeleteSheet");
        });

        chipStations = findViewById(R.id.chip_stations);
        chipLoc = findViewById(R.id.chip_loc);

        tvNoNearby = findViewById(R.id.tv_no_nearby);
        pbLoading = findViewById(R.id.pb_loading);

        nearbyLayout = findViewById(R.id.layout_nearby);
        rvNearby = findViewById(R.id.rv_nearby);
        stationAdapter = new SearchStationAdapter(new SearchStationAdapter.OnStationClickListener() {
            @Override
            public void onStationClick(int id, String name, String code) {
                historyManager.saveStationSearch(id, name);
                returnResult(id, name, code);
            }

            public void onFavoriteClick(int id, String name, String code) {
                String idStr = String.valueOf(id);
                String saved = prefs.getString(MainActivity.KEY_FAV_STATIONS, "");
                List<String> favorites = saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(",")));

                boolean isRemoving = favorites.contains(idStr);

                if (!isRemoving) {
                    if (favorites.size() >= 5) {
                        View view = getLayoutInflater().inflate(R.layout.dialog_fav_limit, null);
                        AlertDialog dialog = new MaterialAlertDialogBuilder(SearchActivity.this, R.style.GreenAlertDialogTheme)
                                .setView(view)
                                .create();
                        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> dialog.dismiss());
                        dialog.show();
                        return;
                    }

                    favorites.add(idStr);
                    showSnackBar(searchLayout, Color.parseColor("#58A473"), "已新增至常用車站");
                } else {
                    favorites.remove(idStr);
                    showSnackBar(searchLayout, Color.parseColor("#58A473"), "已從常用車站刪除");
                }
                prefs.edit().putString(MainActivity.KEY_FAV_STATIONS, TextUtils.join(",", favorites)).apply();


                updateAllAdaptersFavorites();

                if (!isRemoving && !prefs.getBoolean(MainActivity.KEY_FIRST_FAVORITE_USED, false)) {
                    View view = getLayoutInflater().inflate(R.layout.dialog_fav_first_time, null);
                    AlertDialog dialog = new MaterialAlertDialogBuilder(SearchActivity.this, R.style.GreenAlertDialogTheme)
                            .setView(view)
                            .create();

                    view.findViewById(R.id.btn_confirm).setOnClickListener(v -> dialog.dismiss());

                    prefs.edit().putBoolean(MainActivity.KEY_FIRST_FAVORITE_USED, true).apply();
                    dialog.show();
                }
            }
        });
        rvNearby.setLayoutManager(new LinearLayoutManager(this));
        rvNearby.setAdapter(stationAdapter);
        tvMore = findViewById(R.id.tv_more);
        tvMore.setOnClickListener(v -> {
            updateNearbyAdapter(20);
            tvMore.setVisibility(View.GONE);
        });

        historyLayout = findViewById(R.id.layout_history);
        tvDelete = findViewById(R.id.tv_delete);
        tvDelete.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryDeleteActivity.class);
            intent.putExtra("history_type", currentMode == TYPE_LINE ? HistoryDeleteActivity.TYPE_LINE : HistoryDeleteActivity.TYPE_STATION);
            startActivity(intent);
        });
        rvHistory = findViewById(R.id.rv_history);
        rvHistory.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        rvHistory.setLayoutManager(new LinearLayoutManager(this));


        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    historyLayout.setVisibility(View.VISIBLE);
                    if (search_location) locationLayout.setVisibility(View.VISIBLE);

                    showHistory();
                } else {
                    historyLayout.setVisibility(View.GONE);
                    locationLayout.setVisibility(View.GONE);

                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        if (currentMode == TYPE_LINE) {
            historyAdapter = new SearchLineAdapter((id, name, code) -> {
                historyManager.saveLineSearch(id, name);
                returnResult(id, name, code);
            });
        } else {
            historyAdapter = new SearchStationAdapter(new SearchStationAdapter.OnStationClickListener() {
                @Override
                public void onStationClick(int id, String name, String code) {
                    historyManager.saveStationSearch(id, name);
                    returnResult(id, name, code);
                }

                @Override
                public void onFavoriteClick(int id, String name, String code) {
                    String idStr = String.valueOf(id);
                    String saved = prefs.getString(MainActivity.KEY_FAV_STATIONS, "");
                    List<String> favorites = saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(",")));

                    boolean isRemoving = favorites.contains(idStr);

                    if (!isRemoving) {
                        // Maximum 5 favorites
                        if (favorites.size() >= 5) {
                            View view = getLayoutInflater().inflate(R.layout.dialog_fav_limit, null);
                            AlertDialog dialog = new MaterialAlertDialogBuilder(SearchActivity.this, R.style.GreenAlertDialogTheme)
                                    .setView(view)
                                    .create();

                            view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
                                dialog.dismiss();
                            });
                            dialog.show();
                            return;
                        }

                        favorites.add(idStr);
                        showSnackBar(searchLayout, Color.parseColor("#58A473"), "已新增至常用車站");
                    } else {
                        favorites.remove(idStr);
                        showSnackBar(searchLayout, Color.parseColor("#58A473"), "已從常用車站刪除");
                    }

                    prefs.edit().putString(MainActivity.KEY_FAV_STATIONS, TextUtils.join(",", favorites)).apply();

                    updateAllAdaptersFavorites();

                    // If this is first time using favorite
                    if (!isRemoving && !prefs.getBoolean(MainActivity.KEY_FIRST_FAVORITE_USED, false)) {
                        View view = getLayoutInflater().inflate(R.layout.dialog_fav_first_time, null);
                        AlertDialog dialog = new MaterialAlertDialogBuilder(SearchActivity.this, R.style.GreenAlertDialogTheme)
                                .setView(view)
                                .create();

                        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> dialog.dismiss());

                        prefs.edit().putBoolean(MainActivity.KEY_FIRST_FAVORITE_USED, true).apply();
                        dialog.show();
                    }
                }
            });

            updateAllAdaptersFavorites();
        }

        rvHistory.setAdapter(historyAdapter);
        showHistory();


        if (search_location && currentMode == TYPE_STATION) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGpsEnabled && !isNetworkEnabled) {
                    tvNoNearby.setVisibility(View.VISIBLE);
                    return;
                }

                ColorStateList stateList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_green));
                chipLoc.setChipIconTint(stateList);
                chipLoc.setChipStrokeColor(stateList);
                chipLoc.setTextColor(stateList);

                pbLoading.setVisibility(View.VISIBLE);
                tvNoNearby.setVisibility(View.GONE);

                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                Task<Location> currentLocationTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null);
                currentLocationTask.addOnSuccessListener(this, location -> {
                    pbLoading.setVisibility(View.GONE);

                    if (location == null) {
                        tvNoNearby.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<Pair<HRConfig.Station, Float>> distances = new ArrayList<>();
                    for (HRConfig.Station station : hrConfig.getIdMap().values()) {
                        if (station.coordinate != null && station.coordinate.contains(",")) {
                            try {
                                String[] parts = station.coordinate.split(",");
                                double lat = Double.parseDouble(parts[0].trim());
                                double lng = Double.parseDouble(parts[1].trim());

                                float[] results = new float[1];
                                Location.distanceBetween(location.getLatitude(), location.getLongitude(), lat, lng, results);
                                distances.add(new Pair<>(station, results[0]));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    distances.sort((p1, p2) -> Float.compare(p1.second, p2.second));

                    nearbyIds.clear();
                    nearbyNames.clear();
                    nearbyCodes.clear();
                    for (int i = 0; i < Math.min(20, distances.size()); i++) {
                        nearbyIds.add(distances.get(i).first.id);
                        nearbyNames.add(distances.get(i).first.name);
                        nearbyCodes.add(distances.get(i).first.alias);
                    }

                    prefs.edit().putString(MainActivity.KEY_NEAREST_STATION, nearbyCodes.get(0)).apply();

                    updateNearbyAdapter(3);
                    nearbyLayout.setVisibility(View.VISIBLE);
                    tvMore.setVisibility(nearbyIds.size() <= 3 ? View.GONE : View.VISIBLE);
                }).addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    tvNoNearby.setVisibility(View.VISIBLE);
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (etSearch.getText().toString().trim().isEmpty()) {
            showHistory();
            if (search_location) loadFavoriteChips();
        }
    }

    private List<String> getFavorites() {
        String saved = prefs.getString(MainActivity.KEY_FAV_STATIONS, "");
        if (saved.isEmpty())
            return new ArrayList<>();

        return new ArrayList<>(Arrays.asList(saved.split(",")));
    }

    private void updateNearbyAdapter(int count) {
        if (nearbyIds.isEmpty()) return;

        int limit = Math.min(count, nearbyIds.size());
        List<Integer> subIds = nearbyIds.subList(0, limit);
        List<String> subNames = nearbyNames.subList(0, limit);
        List<String> subCodes = nearbyCodes.subList(0, limit);

        stationAdapter.updateData(subIds, subNames, subCodes);
        stationAdapter.updateFavorites(getFavorites());
    }

    private void loadFavoriteChips() {
        if (!search_location) return;

        List<String> favs = getFavorites();
        View chipLoc = findViewById(R.id.chip_loc);

        chipLoc.setOnClickListener(v -> {
            boolean gpsEnabled = false;
            try {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (gpsEnabled) {
                returnResult(-1, "現在地", "CURRENT");
            } else {
                showGpsDialog();
            }
        });

        chipStations.removeAllViews();
        chipStations.addView(chipLoc);

        tvDeleteLoc.setClickable(!favs.isEmpty());

        for (String id : favs) {
            try {
                HRConfig.Station station = hrConfig.getStationById(Integer.parseInt(id));

                Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
                chip.setText(station.name);
                chip.setTextSize(14);
                chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
                chip.setTextColor(Color.WHITE);
                chip.setChipBackgroundColorResource(R.color.button_green);
                chip.setChipStrokeWidth(0);
                chip.setChipCornerRadius(Utils.dpToPx(20));

                chip.setOnClickListener(v -> returnResult(station.id, station.name, station.alias));
                chipStations.addView(chip);
            } catch (Exception ignored) {
            }
        }
    }

    private void updateAllAdaptersFavorites() {
        List<String> favs = getFavorites();

        if (historyAdapter instanceof SearchStationAdapter)
            ((SearchStationAdapter) historyAdapter).updateFavorites(favs);

        stationAdapter.updateFavorites(favs);

        loadFavoriteChips();
    }

    private void returnResult(int id, String name, String code) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_id", id);
        resultIntent.putExtra("selected_name", name);
        resultIntent.putExtra("selected_code", code);
        setResult(Activity.RESULT_OK, resultIntent);

        finish();
    }

    private void showHistory() {
        List<Integer> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> codes = new ArrayList<>();

        if (currentMode == TYPE_LINE) {
            historyManager.loadLineHistory(history -> {
                for (LineHistory h : history) {
                    ids.add(h.lineId);
                    names.add(h.lineName);
                    HRConfig.Line line = hrConfig.getLineMap().get(h.lineId);
                    codes.add(line != null ? line.alias : "");
                }
                ((SearchLineAdapter) historyAdapter).updateData(ids, names, codes);
            });
        } else {
            historyManager.loadStationHistory(history -> {
                for (StationHistory h : history) {
                    ids.add(h.stationId);
                    names.add(h.stationName);
                    codes.add(hrConfig.getStationAlias(h.stationId));
                }
                ((SearchStationAdapter) historyAdapter).updateData(ids, names, codes);
            });
        }
    }

    private void performSearch(String query) {
        filteredIds.clear();
        filteredNames.clear();
        filteredCodes.clear();

        String lowerQuery = query.toLowerCase().trim();

        if (currentMode == TYPE_LINE) {
            for (HRConfig.Line line : hrConfig.getLineMap().values()) {
                if ("HSR".equalsIgnoreCase(line.alias)) continue;

                if (line.name.toLowerCase().contains(lowerQuery)
                        || (line.nameEN != null && line.nameEN.toLowerCase().contains(lowerQuery))
                        || line.alias.toLowerCase().contains(lowerQuery)) {
                    filteredIds.add(line.id);
                    filteredNames.add(line.name);
                    filteredCodes.add(line.alias);
                }
            }
        } else {
            Set<String> seenAliases = new HashSet<>();

            for (HRConfig.Station station : hrConfig.getIdMap().values()) {
                if (seenAliases.contains(station.alias)) continue;

                if (station.name.toLowerCase().contains(lowerQuery) ||
                        (station.nameEN != null && station.nameEN.toLowerCase().contains(lowerQuery)) ||
                        station.alias.toLowerCase().contains(lowerQuery)) {

                    filteredIds.add(station.id);
                    filteredNames.add(station.name);
                    filteredCodes.add(station.alias);

                    seenAliases.add(station.alias);
                }
            }
        }

        if (historyAdapter instanceof SearchStationAdapter)
            ((SearchStationAdapter) historyAdapter).updateData(filteredIds, filteredNames, filteredCodes);
        else if (historyAdapter instanceof SearchLineAdapter)
            ((SearchLineAdapter) historyAdapter).updateData(filteredIds, filteredNames, filteredCodes);
    }

    private void showGpsDialog() {
        View gpsView = getLayoutInflater().inflate(R.layout.dialog_gps_prompt, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.GreenAlertDialogTheme)
                .setView(gpsView)
                .create();

        dialog.show();

        MaterialButton btnPositive = gpsView.findViewById(R.id.btn_positive);
        MaterialButton btnNegative = gpsView.findViewById(R.id.btn_negative);

        btnNegative.setOnClickListener(v -> dialog.dismiss());

        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            android.net.Uri uri = android.net.Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
    }

    private void showSnackBar(View anchor, int color, String message) {
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        snackbarView.setBackgroundTintList(ColorStateList.valueOf(color));
        // snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(16);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }
}