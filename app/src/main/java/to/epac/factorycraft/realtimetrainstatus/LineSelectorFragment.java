package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class LineSelectorFragment extends Fragment {

    public static final String ARG_STATION_CODE = "station_code";

    private ActivityResultLauncher<Intent> searchLauncher;

    public static LineSelectorFragment newInstance(String stationCode) {
        LineSelectorFragment fragment = new LineSelectorFragment();

        Bundle args = new Bundle();
        args.putString(ARG_STATION_CODE, stationCode);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_line_select, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_line_select);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        Switch openData = view.findViewById(R.id.open_data);

        MaterialButton btnSearch = view.findViewById(R.id.btn_search);
        searchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String code = result.getData().getStringExtra("selected_code");

                        if (code != null) {
                            Intent intent = new Intent(getContext(), TrainLocationActivity.class);
                            intent.putExtra("LINE_CODE", code.toLowerCase());
                            intent.putExtra("DATA_SOURCE", openData.isChecked() ? "OPENDATA" : "ROCTEC");
                            startActivity(intent);
                        }
                    }
                }
        );
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            intent.putExtra(SearchActivity.SEARCH_TYPE, SearchActivity.TYPE_LINE);
            searchLauncher.launch(intent);
        });

        HRConfig config = HRConfig.getInstance(getContext());
        String stationCode = getArguments() != null ? getArguments().getString(ARG_STATION_CODE) : null;

        List<HRConfig.Line> display;
        if (stationCode == null || stationCode.isEmpty()) {
            display = config.getAllLines();
        } else {
            display = config.getLinesByStationAlias(stationCode);
        }
        display.removeIf(line -> line.alias.equals("HSR"));

        LineSelectorAdapter adapter = new LineSelectorAdapter(display, line -> {
            Intent intent = new Intent(getContext(), TrainLocationActivity.class);
            intent.putExtra("LINE_CODE", line.alias.toLowerCase());
            intent.putExtra("DATA_SOURCE", openData.isChecked() ? "OPENDATA" : "ROCTEC");
            startActivity(intent);
        });

        rv.setAdapter(adapter);
        return view;
    }
}