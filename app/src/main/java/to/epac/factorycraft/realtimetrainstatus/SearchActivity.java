package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    public static final String SEARCH_TYPE = "SEARCH_TYPE";
    public static final int TYPE_STATION = 0;
    public static final int TYPE_LINE = 1;

    private int currentMode = TYPE_STATION;

    private HistoryManager historyManager;
    private HRConfig hrConfig;

    private final List<Integer> filteredIds = new ArrayList<>();
    private final List<String> filteredNames = new ArrayList<>();
    private final List<String> filteredCodes = new ArrayList<>();

    private EditText etSearch;
    private View layoutHistoryHeader;
    private RecyclerView rv;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        currentMode = getIntent().getIntExtra(SEARCH_TYPE, TYPE_STATION);

        historyManager = HistoryManager.getInstance(this);
        hrConfig = HRConfig.getInstance(this);

        etSearch = findViewById(R.id.et_station_search);
        etSearch.setHint(currentMode == TYPE_LINE ? "搜尋路綫..." : "搜尋車站...");

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    layoutHistoryHeader.setVisibility(View.VISIBLE);
                    showHistory();
                } else {
                    layoutHistoryHeader.setVisibility(View.GONE);
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

        TextView tvDelete = findViewById(R.id.tv_delete);
        tvDelete.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryDeleteActivity.class);
            intent.putExtra(HistoryDeleteActivity.EXTRA_HISTORY_TYPE,
                    currentMode == TYPE_LINE ? HistoryDeleteActivity.TYPE_LINE : HistoryDeleteActivity.TYPE_STATION);
            startActivity(intent);
        });

        rv = findViewById(R.id.rv_station_results);
        rv.setLayoutManager(new LinearLayoutManager(this));

        if (currentMode == TYPE_LINE) {
            adapter = new SearchLineAdapter((id, name, code) -> {
                historyManager.saveLineSearch(id, name);
                returnResult(id, name, code);
            });
        } else {
            adapter = new SearchStationAdapter((id, name, code) -> {
                historyManager.saveStationSearch(id, name);
                returnResult(id, name, code);
            });
        }
        rv.setAdapter(adapter);

        showHistory();
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
            ((SearchLineAdapter) adapter).updateData(filteredIds, filteredNames, filteredCodes);
        } else {
            for (HRConfig.Station station : hrConfig.getIdMap().values()) {
                if (station.name.toLowerCase().contains(lowerQuery) ||
                        (station.nameEN != null && station.nameEN.toLowerCase().contains(lowerQuery)) ||
                        station.alias.toLowerCase().contains(lowerQuery)) {
                    filteredIds.add(station.id);
                    filteredNames.add(station.name);
                    filteredCodes.add(station.alias);
                }
            }
            ((SearchStationAdapter) adapter).updateData(filteredIds, filteredNames, filteredCodes);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (etSearch.getText().toString().trim().isEmpty()) showHistory();
    }
}