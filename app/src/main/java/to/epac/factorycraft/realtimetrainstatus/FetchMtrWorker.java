package to.epac.factorycraft.realtimetrainstatus;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchMtrWorker extends Worker {

    public FetchMtrWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int widgetId = getInputData().getInt("WIDGET_ID", AppWidgetManager.INVALID_APPWIDGET_ID);
        String line = getInputData().getString("LINE");
        String sta = getInputData().getString("STA");

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || line == null || sta == null) {
            return Result.failure();
        }

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MtrWidgetPrefs", Context.MODE_PRIVATE);
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + line + "&sta=" + sta.toUpperCase());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String lineContent;
                    while ((lineContent = reader.readLine()) != null) {
                        response.append(lineContent);
                    }
                }

                JSONObject root = new JSONObject(response.toString());
                int status = root.optInt("status", 0);

                String sysTime = root.optString("sys_time", "-");
                prefs.edit().putString("TIME_" + widgetId, "最後更新：" + sysTime).apply();

                if (status == 1 && root.has("data")) {
                    JSONObject dataNode = root.getJSONObject("data");
                    String dynamicKey = line.toUpperCase() + "-" + sta.toUpperCase();

                    if (dataNode.has(dynamicKey)) {
                        JSONObject stationData = dataNode.getJSONObject(dynamicKey);

                        if (stationData.has("UP")) {
                            JSONArray upArray = stationData.getJSONArray("UP");
                            for (int i = 0; i < upArray.length(); i++) {
                                JSONObject train = upArray.getJSONObject(i);
                                result.append("↑ 往 ")
                                        .append(train.optString("dest")).append("：")
                                        .append(train.optString("ttnt")).append(" 分鐘 (月台")
                                        .append(train.optString("plat")).append(")\n");
                            }
                        }
                        if (stationData.has("DOWN")) {
                            JSONArray downArray = stationData.getJSONArray("DOWN");
                            for (int i = 0; i < downArray.length(); i++) {
                                JSONObject train = downArray.getJSONObject(i);
                                result.append("↓ 往 ")
                                        .append(train.optString("dest")).append("：")
                                        .append(train.optString("ttnt")).append(" 分鐘 (月台")
                                        .append(train.optString("plat")).append(")\n");
                            }
                        }
                    } else {
                        result.append("暫無班次資料");
                    }
                } else {
                    result.append("API 資料異常");
                }
            } else {
                result.append("網路錯誤: ").append(connection.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.append("連線失敗");
        }

        String finalResult = result.toString().trim();
        prefs.edit().putString("RESULT_" + widgetId, finalResult.isEmpty() ? "目前沒有列車" : finalResult).apply();

        AppWidgetManager.getInstance(getApplicationContext()).notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view);

        return Result.success();
    }
}