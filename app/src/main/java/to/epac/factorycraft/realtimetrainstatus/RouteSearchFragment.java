package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.SharedPreferences;
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

    SharedPreferences prefs;

    TabLayout tabLayout;
    ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_search, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

        int lastTab = prefs.getInt(MainActivity.KEY_ROUTESEARCH_LAST_TAB, 1);

        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new RoutePagerAdapter(this));

        tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View tabView = LayoutInflater.from(getContext()).inflate(R.layout.tab_search, null);
            TextView tabText = tabView.findViewById(R.id.tab_text);
            View tabIcon = tabView.findViewById(R.id.tab_icon);

            tabText.setText(subTitles.get(position));
            tabIcon.setVisibility(position == 1 ? View.VISIBLE : View.GONE);

            updateTabColor(tabView, position == lastTab);

            tab.setCustomView(tabView);
        }).attach();

        viewPager.setCurrentItem(lastTab, false);

        LinearLayout layoutDots = view.findViewById(R.id.layout_dots);
        View[] dots = new View[3];
        for (int i = 0; i < 3; i++) {
            dots[i] = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(24, 24);
            params.setMargins(12, 0, 12, 0);
            dots[i].setLayoutParams(params);
            dots[i].setBackgroundResource(R.drawable.tab_indicator_dot);

            dots[i].setAlpha(i == lastTab ? 1.0f : 0.3f);

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

                prefs.edit()
                        .putInt(MainActivity.KEY_ROUTESEARCH_LAST_TAB, position)
                        .apply();
            }
        });

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