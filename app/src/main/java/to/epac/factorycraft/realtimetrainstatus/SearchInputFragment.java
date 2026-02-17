package to.epac.factorycraft.realtimetrainstatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class SearchInputFragment extends Fragment {

    private View layoutOrigin;
    private TextView tvOrigin;

    private MaterialButton btnSwap;

    private View layoutDest;
    private TextView tvDest;

    private Button btnGo;

    private String selectedOriginID = null;
    private String selectedDestID = null;
    private boolean isSelectingOrigin = true;

    private final ActivityResultLauncher<Intent> searchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    int id = result.getData().getIntExtra("selected_station_id", 1);
                    String name = result.getData().getStringExtra("selected_station_name");

                    if (isSelectingOrigin) {
                        selectedOriginID = String.valueOf(id);
                        tvOrigin.setText(name);
                    } else {
                        selectedDestID = String.valueOf(id);
                        tvDest.setText(name);
                    }
                    updateButtonStates();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_input, container, false);

        layoutOrigin = view.findViewById(R.id.layout_origin);
        tvOrigin = view.findViewById(R.id.tv_origin_name);
        layoutDest = view.findViewById(R.id.layout_dest);
        tvDest = view.findViewById(R.id.tv_dest_name);

        btnSwap = view.findViewById(R.id.btn_swap);
        btnSwap.setOnClickListener(v -> {
            String tempID = selectedOriginID;
            selectedOriginID = selectedDestID;
            selectedDestID = tempID;

            updateStationDisplay(tvOrigin, selectedOriginID, "出發地");
            updateStationDisplay(tvDest, selectedDestID, "目的地");
            updateButtonStates();
        });

        View.OnClickListener searchClick = v -> {
            isSelectingOrigin = (v.getId() == R.id.layout_origin);
            searchLauncher.launch(new Intent(requireContext(), StationSearchActivity.class));
        };
        layoutOrigin.setOnClickListener(searchClick);
        layoutDest.setOnClickListener(searchClick);

        btnGo = view.findViewById(R.id.btn_go);
        btnGo.setOnClickListener(v -> {
            HistoryManager.getInstance(requireContext()).saveRouteSearch(selectedOriginID, selectedDestID, tvOrigin.getText().toString(), tvDest.getText().toString());

            RouteListFragment listFragment = new RouteListFragment();
            Bundle bundle = new Bundle();
            bundle.putString("o", selectedOriginID);
            bundle.putString("d", selectedDestID);
            listFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .replace(R.id.main_container, listFragment)
                    .addToBackStack("LIST_PAGE")
                    .commit();
        });

        updateButtonStates();

        return view;
    }

    private void updateStationDisplay(TextView tv, String id, String hint) {
        if (id == null) {
            tv.setText(hint);
            return;
        }
        HRConfig.Station s = HRConfig.getInstance(requireContext()).getStationById(Integer.parseInt(id));
        tv.setText(s != null ? s.name : hint);
    }

    private void updateButtonStates() {
        boolean canSwap = selectedOriginID != null || selectedDestID != null;
        btnSwap.setEnabled(canSwap);
        btnSwap.setAlpha(canSwap ? 1.0f : 0.5f);

        boolean canGo = selectedOriginID != null && selectedDestID != null;
        btnGo.setEnabled(canGo);
        btnGo.setAlpha(canGo ? 1.0f : 0.5f);
    }
}