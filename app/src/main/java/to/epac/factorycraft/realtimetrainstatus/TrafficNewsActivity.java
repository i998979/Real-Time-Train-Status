package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

public class TrafficNewsActivity extends AppCompatActivity {

    private HRConfig hrConf;

    View lineColorBadge;
    TextView tvLineCodeBadge;
    TextView tvBannerName;
    MaterialButton btnClose;
    ImageView ivIcon;
    TextView tvStatus;
    TextView tvLineSection;
    TextView tvReason;
    TextView tvDetail;
    WebView wvDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_news);

        hrConf = HRConfig.getInstance(this);

        lineColorBadge = findViewById(R.id.line_color_badge);
        tvLineCodeBadge = findViewById(R.id.tv_line_code_badge);
        tvBannerName = findViewById(R.id.tv_banner_name);
        btnClose = findViewById(R.id.btn_close);
        findViewById(R.id.btn_close).setOnClickListener(v -> {
            finish();
        });
        ivIcon = findViewById(R.id.iv_status_icon);
        tvStatus = findViewById(R.id.tv_status);
        tvLineSection = findViewById(R.id.tv_line_section);
        tvReason = findViewById(R.id.tv_reason);
        tvDetail = findViewById(R.id.tv_detail);
        wvDetail = findViewById(R.id.wv_detail);

        WebSettings webSettings = wvDetail.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wvDetail.setWebViewClient(new WebViewClient());

        String lineCode = getIntent().getStringExtra("line_code");
        String lineNameTc = getIntent().getStringExtra("line_name_tc");
        String lineColor = getIntent().getStringExtra("line_color");
        String status = getIntent().getStringExtra("status");
        String messages = getIntent().getStringExtra("messages");

        try {
            JSONObject messagesObj = new JSONObject(messages);
            JSONObject msgObj = messagesObj.getJSONObject("message");

            tvReason.setText(msgObj.optString("title_tc", ""));
            String cause = msgObj.optString("cause_tc", "");
            tvDetail.setText(cause.equals("null") ? "" : cause);

            String url = msgObj.optString("url_tc");
            if (!url.isEmpty()) {
                wvDetail.loadUrl(url);
            }

            String lineSection = "全綫";

            JSONObject affectedAreas = msgObj.optJSONObject("affected_areas");
            if (affectedAreas != null) {
                Object affectedArea = affectedAreas.opt("affected_area");

                if (affectedArea instanceof JSONArray) {
                    JSONArray areaArray = (JSONArray) affectedArea;
                    if (areaArray.length() > 0) {
                        JSONObject areaObj = areaArray.getJSONObject(0);
                        String fr = areaObj.optString("station_code_fr");
                        String to = areaObj.optString("station_code_to");
                        if (!fr.isEmpty() && !to.isEmpty()) {
                            lineSection = hrConf.getStationName(Integer.parseInt(fr)) + "~"
                                    + hrConf.getStationName(Integer.parseInt(to));
                        }
                    }
                } else if (affectedArea instanceof JSONObject) {
                    JSONObject areaObj = (JSONObject) affectedArea;
                    String fr = areaObj.optString("station_code_fr");
                    String to = areaObj.optString("station_code_to");
                    if (!fr.isEmpty() && !to.isEmpty()) {
                        lineSection = hrConf.getStationName(Integer.parseInt(fr)) + "~"
                                + hrConf.getStationName(Integer.parseInt(to));
                    }
                }
            }

            tvLineSection.setText(lineSection);
        } catch (Exception e) {
            e.printStackTrace();
            tvDetail.setText(messages.isEmpty() ? "現在，列車服務運作正常。" : messages);
        }

        lineColorBadge.setBackgroundColor(Color.parseColor(lineColor));
        tvLineCodeBadge.setText(lineCode);
        tvBannerName.setText(lineNameTc);


        switch (status.toLowerCase()) {
            case "green":
                tvStatus.setText("服務正常");
                if (tvReason.getText().toString().isEmpty())
                    tvReason.setText("服務正常");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.parseColor("#49AD7F"));
                break;
            case "yellow":
                tvStatus.setText("服務延誤");
                if (tvReason.getText().toString().isEmpty())
                    tvReason.setText("服務延誤");
                ivIcon.setImageResource(R.drawable.outline_exclamation_24);
                ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                break;
            case "red":
                tvStatus.setText("服務受阻");
                if (tvReason.getText().toString().isEmpty())
                    tvReason.setText("服務受阻");
                ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivIcon.setColorFilter(Color.parseColor("#FF0000"));
                break;
            case "pink":
                tvStatus.setText("服務延誤或受阻");
                if (tvReason.getText().toString().isEmpty())
                    tvReason.setText("服務延誤或受阻");
                ivIcon.setImageResource(R.drawable.baseline_warning_24);
                ivIcon.setColorFilter(Color.parseColor("#FF69B4"));
                break;
            case "typhoon":
                tvStatus.setText("熱帶氣旋警告信號生效");
                if (tvReason.getText().toString().isEmpty())
                    tvReason.setText("熱帶氣旋警告信號生效");
                ivIcon.setImageResource(R.drawable.baseline_storm_24);
                ivIcon.setColorFilter(Color.parseColor("#00BCD4"));
                break;
            case "grey":
                tvStatus.setText("非服務時間");
                if (tvReason.getText().toString().isEmpty())
                    tvReason.setText("非服務時間");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.GRAY);
                break;
        }
    }
}