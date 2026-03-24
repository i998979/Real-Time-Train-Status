package to.epac.factorycraft.transitapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    private LinearLayout searchLayout;
    private EditText etSearch;
    private View layoutHistoryHeader;
    private View layoutLocation;
    private TextView tvDeleteLoc;
    private RecyclerView rvSearch;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);

        currentMode = getIntent().getIntExtra("search_type", TYPE_STATION);
        search_location = getIntent().getBooleanExtra("search_location", false);
        title = getIntent().getStringExtra("search_title");

        historyManager = HistoryManager.getInstance(this);
        hrConfig = HRConfig.getInstance(this);

        searchLayout = findViewById(R.id.layout_search);

        etSearch = findViewById(R.id.et_station_search);
        etSearch.setHint(title);

        layoutLocation = findViewById(R.id.layout_location);
        tvDeleteLoc = findViewById(R.id.tv_delete_loc);

        if (!search_location)
            layoutLocation.setVisibility(View.GONE);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    layoutHistoryHeader.setVisibility(View.VISIBLE);
                    if (search_location)
                        layoutLocation.setVisibility(View.VISIBLE);
                    showHistory();
                } else {
                    layoutHistoryHeader.setVisibility(View.GONE);
                    layoutLocation.setVisibility(View.GONE);
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btn_close).setOnClickListener(v -> {
            finish();
        });
        layoutHistoryHeader = findViewById(R.id.layout_history_header);

        findViewById(R.id.tv_delete).setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryDeleteActivity.class);
            intent.putExtra("history_type", currentMode == TYPE_LINE ? HistoryDeleteActivity.TYPE_LINE : HistoryDeleteActivity.TYPE_STATION);
            startActivity(intent);
        });

        rvSearch = findViewById(R.id.rv_station_results);
        rvSearch.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        rvSearch.setLayoutManager(new LinearLayoutManager(this));

        if (currentMode == TYPE_LINE) {
            adapter = new SearchLineAdapter((id, name, code) -> {
                historyManager.saveLineSearch(id, name);
                returnResult(id, name, code);
            });
        } else {
            adapter = new SearchStationAdapter(new SearchStationAdapter.OnStationClickListener() {
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
                        if (favorites.size() >= 5) {
                            View view = getLayoutInflater().inflate(R.layout.dialog_fav_limit, null);
                            AlertDialog dialog = new MaterialAlertDialogBuilder(SearchActivity.this)
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


                    if (adapter instanceof SearchStationAdapter) {
                        ((SearchStationAdapter) adapter).updateFavorites(favorites);
                    }
                    loadFavoriteChips();

                    if (!isRemoving && !prefs.getBoolean(MainActivity.KEY_FIRST_FAVORITE_USED, false)) {
                        View view = getLayoutInflater().inflate(R.layout.dialog_fav_first_time, null);
                        AlertDialog dialog = new MaterialAlertDialogBuilder(SearchActivity.this)
                                .setView(view)
                                .create();

                        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
                            dialog.dismiss();
                        });

                        prefs.edit().putBoolean(MainActivity.KEY_FIRST_FAVORITE_USED, true).apply();
                        dialog.show();
                    }
                }
            });

            ((SearchStationAdapter) adapter).updateFavorites(getFavorites());
        }
        rvSearch.setAdapter(adapter);
        showHistory();
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

    private void loadFavoriteChips() {
        if (!search_location) return;

        List<String> favs = getFavorites();
        ChipGroup chipStations = findViewById(R.id.chip_stations);
        View chipLoc = findViewById(R.id.chip_loc);

        chipStations.removeAllViews();
        if (chipLoc != null) chipStations.addView(chipLoc);

        tvDeleteLoc.setVisibility(favs.isEmpty() ? View.GONE : View.VISIBLE);

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
                chip.setChipCornerRadius(Utils.dpToPx(this, 20));

                chip.setOnClickListener(v -> returnResult(station.id, station.name, station.alias));
                chipStations.addView(chip);
            } catch (Exception ignored) {
            }
        }
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
                ((SearchLineAdapter) adapter).updateData(ids, names, codes);
            });
        } else {
            historyManager.loadStationHistory(history -> {
                for (StationHistory h : history) {
                    ids.add(h.stationId);
                    names.add(h.stationName);
                    codes.add(hrConfig.getStationAlias(h.stationId));
                }
                ((SearchStationAdapter) adapter).updateData(ids, names, codes);
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

        if (adapter instanceof SearchStationAdapter)
            ((SearchStationAdapter) adapter).updateData(filteredIds, filteredNames, filteredCodes);
        else if (adapter instanceof SearchLineAdapter)
            ((SearchLineAdapter) adapter).updateData(filteredIds, filteredNames, filteredCodes);
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