package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrafficNewsFragment extends Fragment {

    private static final Map<String, String> CHECK_STATIONS = new HashMap<>() {{
        put("EAL", "TAW");
        put("TML", "HUH");
        put("KTL", "PRE");
        put("AEL", "HOK");
        put("DRL", "SUN");
        put("ISL", "ADM");
        put("TCL", "HOK");
        put("TKL", "TIK");
        put("TWL", "ADM");
        put("SIL", "ADM");
    }};

    private LinearLayout statusContainer;

    private ImageView mapImageView;
    private View layoutNormal;
    private View layoutDelayed;

    private final ExecutorService crossCheckExecutor = Executors.newFixedThreadPool(CHECK_STATIONS.size());
    private boolean isFetching = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traffic_news, container, false);

        statusContainer = view.findViewById(R.id.status_container);
        mapImageView = view.findViewById(R.id.iv_system_map);
        layoutNormal = view.findViewById(R.id.layout_normal);
        layoutDelayed = view.findViewById(R.id.layout_delayed);

        fetchTrafficNews();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!crossCheckExecutor.isShutdown()) {
            crossCheckExecutor.shutdownNow();
        }
    }


    private void fetchTrafficNews() {
        if (isFetching) return;
        isFetching = true;

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

                    JSONObject root = new JSONObject(response.toString());
                    JSONArray lines = root.getJSONObject("ryg_status").getJSONArray("line");

                    fetchNextTrain(lines);

                    List<Integer> targetColors = new ArrayList<>();
                    for (int i = 0; i < lines.length(); i++) {
                        JSONObject lineObj = lines.getJSONObject(i);
                        String status = lineObj.getString("status").toLowerCase();

                        if (!status.equals("green") && !status.equals("grey") && !status.equals("typhoon")) {
                            targetColors.add(Color.parseColor(lineObj.getString("line_color")));
                        }
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!isAdded() || getView() == null) return;

                        if (!targetColors.isEmpty()) {
                            applyMultiOutlineAsync(mapImageView, targetColors, Color.RED, 20);
                        }
                        updateMainLayout(lines);
                        updateUI(lines);

                    });
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isFetching = false;
            }
        }).start();
    }

    private void updateMainLayout(JSONArray lines) {
        boolean delayed = false;
        try {
            for (int i = 0; i < lines.length(); i++) {
                String status = lines.getJSONObject(i).getString("status").toLowerCase();
                if (!status.equals("green") && !status.equals("grey") && !status.equals("typhoon")) {
                    delayed = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (delayed) {
            layoutNormal.setVisibility(View.GONE);
            layoutDelayed.setVisibility(View.VISIBLE);
        } else {
            layoutNormal.setVisibility(View.VISIBLE);
            layoutDelayed.setVisibility(View.GONE);
        }
    }

    private void fetchNextTrain(JSONArray lines) throws Exception {
        CountDownLatch latch = new CountDownLatch(lines.length());

        for (int i = 0; i < lines.length(); i++) {
            final JSONObject line = lines.getJSONObject(i);
            crossCheckExecutor.execute(() -> {
                HttpURLConnection conn = null;
                try {
                    String lineCode = line.getString("line_code").toUpperCase();
                    String sta = CHECK_STATIONS.get(lineCode);

                    URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + lineCode + "&sta=" + sta);

                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String l;
                        while ((l = r.readLine()) != null) sb.append(l);

                        JSONObject rtData = new JSONObject(sb.toString());

                        boolean isApiDelay = rtData.optString("isdelay", "N").equals("Y");
                        boolean isTimeBlank = rtData.optString("sys_time", "").equals("-") ||
                                rtData.optString("curr_time", "").equals("-");

                        if (isApiDelay || isTimeBlank) {
                            String currentStatus = line.getString("status").toLowerCase();
                            if (currentStatus.equals("green")) {
                                line.put("status", "yellow");
                                String original = line.optString("messages", "");
                                String nexttrain = "列車服務可能受阻，詳情請留意官方發出的最新車務資訊。";
                                line.put("messages", !original.isEmpty() ? original : nexttrain);
                            }
                        }
                    }
                } catch (Exception e) {
                } finally {
                    if (conn != null) conn.disconnect();
                    latch.countDown();
                }
            });
        }

        latch.await(6, TimeUnit.SECONDS);
    }

    private void updateUI(JSONArray lines) {
        try {
            statusContainer.removeAllViews();
            for (int i = 0; i < lines.length(); i++) {
                JSONObject lineObj = lines.getJSONObject(i);

                String lineCode = lineObj.getString("line_code");
                String lineNameTc = lineObj.getString("line_name_tc");
                String lineColor = lineObj.getString("line_color");
                String status = lineObj.getString("status");
                String messages = lineObj.optString("messages", "");

                if (status.equals("green") || status.equals("grey")) continue;

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

                itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getActivity(), TrafficNewsActivity.class);
                    intent.putExtra("line_code", lineCode);
                    intent.putExtra("line_name_tc", lineNameTc);
                    intent.putExtra("line_color", lineColor);
                    intent.putExtra("status", status);
                    intent.putExtra("messages", messages);
                    startActivity(intent);
                });

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
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.parseColor("#49AD7F"));
                break;
            case "yellow":
                tvStatus.setText("服務延誤");
                ivIcon.setImageResource(R.drawable.outline_exclamation_24);
                ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                break;
            case "red":
                tvStatus.setText("服務受阻");
                ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivIcon.setColorFilter(Color.parseColor("#FF0000"));
                break;
            case "pink":
                tvStatus.setText("服務延誤或受阻");
                ivIcon.setImageResource(R.drawable.baseline_warning_24);
                ivIcon.setColorFilter(Color.parseColor("#FF69B4"));
                break;
            case "typhoon":
                tvStatus.setText("熱帶氣旋警告信號生效");
                ivIcon.setImageResource(R.drawable.outline_storm_24);
                ivIcon.setColorFilter(Color.parseColor("#00BCD4"));
                break;
            case "grey":
                tvStatus.setText("非服務時間");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.GRAY);
                break;
        }
    }

    public void applyMultiOutlineAsync(ImageView imageView, List<Integer> targetColors, int shadowColor, int tolerance) {
        new Thread(() -> {
            try {
                Bitmap originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                int width = originalBitmap.getWidth();
                int height = originalBitmap.getHeight();

                int[] pixels = new int[width * height];
                originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                int[] resultPixels = pixels.clone();

                int radius = 10; // 陰影寬度

                for (int y = radius; y < height - radius; y++) {
                    for (int x = radius; x < width - radius; x++) {
                        int index = y * width + x;

                        // 檢查目前像素是否為任何一個目標顏色（縮短判斷，提升效能）
                        boolean isAnyTarget = false;
                        for (int tc : targetColors) {
                            if (isColorSimilar(pixels[index], tc, tolerance)) {
                                isAnyTarget = true;
                                break;
                            }
                        }

                        // 如果目前像素不是故障線路，我們才在它上面畫陰影
                        if (!isAnyTarget) {
                            float minDistance = radius + 1;

                            // 檢查周邊是否有故障線路的顏色
                            for (int sy = -radius; sy <= radius; sy++) {
                                for (int sx = -radius; sx <= radius; sx++) {
                                    int neighborIdx = (y + sy) * width + (x + sx);

                                    for (int tc : targetColors) {
                                        if (isColorSimilar(pixels[neighborIdx], tc, tolerance)) {
                                            float dist = (float) Math.sqrt(sx * sx + sy * sy);
                                            if (dist < minDistance) minDistance = dist;
                                            break;
                                        }
                                    }
                                    if (minDistance < 1.0f) break;
                                }
                            }

                            if (minDistance <= radius) {
                                float ratio = 1.0f - (minDistance / radius);
                                int alpha = (int) (ratio * 255);

                                // 進行簡易的 Alpha Blending，避免直接蓋掉地圖背景
                                int backgroundPixel = pixels[index];
                                int r = (Color.red(shadowColor) * alpha + Color.red(backgroundPixel) * (255 - alpha)) / 255;
                                int g = (Color.green(shadowColor) * alpha + Color.green(backgroundPixel) * (255 - alpha)) / 255;
                                int b = (Color.blue(shadowColor) * alpha + Color.blue(backgroundPixel) * (255 - alpha)) / 255;

                                resultPixels[index] = Color.rgb(r, g, b);
                            }
                        }
                    }
                }

                Bitmap resultBitmap = Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888);
                new Handler(Looper.getMainLooper()).post(() -> {
                    imageView.setImageBitmap(resultBitmap);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isColorSimilar(int color1, int color2, int tolerance) {
        return Math.abs(Color.red(color1) - Color.red(color2)) < tolerance &&
                Math.abs(Color.green(color1) - Color.green(color2)) < tolerance &&
                Math.abs(Color.blue(color1) - Color.blue(color2)) < tolerance;
    }
}