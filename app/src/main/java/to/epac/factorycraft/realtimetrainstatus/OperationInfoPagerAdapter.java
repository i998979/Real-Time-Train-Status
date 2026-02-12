package to.epac.factorycraft.realtimetrainstatus;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OperationInfoPagerAdapter extends FragmentStateAdapter {
    public OperationInfoPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: // Recently Viewed Route
                return new RecentViewedFragment();
            case 1: // Traffic News
                return new Fragment();
            case 2: // Realtime Train Location
                return new LineSelectorFragment();
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}