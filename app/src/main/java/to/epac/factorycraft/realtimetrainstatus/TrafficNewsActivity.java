package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TrafficNewsActivity extends AppCompatActivity {

    View lineColorBadge;
    TextView tvLineCodeBadge;
    TextView tvBannerName;
    MaterialButton btnClose;
    ImageView ivIcon;
    TextView tvStatus;
    TextView tvStatus2;
    TextView tvReason;
    TextView tvDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_news);

        lineColorBadge = findViewById(R.id.line_color_badge);
        tvLineCodeBadge = findViewById(R.id.tv_line_code_badge);
        tvBannerName = findViewById(R.id.tv_banner_name);
        btnClose = findViewById(R.id.btn_close);
        findViewById(R.id.btn_close).setOnClickListener(v -> {
            finish();
        });
        ivIcon = findViewById(R.id.iv_status_icon);
        tvStatus = findViewById(R.id.tv_status);
        tvStatus2 = findViewById(R.id.tv_status2);
        tvReason = findViewById(R.id.tv_reason);
        tvDetail = findViewById(R.id.tv_detail);

        String lineCode = getIntent().getStringExtra("line_code");
        String lineNameTc = getIntent().getStringExtra("line_name_tc");
        String lineColor = getIntent().getStringExtra("line_color");
        String status = getIntent().getStringExtra("status");
        String messages = getIntent().getStringExtra("messages");

        lineColorBadge.setBackgroundColor(Color.parseColor(lineColor));
        tvLineCodeBadge.setText(lineCode);
        tvBannerName.setText(lineNameTc);
        tvDetail.setText(messages.isEmpty() ? "現在，列車服務運作正常。" : messages);


        switch (status.toLowerCase()) {
            case "green":
                tvStatus.setText("服務正常");
                tvReason.setText("服務正常");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.parseColor("#49AD7F"));
                break;
            case "yellow":
                tvStatus.setText("服務延誤");
                tvReason.setText("服務延誤");
                ivIcon.setImageResource(R.drawable.outline_exclamation_24);
                ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                break;
            case "red":
                tvStatus.setText("服務受阻");
                tvReason.setText("服務受阻");
                ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivIcon.setColorFilter(Color.parseColor("#FF0000"));
                break;
            case "pink":
                tvStatus.setText("服務延誤或受阻");
                tvReason.setText("服務延誤或受阻");
                ivIcon.setImageResource(R.drawable.baseline_warning_24);
                ivIcon.setColorFilter(Color.parseColor("#FF69B4"));
                break;
            case "typhoon":
                tvStatus.setText("熱帶氣旋警告信號生效");
                tvReason.setText("熱帶氣旋警告信號生效");
                ivIcon.setImageResource(R.drawable.outline_storm_24);
                ivIcon.setColorFilter(Color.parseColor("#00BCD4"));
                break;
            case "grey":
                tvStatus.setText("非服務時間");
                tvReason.setText("非服務時間");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.GRAY);
                break;
        }
    }
}