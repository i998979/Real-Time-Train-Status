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
            case 1: // 位置圖
                return WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/layouts/adm.pdf");
            case 2: // 街道圖
                return WebViewFragment.newInstance("https://www.mtr.com.hk/archive/ch/services/maps/hok.pdf");
            case 3: // 列車走行位置
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