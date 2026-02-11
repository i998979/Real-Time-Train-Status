package to.epac.factorycraft.realtimetrainstatus;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: // Route Search
                return new RouteSearchFragment();
            case 1: // Location Map
                return WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/layouts/adm.pdf");
            case 2: // Street Map
                return WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/maps/hok.pdf");
            case 3: // Realtime Train Location
                return new LineSelectorFragment();
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}