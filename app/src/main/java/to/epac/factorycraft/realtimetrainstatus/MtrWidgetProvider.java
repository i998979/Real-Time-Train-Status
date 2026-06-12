package to.epac.factorycraft.realtimetrainstatus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MtrWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        SharedPreferences prefs = context.getSharedPreferences("MtrWidgetPrefs", Context.MODE_PRIVATE);

        // 取得 Widget ID
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        String currentState = prefs.getString("STATE_" + widgetId, "LINES");

        // 1. 處理返回按鈕
        if ("ACTION_WIDGET_BACK".equals(action)) {
            if ("TRAINS".equals(currentState)) {
                prefs.edit().putString("STATE_" + widgetId, "STATIONS").apply();
                // 停止 10 秒定時刷新
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_REFRESH");
                PendingIntent pi = PendingIntent.getBroadcast(context, widgetId + 3000, i, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) am.cancel(pi);
            } else if ("STATIONS".equals(currentState)) {
                prefs.edit().putString("STATE_" + widgetId, "LINES").apply();
            }
        }
        // 2. 處理項目點擊
        else if ("ACTION_WIDGET_CLICK".equals(action)) {
            String clickedId = intent.getStringExtra("ITEM_ID");
            String clickedName = intent.getStringExtra("ITEM_NAME");

            if ("LINES".equals(currentState)) {
                // 從線路跳轉到車站列表
                prefs.edit().putString("STATE_" + widgetId, "STATIONS")
                        .putString("LINE_" + widgetId, clickedId)
                        .putString("LINE_NAME_" + widgetId, clickedName).apply();
            } else if ("STATIONS".equals(currentState)) {
                // 從車站跳轉到火車時刻
                prefs.edit().putString("STATE_" + widgetId, "TRAINS")
                        .putString("STA_" + widgetId, clickedId)
                        .putString("STA_NAME_" + widgetId, clickedName)
                        .putString("RESULT_" + widgetId, "載入中...").apply();

                context.sendBroadcast(new Intent(context, MtrWidgetProvider.class)
                        .setAction("ACTION_WIDGET_REFRESH")
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));

                // --- 在這裡呼叫！啟動循環 ---
                scheduleNextRefresh(context, widgetId);
            }
        }
        // 3. 處理重新整理
        else if ("ACTION_WIDGET_REFRESH".equals(action)) {
            if ("TRAINS".equals(prefs.getString("STATE_" + widgetId, ""))) {
                String line = prefs.getString("LINE_" + widgetId, "");
                String sta = prefs.getString("STA_" + widgetId, "");
                
                PendingResult pendingResult = goAsync();
                new Thread(() -> {
                    try {
                        fetchMtrData(context, widgetId, line, sta);
                    } finally {
                        pendingResult.finish();
                    }
                }).start();

                // --- 取代原本手寫的 AlarmManager 邏輯，改用你的方法 ---
                scheduleNextRefresh(context, widgetId);
            }
        }

        updateWidgetUI(context, AppWidgetManager.getInstance(context), widgetId, prefs);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 呼叫 onReceive 的邏輯來初始化，或手動觸發廣播
        for (int widgetId : appWidgetIds) {
            Intent intent = new Intent(context, MtrWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            onReceive(context, intent);
        }
    }

    private void scheduleNextRefresh(Context context, int widgetId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MtrWidgetProvider.class);
        intent.setAction("ACTION_WIDGET_REFRESH");
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                widgetId + 3000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 10000; // 10秒後

        // 直接使用一般鬧鐘，不需要 SCHEDULE_EXACT_ALARM 權限，也不會引發 SecurityException。
        // 在螢幕開啟的狀態下，通常依然能非常接近 10 秒執行。
        am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
    }

    private void fetchMtrData(Context context, int widgetId, String line, String sta) {
        if (line == null || sta == null || line.isEmpty() || sta.isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences("MtrWidgetPrefs", Context.MODE_PRIVATE);
        StringBuilder result = new StringBuilder();

        try {
            java.net.URL url = new java.net.URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + line + "&sta=" + sta.toUpperCase());

            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                StringBuilder response = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
                    String lineContent;
                    while ((lineContent = reader.readLine()) != null) {
                        response.append(lineContent);
                    }
                }

                org.json.JSONObject root = new org.json.JSONObject(response.toString());
                int status = root.optInt("status", 0);

                String sysTime = root.optString("sys_time", "-");
                prefs.edit().putString("TIME_" + widgetId, "最後更新：" + sysTime).apply();

                if (status == 1 && root.has("data")) {
                    org.json.JSONObject dataNode = root.getJSONObject("data");
                    String dynamicKey = line.toUpperCase() + "-" + sta.toUpperCase();

                    if (dataNode.has(dynamicKey)) {
                        org.json.JSONObject stationData = dataNode.getJSONObject(dynamicKey);

                        if (stationData.has("UP")) {
                            org.json.JSONArray upArray = stationData.getJSONArray("UP");
                            for (int i = 0; i < upArray.length(); i++) {
                                org.json.JSONObject train = upArray.getJSONObject(i);
                                String dest = train.optString("dest");
                                String destName = getStationName(context, line, dest);
                                String ttnt = train.optString("ttnt");
                                String timeStr = ("0".equals(ttnt) || "-".equals(ttnt) || "".equals(ttnt)) ? "即將抵達" : ttnt + " 分鐘";
                                result.append("↑ 往 ")
                                        .append(destName).append("：")
                                        .append(timeStr).append(" (月台")
                                        .append(train.optString("plat")).append(")\n");
                            }
                        }
                        if (stationData.has("DOWN")) {
                            org.json.JSONArray downArray = stationData.getJSONArray("DOWN");
                            for (int i = 0; i < downArray.length(); i++) {
                                org.json.JSONObject train = downArray.getJSONObject(i);
                                String dest = train.optString("dest");
                                String destName = getStationName(context, line, dest);
                                String ttnt = train.optString("ttnt");
                                String timeStr = ("0".equals(ttnt) || "-".equals(ttnt) || "".equals(ttnt)) ? "即將抵達" : ttnt + " 分鐘";
                                result.append("↓ 往 ")
                                        .append(destName).append("：")
                                        .append(timeStr).append(" (月台")
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
        
        if (!"TRAINS".equals(prefs.getString("STATE_" + widgetId, ""))) {
            return;
        }
        
        prefs.edit().putString("RESULT_" + widgetId, finalResult.isEmpty() ? "目前沒有列車" : finalResult).apply();

        // Update the widget UI
        updateWidgetUI(context, AppWidgetManager.getInstance(context), widgetId, prefs);
    }

    private String getStationName(Context context, String line, String destCode) {
        String prefix = line.toLowerCase();
        int codesResId = context.getResources().getIdentifier(prefix + "_stations", "string", context.getPackageName());
        int namesResId = context.getResources().getIdentifier(prefix + "_stations_long", "string", context.getPackageName());
        if (codesResId != 0 && namesResId != 0) {
            String[] codes = context.getString(codesResId).split(" ");
            String[] names = context.getString(namesResId).split(";");
            for (int i = 0; i < codes.length && i < names.length; i++) {
                if (codes[i].equalsIgnoreCase(destCode)) {
                    return names[i];
                }
            }
        }
        return destCode;
    }

    private void updateWidgetUI(Context context, AppWidgetManager appWidgetManager, int widgetId, SharedPreferences prefs) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_main);

        // A. 重新設置 ListView 的 Service
        Intent serviceIntent = new Intent(context, LineListService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

        // B. 重新設置點擊 Template
        Intent clickIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_CLICK").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        views.setPendingIntentTemplate(R.id.widget_list_view, PendingIntent.getBroadcast(context, widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));

        // C. 更新文字顯示
        String updatedState = prefs.getString("STATE_" + widgetId, "LINES");
        if ("LINES".equals(updatedState)) {
            views.setTextViewText(R.id.text_selected_station, "請選擇線路");
            views.setTextViewText(R.id.text_refresh_time, "");
        } else if ("STATIONS".equals(updatedState)) {
            views.setTextViewText(R.id.text_selected_station, prefs.getString("LINE_NAME_" + widgetId, ""));
            views.setTextViewText(R.id.text_refresh_time, "");
        } else {
            views.setTextViewText(R.id.text_selected_station, prefs.getString("LINE_NAME_" + widgetId, "") + " - " + prefs.getString("STA_NAME_" + widgetId, ""));
            views.setTextViewText(R.id.text_refresh_time, prefs.getString("TIME_" + widgetId, ""));
        }

        // D. 重新綁定按鈕
        Intent backIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_BACK").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        views.setOnClickPendingIntent(R.id.widget_btn_back, PendingIntent.getBroadcast(context, widgetId + 1000, backIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent refreshIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_REFRESH").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, PendingIntent.getBroadcast(context, widgetId + 2000, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent dummyIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_DUMMY");
        views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getBroadcast(context, widgetId + 4000, dummyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        appWidgetManager.updateAppWidget(widgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view);
    }
}