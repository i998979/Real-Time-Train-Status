package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TrafficNewsFragment extends Fragment {

    private LinearLayout statusContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traffic_news, container, false);
        statusContainer = view.findViewById(R.id.status_container);

        fetchTrafficNews();
        return view;
    }

    private void fetchTrafficNews() {
        new Thread(() -> {
            try {
                URL url = new URL("https://tnews.mtr.com.hk/alert/ryg_line_status.json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String finalJsonData = response.toString();
                    new Handler(Looper.getMainLooper()).post(() -> parseAndPopulate(finalJsonData));
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseAndPopulate(String json) {
        try {
            statusContainer.removeAllViews();
            JSONObject root = new JSONObject(json);
            JSONArray lines = root.getJSONObject("ryg_status").getJSONArray("line");

            for (int i = 0; i < lines.length(); i++) {
                JSONObject lineObj = lines.getJSONObject(i);

                String lineCode = lineObj.getString("line_code");
                String lineNameTc = lineObj.getString("line_name_tc");
                String lineColor = lineObj.getString("line_color");
                String status = lineObj.getString("status");
                String messages = lineObj.optString("messages", "");

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_line_status, statusContainer, false);

                View vColorBar = itemView.findViewById(R.id.v_line_color_bar);
                View vBadgeLayout = itemView.findViewById(R.id.line_color_badge);
                TextView tvLineCode = itemView.findViewById(R.id.tv_line_code_badge);
                TextView tvLineName = itemView.findViewById(R.id.tv_line);
                TextView tvStatus = itemView.findViewById(R.id.tv_status);
                TextView tvMessage = itemView.findViewById(R.id.tv_message);
                ImageView ivIcon = itemView.findViewById(R.id.iv_status_icon);

                int colorInt = Color.parseColor(lineColor);
                vColorBar.setBackgroundColor(colorInt);
                vBadgeLayout.setBackgroundColor(colorInt);
                tvLineCode.setText(lineCode);
                tvLineName.setText(lineNameTc);
                tvMessage.setText(messages.isEmpty() ? "服務正常" : messages);

                updateStatusUI(status, tvStatus, ivIcon);

                statusContainer.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatusUI(String status, TextView tvStatus, ImageView ivIcon) {
        switch (status.toLowerCase()) {
            case "green":
                tvStatus.setText("服務正常");
                tvStatus.setTextColor(Color.WHITE);
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.parseColor("#49AD7F"));
                break;
            case "yellow":
                tvStatus.setText("服務延誤");
                tvStatus.setTextColor(Color.parseColor("#FFA500"));
                ivIcon.setImageResource(R.drawable.outline_exclamation_24);
                ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                break;
            case "red":
                tvStatus.setText("服務受阻");
                tvStatus.setTextColor(Color.parseColor("#FF0000"));
                ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivIcon.setColorFilter(Color.parseColor("#FF0000"));
                break;
            case "pink":
                tvStatus.setText("服務延誤或受阻");
                tvStatus.setTextColor(Color.parseColor("#FF69B4"));
                ivIcon.setImageResource(R.drawable.baseline_warning_24);
                ivIcon.setColorFilter(Color.parseColor("#FF69B4"));
                break;
            case "typhoon":
                tvStatus.setText("熱帶氣旋警告信號生效");
                tvStatus.setTextColor(Color.parseColor("#00BCD4"));
                ivIcon.setImageResource(R.drawable.outline_storm_24);
                ivIcon.setColorFilter(Color.parseColor("#00BCD4"));
                break;
            case "grey":
                tvStatus.setText("非服務時間");
                tvStatus.setTextColor(Color.GRAY);
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.GRAY);
                break;
        }
    }
}