package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TrafficNews2Fragment extends Fragment {

    private HRConfig hrConf;

    TextView tvRefreshTime;

    LinearLayout normalLayout;

    LinearLayout delayedLayout;
    ImageView ivStatusIcon;
    TextView tvStatus;
    TextView tvLineSection;
    TextView tvReason;

    TextView tvDetail;
    WebView wvDetail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_traffic_news2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hrConf = HRConfig.getInstance(requireContext());

        tvRefreshTime = view.findViewById(R.id.tv_refresh_time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String currentTime = dateFormat.format(new Date());
        tvRefreshTime.setText(currentTime);

        normalLayout = view.findViewById(R.id.layout_normal);

        delayedLayout = view.findViewById(R.id.layout_delayed);
        ivStatusIcon = view.findViewById(R.id.iv_status_icon);
        tvStatus = view.findViewById(R.id.tv_status);
        tvLineSection = view.findViewById(R.id.tv_line_section);
        tvReason = view.findViewById(R.id.tv_reason);

        tvDetail = view.findViewById(R.id.tv_detail);
        wvDetail = view.findViewById(R.id.wv_detail);
        WebSettings webSettings = wvDetail.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wvDetail.setWebViewClient(new WebViewClient());


        String lineCode = getArguments().getString("line_code");
        String lineNameTc = getArguments().getString("line_name_tc");
        String lineColor = getArguments().getString("line_color");
        String status = getArguments().getString("status");
        String messages = getArguments().getString("messages");


        // Non-traffic Hour
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int startTime = 1 * 60 + 30; // 01:30
        int endTime = 5 * 60;        // 05:00

        boolean isMaintenanceTime = currentTimeInMinutes >= startTime && currentTimeInMinutes < endTime;

        LinearLayout nthMessage = view.findViewById(R.id.layout_nth);
        nthMessage.setVisibility(isMaintenanceTime ? View.VISIBLE : View.GONE);


        // Only parse message if delayed
        if (status.equalsIgnoreCase("green") | status.equalsIgnoreCase("grey")) {
            normalLayout.setVisibility(View.VISIBLE);
            delayedLayout.setVisibility(View.GONE);
        } else {
            normalLayout.setVisibility(View.GONE);
            delayedLayout.setVisibility(View.VISIBLE);

            parseTrafficMessages(messages);
            applyStatusUI(status);
        }
    }

    private void parseTrafficMessages(String messages) {
        String reason = "";
        String detail = "";

        if (messages == null || messages.trim().isEmpty()) {
            detail = "現時，列車服務運作正常。";
        } else if (messages.trim().startsWith("{")) {
            try {
                JSONObject root = new JSONObject(messages);
                JSONObject msgObj = root.optJSONObject("message");
                if (msgObj != null) {
                    reason = msgObj.optString("title_tc", "").trim();

                    String cause = msgObj.optString("cause_tc", "").trim();
                    detail = cause.equalsIgnoreCase("null") ? "" : cause;

                    String url = msgObj.optString("url_tc", "").trim();
                    if (!url.isEmpty())
                        wvDetail.loadUrl(url);

                    parseAffectedAreas(msgObj.optJSONObject("affected_areas"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            detail = messages;
        }

        tvReason.setText(reason);
        tvDetail.setText(detail);
    }

    private void parseAffectedAreas(JSONObject affectedAreas) {
        if (affectedAreas == null) {
            tvLineSection.setText("全綫");
            return;
        }

        try {
            Object area = affectedAreas.opt("affected_area");
            JSONObject targetArea = null;

            if (area instanceof JSONArray && ((JSONArray) area).length() > 0) {
                targetArea = ((JSONArray) area).getJSONObject(0);
            } else if (area instanceof JSONObject) {
                targetArea = (JSONObject) area;
            }

            if (targetArea != null) {
                String fr = targetArea.optString("station_code_fr");
                String to = targetArea.optString("station_code_to");
                if (!fr.isEmpty() && !to.isEmpty()) {
                    String section = hrConf.getStationName(Integer.parseInt(fr)) + "~"
                            + hrConf.getStationName(Integer.parseInt(to));
                    tvLineSection.setText(section);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tvLineSection.setText("全綫");
    }

    private void applyStatusUI(String status) {
        String currentReason = tvReason.getText().toString();
        String displayStatus;
        int iconRes;
        int iconColor;

        switch (status.toLowerCase()) {
            case "yellow":
                displayStatus = "服務延誤";
                iconRes = R.drawable.outline_exclamation_24;
                iconColor = Color.parseColor("#FFA500");
                break;
            case "red":
                displayStatus = "服務受阻";
                iconRes = android.R.drawable.ic_menu_close_clear_cancel;
                iconColor = Color.RED;
                break;
            case "pink":
                displayStatus = "服務延誤或受阻";
                iconRes = R.drawable.baseline_warning_24;
                iconColor = Color.parseColor("#FF69B4");
                break;
            case "typhoon":
                displayStatus = "熱帶氣旋警告信號生效";
                iconRes = R.drawable.baseline_storm_24;
                iconColor = Color.parseColor("#00BCD4");
                break;
            case "grey":
                displayStatus = "非服務時間";
                iconRes = R.drawable.baseline_trip_origin_24;
                iconColor = Color.GRAY;
                break;
            case "green":
            default:
                displayStatus = "服務正常";
                iconRes = R.drawable.baseline_trip_origin_24;
                iconColor = Color.parseColor("#49AD7F");
                break;
        }

        tvStatus.setText(displayStatus);
        ivStatusIcon.setImageResource(iconRes);
        ivStatusIcon.setColorFilter(iconColor);
        if (currentReason.isEmpty()) tvReason.setText(displayStatus);
    }
}