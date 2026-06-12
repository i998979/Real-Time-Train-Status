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
                Data data = new Data.Builder()
                        .putInt("WIDGET_ID", widgetId)
                        .putString("LINE", prefs.getString("LINE_" + widgetId, ""))
                        .putString("STA", prefs.getString("STA_" + widgetId, ""))
                        .build();
                WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(FetchMtrWorker.class).setInputData(data).build());

                // --- 取代原本手寫的 AlarmManager 邏輯，改用你的方法 ---
                scheduleNextRefresh(context, widgetId);
            }
        }

        // --- 核心修正：統一更新 RemoteViews 的所有設定 ---
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_main);

        // A. 重新設置 ListView 的 Service (確保跳轉後列表能刷出新內容)
        Intent serviceIntent = new Intent(context, LineListService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

        // B. 重新設置點擊 Template (確保新生成的列表項目依然可點擊)
        Intent clickIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_CLICK").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        views.setPendingIntentTemplate(R.id.widget_list_view, PendingIntent.getBroadcast(context, widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));

        // C. 更新文字顯示
        String updatedState = prefs.getString("STATE_" + widgetId, "LINES");
        if ("LINES".equals(updatedState)) {
            views.setTextViewText(R.id.text_selected_station, "請選擇線路");
            views.setTextViewText(R.id.text_refresh_time, "");
        } else if ("STATIONS".equals(updatedState)) {
            views.setTextViewText(R.id.text_selected_station, "線路: " + prefs.getString("LINE_NAME_" + widgetId, ""));
            views.setTextViewText(R.id.text_refresh_time, "");
        } else {
            views.setTextViewText(R.id.text_selected_station, "車站: " + prefs.getString("STA_NAME_" + widgetId, ""));
            views.setTextViewText(R.id.text_refresh_time, prefs.getString("TIME_" + widgetId, ""));
        }

        // D. 重新綁定按鈕
        Intent backIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_BACK").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        views.setOnClickPendingIntent(R.id.widget_btn_back, PendingIntent.getBroadcast(context, widgetId + 1000, backIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent refreshIntent = new Intent(context, MtrWidgetProvider.class).setAction("ACTION_WIDGET_REFRESH").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, PendingIntent.getBroadcast(context, widgetId + 2000, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // 執行更新
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(widgetId, views);
        // 通知 Factory 觸發 onDataSetChanged() 來讀取新的 STATE
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view);

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
}