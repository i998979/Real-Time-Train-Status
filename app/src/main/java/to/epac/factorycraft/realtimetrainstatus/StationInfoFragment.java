package to.epac.factorycraft.realtimetrainstatus;

import android.content.Intent;
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

    private View searchBar;
    private TextView tvSearchStation;
    private MaterialButton btnRefresh;
    private TabLayout tabLayout;
    private ViewPager2 pagerContent;

    private static final SparseArray<Fragment> activeFragments = new SparseArray<>();

    private final ActivityResultLauncher<Intent> searchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    String name = result.getData().getStringExtra("selected_station_name");
                    String code = result.getData().getStringExtra("selected_station_code");
                    int id = result.getData().getIntExtra("selected_station_id", 1);

                    updateStationInfo(name, code, id);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_station_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchBar = view.findViewById(R.id.search_bar);
        tvSearchStation = view.findViewById(R.id.tv_search_station);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            int currentTab = pagerContent.getCurrentItem();

            if (pagerContent.getAdapter() instanceof StationInfoPagerAdapter) {
                StationInfoPagerAdapter adapter = (StationInfoPagerAdapter) pagerContent.getAdapter();

                Fragment currentFragment = adapter.getFragmentAt(currentTab);

                if (currentFragment instanceof WebViewFragment) {
                    ((WebViewFragment) currentFragment).refresh();
                }
            }
        });
        tabLayout = view.findViewById(R.id.tab_layout);
        pagerContent = view.findViewById(R.id.pager_content);
        pagerContent.setUserInputEnabled(false);

        View.OnClickListener searchClickListener = v -> {
            searchLauncher.launch(new Intent(requireContext(), StationSearchActivity.class));
        };
        searchBar.setOnClickListener(searchClickListener);
        tvSearchStation.setOnClickListener(searchClickListener);

        Bundle args = getArguments();
        String initName = args != null ? args.getString("station_name", "中環") : "中環";
        String initCode = args != null ? args.getString("station_code", "CEN") : "CEN";
        int initId = args != null ? args.getInt("station_id", 1) : 1;

        updateStationInfo(initName, initCode, initId);
    }

    private void updateStationInfo(String name, String code, int id) {
        tvSearchStation.setText(name);

        pagerContent.setAdapter(new StationInfoPagerAdapter(this, code, id));
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
                    fragment = new LineSelectorFragment();
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

        public Fragment getFragmentAt(int position) {
            return activeFragments.get(position);
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