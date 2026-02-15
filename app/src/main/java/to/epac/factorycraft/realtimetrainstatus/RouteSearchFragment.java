package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class RouteSearchFragment extends Fragment {
    private static final List<String> subTitles = Arrays.asList("檢索履歷", "檢索", "常用檢索");

    TabLayout tabLayout;
    ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_search, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        viewPager.setAdapter(new RoutePagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View tabView = LayoutInflater.from(getContext()).inflate(R.layout.tab_search, null);

            ((TextView) tabView.findViewById(R.id.tab_text)).setText(subTitles.get(position));
            tabView.findViewById(R.id.tab_icon).setVisibility(position == 1 ? View.VISIBLE : View.GONE);

            updateTabColor(tabView, position == 1);

            tab.setCustomView(tabView);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getCustomView() != null) {
                    updateTabColor(tab.getCustomView(), true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getCustomView() != null) {
                    updateTabColor(tab.getCustomView(), false);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewPager.setCurrentItem(1, false);

        LinearLayout layoutDots = view.findViewById(R.id.layout_dots);
        View[] dots = new View[3];

        int defaultPosition = 1;

        for (int i = 0; i < 3; i++) {
            dots[i] = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(24, 24);
            params.setMargins(12, 0, 12, 0);
            dots[i].setLayoutParams(params);
            dots[i].setBackgroundResource(R.drawable.tab_indicator_dot);

            dots[i].setAlpha(i == defaultPosition ? 1.0f : 0.3f);

            final int position = i;
            dots[i].setOnClickListener(v -> {
                viewPager.setCurrentItem(position, true);
            });

            layoutDots.addView(dots[i]);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < 3; i++) {
                    dots[i].setAlpha(i == position ? 1.0f : 0.3f);
                }
            }
        });

        return view;
    }

    private void updateTabColor(View view, boolean isSelected) {
        ImageView icon = view.findViewById(R.id.tab_icon);
        TextView text = view.findViewById(R.id.tab_text);
        int color = isSelected ? Color.parseColor("#FFFFFF") : Color.parseColor("#96ADBB");
        icon.setColorFilter(color);
        text.setTextColor(color);
    }


    private static class RoutePagerAdapter extends FragmentStateAdapter {
        private RoutePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: // Recent Viewed
                    return new RecentViewedFragment();
                case 1: // Route Search
                    return new SearchInputFragment();
                case 2: // Saved Route
                    return new Fragment();
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