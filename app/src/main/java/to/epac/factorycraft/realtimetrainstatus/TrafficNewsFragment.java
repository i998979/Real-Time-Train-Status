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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrafficNewsFragment extends Fragment {

    private HRConfig hrConf;

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvRefreshTime;
    private LinearLayout statusContainer;

    private ImageView mapImageView;
    private View layoutNormal;
    private View layoutDelayed;

    private final ExecutorService crossCheckExecutor = Executors.newFixedThreadPool(MainActivity.NEXTTRAIN_CHECK_STATIONS.size());
    private boolean isFetching = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traffic_news, container, false);

        hrConf = HRConfig.getInstance(getContext());

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        tvRefreshTime = view.findViewById(R.id.tv_refresh_time);
        statusContainer = view.findViewById(R.id.status_container);
        mapImageView = view.findViewById(R.id.iv_system_map);
        layoutNormal = view.findViewById(R.id.layout_normal);
        layoutDelayed = view.findViewById(R.id.layout_delayed);

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int startTime = 1 * 60 + 30; // 01:30
        int endTime = 5 * 60;        // 05:00

        boolean isMaintenanceTime = currentTimeInMinutes >= startTime && currentTimeInMinutes < endTime;

        LinearLayout nthMessage = view.findViewById(R.id.layout_nth);
        if (isMaintenanceTime)
            nthMessage.setVisibility(View.VISIBLE);
        else
            nthMessage.setVisibility(View.GONE);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isMaintenanceTime)
                fetchTrafficNews();
            else
                swipeRefreshLayout.setRefreshing(false);
        });

        tvRefreshTime.setText("");
        if (!isMaintenanceTime) fetchTrafficNews();

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
                            if (lineObj.getString("line_code").equalsIgnoreCase("SIL"))
                                targetColors.add(Color.parseColor("#CDD002"));
                            else
                                targetColors.add(Color.parseColor(lineObj.getString("line_color")));
                        }
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!isAdded() || getView() == null) return;

                        if (!targetColors.isEmpty()) {
                            applyMultiOutlineAsync(mapImageView, targetColors, Color.parseColor("#E18E83"), 35);
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

                new Handler(Looper.getMainLooper()).post(() -> {
                    swipeRefreshLayout.setRefreshing(false);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                    String currentTime = dateFormat.format(new Date());
                    tvRefreshTime.setText(currentTime);
                });
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
                    String sta = MainActivity.NEXTTRAIN_CHECK_STATIONS.get(lineCode);

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
                String lineSection = "全綫";

                String displayMessage = "列車服務正常";
                Object messagesObj = lineObj.opt("messages");

                if (messagesObj instanceof JSONObject) {
                    JSONObject msgObj = ((JSONObject) messagesObj).optJSONObject("message");
                    if (msgObj != null) {
                        String title = msgObj.optString("title_tc", "");
                        String cause = msgObj.optString("cause_tc", "");
                        displayMessage = !title.isEmpty() ? title : cause;

                        JSONObject affectedAreas = msgObj.optJSONObject("affected_areas");
                        if (affectedAreas != null) {
                            JSONObject affectedAreaObj = affectedAreas.optJSONObject("affected_area");

                            if (affectedAreaObj != null) {
                                String stationFr = affectedAreaObj.optString("station_code_fr");
                                String stationTo = affectedAreaObj.optString("station_code_to");

                                if (!stationFr.isEmpty() && !stationTo.isEmpty()) {
                                    lineSection = hrConf.getStationName(Integer.parseInt(stationFr)) + "~"
                                            + hrConf.getStationName(Integer.parseInt(stationTo));
                                }
                            }
                        }
                    }
                } else if (messagesObj instanceof String) {
                    String msgStr = (String) messagesObj;
                    if (!msgStr.isEmpty()) {
                        displayMessage = msgStr;
                    }
                }

                if (status.equals("green") || status.equals("grey")) continue;

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_line_status, statusContainer, false);

                View vColorBar = itemView.findViewById(R.id.v_line_color_bar);
                View vBadgeLayout = itemView.findViewById(R.id.line_color_badge);
                TextView tvLineCode = itemView.findViewById(R.id.tv_line_code_badge);
                TextView tvLineName = itemView.findViewById(R.id.tv_line);
                TextView tvLineSection = itemView.findViewById(R.id.tv_line_section);
                TextView tvStatus = itemView.findViewById(R.id.tv_status);
                TextView tvMessage = itemView.findViewById(R.id.tv_message);
                ImageView ivIcon = itemView.findViewById(R.id.iv_status_icon);

                int colorInt = Color.parseColor(lineColor);
                vColorBar.setBackgroundColor(colorInt);
                vBadgeLayout.setBackgroundColor(colorInt);
                tvLineCode.setText(lineCode);
                tvLineName.setText(lineNameTc);
                tvLineSection.setText(lineSection);
                tvMessage.setText(displayMessage);

                updateStatusUI(status, tvStatus, ivIcon);

                itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getActivity(), TrafficNewsActivity.class);
                    intent.putExtra("line_code", lineCode);
                    intent.putExtra("line_name_tc", lineNameTc);
                    intent.putExtra("line_color", lineColor);
                    intent.putExtra("status", status);
                    intent.putExtra("messages", messagesObj.toString());
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
                ivIcon.setImageResource(R.drawable.baseline_storm_24);
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
        int displayWidth = imageView.getWidth();
        int displayHeight = imageView.getHeight();
        if (displayWidth <= 0 || displayHeight <= 0) return;

        new Thread(() -> {
            try {
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap originalBitmap = drawable.getBitmap();

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, displayWidth, displayHeight, false);
                int width = scaledBitmap.getWidth();
                int height = scaledBitmap.getHeight();

                int[] pixels = new int[width * height];
                scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                float[] distMap = new float[width * height];
                Arrays.fill(distMap, 1000f);

                int tolSq = tolerance * tolerance;

                for (int i = 0; i < pixels.length; i++) {
                    int p = pixels[i];
                    if (((p >> 24) & 0xFF) < 10) continue;

                    for (int tc : targetColors) {
                        int r = ((p >> 16) & 0xFF) - ((tc >> 16) & 0xFF);
                        int g = ((p >> 8) & 0xFF) - ((tc >> 8) & 0xFF);
                        int b = (p & 0xFF) - (tc & 0xFF);
                        if ((r * r + g * g + b * b) < tolSq) {
                            distMap[i] = 0;
                            break;
                        }
                    }
                }


                for (int y = 1; y < height; y++) {
                    for (int x = 1; x < width; x++) {
                        int i = y * width + x;
                        distMap[i] = Math.min(distMap[i], Math.min(distMap[i - 1] + 1, distMap[i - width] + 1));
                    }
                }
                for (int y = height - 2; y >= 0; y--) {
                    for (int x = width - 2; x >= 0; x--) {
                        int i = y * width + x;
                        distMap[i] = Math.min(distMap[i], Math.min(distMap[i + 1] + 1, distMap[i + width] + 1));
                    }
                }


                int shadowR = (shadowColor >> 16) & 0xFF;
                int shadowG = (shadowColor >> 8) & 0xFF;
                int shadowB = shadowColor & 0xFF;
                int radius = 10;

                for (int i = 0; i < pixels.length; i++) {
                    float d = distMap[i];
                    if (d > 0 && d <= radius) {
                        float alpha = (1.0f - d / radius);
                        int a = (int) (alpha * 255);

                        int bg = pixels[i];
                        int r = (shadowR * a + ((bg >> 16) & 0xFF) * (255 - a)) >> 8;
                        int g = (shadowG * a + ((bg >> 8) & 0xFF) * (255 - a)) >> 8;
                        int b = (shadowB * a + (bg & 0xFF) * (255 - a)) >> 8;
                        pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                }

                Bitmap result = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
                new Handler(Looper.getMainLooper()).post(() -> imageView.setImageBitmap(result));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}