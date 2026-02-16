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

public class OperationInfoFragment extends Fragment {
    private static final List<String> subTitles = Arrays.asList("最近查看路綫", "運行情報", "列車走行位置");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_operation_info, container, false);

        ViewPager2 pagerContent = view.findViewById(R.id.pager_content);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        pagerContent.setAdapter(new OperationInfoAdapter(this));

        new TabLayoutMediator(tabLayout, pagerContent, (tab, position) -> {
            tab.setText(subTitles.get(position));
        }).attach();

        pagerContent.post(() -> {
            pagerContent.setCurrentItem(0, false);
        });

        return view;
    }


    private static class OperationInfoAdapter extends FragmentStateAdapter {
        public OperationInfoAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: // Frequently Viewed Route
                    return new Fragment();
                case 1: // Traffic News
                    return new TrafficNewsFragment();
                case 2: // Realtime Train Location
                    return new LineSelectorFragment();
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