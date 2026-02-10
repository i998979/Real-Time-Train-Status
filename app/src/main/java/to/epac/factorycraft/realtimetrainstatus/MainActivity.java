package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 pagerContent;
    private TabLayout tabLayout;
    private List<String> titles = Arrays.asList("時刻表", "位置圖", "街道圖", "列車走行位置");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pagerContent = findViewById(R.id.pager_content);
        tabLayout = findViewById(R.id.tab_layout);

        pagerContent.setAdapter(new MainPagerAdapter(this));
        pagerContent.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, pagerContent, (tab, position) -> {
            tab.setText(titles.get(position));
        }).attach();

        pagerContent.setCurrentItem(3, false);
    }
}