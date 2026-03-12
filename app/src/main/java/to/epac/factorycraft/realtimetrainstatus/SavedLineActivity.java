package to.epac.factorycraft.realtimetrainstatus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class SavedLineActivity extends AppCompatActivity {
    private static final List<String> subTitles = Arrays.asList("運行情報", "列車走行位置");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_line);

        Intent intent = getIntent();
        String lineCode = intent.getStringExtra("line_code");
        String lineNameTc = intent.getStringExtra("line_name_tc");
        String lineColor = intent.getStringExtra("line_color");
        String status = intent.getStringExtra("status");
        String messages = intent.getStringExtra("messages");

        FrameLayout lineBanner = findViewById(R.id.line_banner);
        FrameLayout badgeBg = findViewById(R.id.line_color_badge);
        TextView tvCodeBadge = findViewById(R.id.tv_line_code_badge);
        TextView tvLineName = findViewById(R.id.tv_banner_name);

        badgeBg.setBackgroundColor(Color.parseColor(lineColor));
        tvCodeBadge.setText(lineCode);
        tvLineName.setText(lineNameTc);

        MaterialButton btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });

        ViewPager2 pagerContent = findViewById(R.id.pager_content);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        SavedLinePagerAdapter adapter = new SavedLinePagerAdapter(this, lineCode, lineNameTc, lineColor, status, messages);
        pagerContent.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, pagerContent, (tab, position) -> {
            tab.setText(subTitles.get(position));
        }).attach();

        pagerContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                lineBanner.animate()
                        .translationY(0)
                        .setDuration(200)
                        .start();
                tabLayout.animate()
                        .translationY(0)
                        .setDuration(200)
                        .start();
            }
        });
    }

    private static class SavedLinePagerAdapter extends FragmentStateAdapter {
        private final String lineCode;
        private final String lineNameTc;
        private final String lineColor;
        private final String status;
        private final String messages;

        public SavedLinePagerAdapter(@NonNull FragmentActivity fragmentActivity, String lineCode,
                                     String lineNameTc, String lineColor, String status, String messages) {
            super(fragmentActivity);
            this.lineCode = lineCode;
            this.lineNameTc = lineNameTc;
            this.lineColor = lineColor;
            this.status = status;
            this.messages = messages;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            Bundle args = new Bundle();

            if (position == 0) {
                fragment = new TrafficNews2Fragment();

                args.putString("line_code", lineCode);
                args.putString("line_name_tc", lineNameTc);
                args.putString("line_color", lineColor);
                args.putString("status", status);
                args.putString("messages", messages);
            } else {
                fragment = new TrainLocationFragment();

                args.putString("LINE_CODE", lineCode);
                args.putString("DATA_SOURCE", "OPENDATA");
            }

            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return subTitles.size();
        }
    }
}