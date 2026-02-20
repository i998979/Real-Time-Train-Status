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

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class StationSearchActivity extends AppCompatActivity {

    private HistoryManager historyManager;

    private HRConfig hrConfig;

    private List<Integer> filteredIds = new ArrayList<>();
    private List<String> filteredNames = new ArrayList<>();
    private List<String> filteredCodes = new ArrayList<>();

    private EditText etSearch;
    private MaterialButton btnClose;

    private View layoutHistoryHeader;

    private TextView tvDelete;
    private RecyclerView rv;
    private StationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_search);

        historyManager = HistoryManager.getInstance(this);

        hrConfig = HRConfig.getInstance(this);

        etSearch = findViewById(R.id.et_station_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    layoutHistoryHeader.setVisibility(View.VISIBLE);
                    showHistory();
                } else {
                    layoutHistoryHeader.setVisibility(View.GONE);
                    filterStations(query);
                }
            }

            public void afterTextChanged(Editable s) {
            }
        });

        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());

        layoutHistoryHeader = findViewById(R.id.layout_history_header);

        tvDelete = findViewById(R.id.tv_delete);
        tvDelete.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryDeleteActivity.class);
            intent.putExtra(HistoryDeleteActivity.EXTRA_HISTORY_TYPE, HistoryDeleteActivity.TYPE_STATION);
            startActivity(intent);
        });

        rv = findViewById(R.id.rv_station_results);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StationAdapter((stationId, stationName, stationCode) -> {
            historyManager.saveStationSearch(stationId, stationName);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_station_id", stationId);
            resultIntent.putExtra("selected_station_name", stationName);
            resultIntent.putExtra("selected_station_code", stationCode);
            setResult(Activity.RESULT_OK, resultIntent);

            finish();
        });
        rv.setAdapter(adapter);

        showHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (etSearch.getText().toString().trim().isEmpty()) {
            showHistory();
        }
    }


    private void showHistory() {
        historyManager.loadStationHistory(history -> {
            List<Integer> historyIds = new ArrayList<>();
            List<String> historyNames = new ArrayList<>();
            List<String> historyCodes = new ArrayList<>();

            for (StationHistory h : history) {
                historyIds.add(h.stationId);
                historyNames.add(h.stationName);

                historyCodes.add(hrConfig.getStationAlias(h.stationId));
            }

            adapter.updateData(historyIds, historyNames, historyCodes);
        });
    }

    private void filterStations(String query) {
        filteredIds.clear();
        filteredNames.clear();
        filteredCodes.clear();

        String lowerQuery = query.toLowerCase().trim();

        if (lowerQuery.isEmpty()) {
            showHistory();
            return;
        }

        for (HRConfig.Station station : hrConfig.getIdMap().values()) {
            String name = station.name.toLowerCase();
            String enName = station.nameEN.toLowerCase();
            String code = station.alias.toLowerCase();

            if (name.contains(lowerQuery) || enName.contains(lowerQuery) || code.contains(lowerQuery)) {
                filteredIds.add(station.id);
                filteredNames.add(station.name);
                filteredCodes.add(station.alias);
            }
        }

        adapter.updateData(filteredIds, filteredNames, filteredCodes);
    }
}