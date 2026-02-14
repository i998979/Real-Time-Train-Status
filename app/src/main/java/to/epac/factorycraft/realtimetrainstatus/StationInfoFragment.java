package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class StationInfoFragment extends Fragment {
    private static final List<String> subTitles = Arrays.asList("位置圖", "街道圖", "列車走行位置", "車站商店");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_info, container, false);

        ViewPager2 pagerContent = view.findViewById(R.id.pager_content);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        pagerContent.setAdapter(new StationInfoPagerAdapter(this));
        pagerContent.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, pagerContent, (tab, position) -> {
            tab.setText(subTitles.get(position));
        }).attach();

        pagerContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + pagerContent.getCurrentItem());

                    if (fragment instanceof WebViewFragment) {
                        ((WebViewFragment) fragment).loadContent();
                    }
                }
            }
        });

        pagerContent.post(() -> {
            pagerContent.setCurrentItem(2, false);
        });

        return view;
    }


    private static class StationInfoPagerAdapter extends FragmentStateAdapter {
        private StationInfoPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: // Location Map
                    return WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/layouts/adm.pdf");
                case 1: // Street Map
                    return WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/maps/hok.pdf");
                case 2: // Realtime Train Location
                    return new LineSelectorFragment();
                case 3: // Station Stores
                    return WebViewFragment.newInstance("https://www.mtr.com.hk/ch/customer/shops/shop_search.php?query_type=search&start=39");
                default:
                    return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return subTitles.size();
        }
    }
}
