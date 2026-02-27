package to.epac.factorycraft.realtimetrainstatus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class StationActivity extends AppCompatActivity {
    private static final List<String> subTitles = Arrays.asList("位置圖", "街道圖", "列車走行位置", "車站商店");

    private HRConfig hrConf;
    private SharedPreferences prefs;

    private TextView tvBannerName;
    private MaterialButton btnClose;
    private TabLayout tabLayout;
    private ViewPager2 pagerContent;

    private static final SparseArray<Fragment> activeFragments = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        hrConf = HRConfig.getInstance(this);

        tvBannerName = findViewById(R.id.tv_banner_name);
        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });

        tabLayout = findViewById(R.id.tab_layout);
        pagerContent = findViewById(R.id.pager_content);
        pagerContent.setUserInputEnabled(false);

        pagerContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                prefs.edit()
                        .putInt(MainActivity.KEY_STATION_LAST_TAB, position)
                        .apply();
            }
        });

        String code = null;
        if (getIntent() != null) {
            code = getIntent().getStringExtra("station_code");
        }

        updateStationInfoFromCode(code);

        int lastTab = prefs.getInt(MainActivity.KEY_STATION_LAST_TAB, 0);
        pagerContent.setCurrentItem(lastTab, false);
    }

    private void updateStationInfoFromCode(String code) {
        HRConfig.Station sta = hrConf.getStationByAlias(code);

        tvBannerName.setText(sta.name);

        pagerContent.setAdapter(new StationPagerAdapter(this, sta.alias, sta.id));
        new TabLayoutMediator(tabLayout, pagerContent, (tab, pos) -> {
            tab.setText(subTitles.get(pos));
        }).attach();
    }

    public static class StationPagerAdapter extends FragmentStateAdapter {
        private final String code;
        private final int id;

        private StationPagerAdapter(androidx.fragment.app.FragmentActivity activity, String code, int id) {
            super(activity);
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
                    fragment = LineSelectorFragment.newInstance(code.toUpperCase(), false);
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