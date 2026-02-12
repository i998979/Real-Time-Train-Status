package to.epac.factorycraft.realtimetrainstatus;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StationInfoPagerAdapter extends FragmentStateAdapter {
    public StationInfoPagerAdapter(@NonNull Fragment fragment) {
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
        return 4;
    }
}