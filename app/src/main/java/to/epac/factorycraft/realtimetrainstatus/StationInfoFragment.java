package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class StationInfoFragment extends Fragment {
    private static final List<String> subTitles = Arrays.asList("位置圖", "街道圖", "列車走行位置", "車站商店");

    private SharedPreferences prefs;

    private View searchBar;
    private TextView tvSearchStation;
    private MaterialButton btnRefresh;
    private TabLayout tabLayout;
    private ViewPager2 pagerContent;

    private static final SparseArray<Fragment> activeFragments = new SparseArray<>();

    private final ActivityResultLauncher<Intent> searchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    updateStationInfoFromCode(result.getData().getStringExtra("selected_station_code"));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

        return inflater.inflate(R.layout.fragment_station_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchBar = view.findViewById(R.id.search_bar);
        tvSearchStation = view.findViewById(R.id.tv_search_station);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            if (pagerContent.getAdapter() instanceof StationInfoPagerAdapter) {
                Fragment f = activeFragments.get(pagerContent.getCurrentItem());
                if (f instanceof WebViewFragment) {
                    ((WebViewFragment) f).refresh();
                }
            }
        });
        tabLayout = view.findViewById(R.id.tab_layout);
        pagerContent = view.findViewById(R.id.pager_content);
        pagerContent.setUserInputEnabled(false);

        pagerContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                prefs.edit()
                        .putInt(MainActivity.KEY_STATIONINFO_LAST_TAB, position)
                        .apply();
            }
        });

        View.OnClickListener searchClickListener = v -> {
            searchLauncher.launch(new Intent(requireContext(), StationSearchActivity.class));
        };
        searchBar.setOnClickListener(searchClickListener);
        tvSearchStation.setOnClickListener(searchClickListener);

        String code;
        if (getArguments() != null) {
            code = getArguments().getString("station_code", "CEN");
        } else {
            code = prefs.getString(MainActivity.KEY_LAST_STATION_CODE, "CEN");
        }

        updateStationInfoFromCode(code);

        int lastTab = prefs.getInt(MainActivity.KEY_STATIONINFO_LAST_TAB, 0);
        pagerContent.setCurrentItem(lastTab, false);
    }

    private void updateStationInfoFromCode(String code) {
        HRConfig.Station sta = HRConfig.getInstance(requireContext()).getStationByAlias(code);

        prefs.edit()
                .putString(MainActivity.KEY_LAST_STATION_CODE, code)
                .apply();
        tvSearchStation.setText(sta.name);

        pagerContent.setAdapter(new StationInfoPagerAdapter(this, sta.alias, sta.id));
        new TabLayoutMediator(tabLayout, pagerContent, (tab, pos) -> {
            tab.setText(subTitles.get(pos));
        }).attach();
    }

    public static class StationInfoPagerAdapter extends FragmentStateAdapter {
        private final String code;
        private final int id;

        private StationInfoPagerAdapter(Fragment fragment, String code, int id) {
            super(fragment);
            this.code = code.toLowerCase();
            this.id = id;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/layouts/" + code + ".pdf");
                    break;
                case 1:
                    fragment = WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/maps/" + code + ".pdf");
                    break;
                case 2:
                    fragment = LineSelectorFragment.newInstance(code.toUpperCase());
                    break;
                case 3:
                    fragment = WebViewFragment.newInstance("https://www.mtr.com.hk/ch/customer/shops/shop_search.php?query_type=search&start=" + id);
                    break;
                default:
                    fragment = new Fragment();
            }
            activeFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return subTitles.size();
        }

        @Override
        public long getItemId(int pos) {
            return (code + pos).hashCode();
        }

        @Override
        public boolean containsItem(long id) {
            return true;
        }
    }
}