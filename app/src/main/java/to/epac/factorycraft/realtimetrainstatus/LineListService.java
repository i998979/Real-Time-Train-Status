package to.epac.factorycraft.realtimetrainstatus;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class LineListService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Context context = getApplicationContext();
            private int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            private List<String[]> list = new ArrayList<>();

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                list.clear();
                SharedPreferences prefs = context.getSharedPreferences("MtrWidgetPrefs", Context.MODE_PRIVATE);
                String state = prefs.getString("STATE_" + widgetId, "LINES");

                // Line list
                if ("LINES".equals(state)) {
                    list.add(new String[]{"機場快綫 (Airport Express)", "AEL"});
                    list.add(new String[]{"迪士尼綫 (Disneyland Resort Line)", "DRL"});
                    list.add(new String[]{"東鐵綫 (East Rail Line)", "EAL"});
                    list.add(new String[]{"港島綫 (Island Line)", "ISL"});
                    list.add(new String[]{"觀塘綫 (Kwun Tong Line)", "KTL"});
                    list.add(new String[]{"南港島綫 (South Island Line)", "SIL"});
                    list.add(new String[]{"將軍澳綫 (Tseung Kwan O Line)", "TKL"});
                    list.add(new String[]{"屯馬綫 (Tuen Ma Line)", "TML"});
                    list.add(new String[]{"東涌綫 (Tung Chung Line)", "TCL"});
                    list.add(new String[]{"荃灣綫 (Tsuen Wan Line)", "TWL"});
                }
                // Station list
                else if ("STATIONS".equals(state)) {
                    String lineId = prefs.getString("LINE_" + widgetId, "");

                    String prefix = lineId.toLowerCase();
                    int codesResId = context.getResources().getIdentifier(prefix + "_stations", "string", context.getPackageName());
                    int namesResId = context.getResources().getIdentifier(prefix + "_stations_long", "string", context.getPackageName());

                    String[] codes = context.getString(codesResId).split(" ");
                    String[] names = context.getString(namesResId).split(";");

                    for (int i = 0; i < codes.length && i < names.length; i++) {
                        list.add(new String[]{names[i], codes[i]});
                    }
                }
                // Train list
                else if ("TRAINS".equals(state)) {
                    String result = prefs.getString("RESULT_" + widgetId, "載入中...");
                    String[] lines = result.split("\n");
                    for (String l : lines) {
                        if (!l.trim().isEmpty()) {
                            list.add(new String[]{l, ""});
                        }
                    }
                }
            }

            @Override
            public void onDestroy() {
            }

            @Override
            public int getCount() {
                return list.size();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_list_item);
                String[] item = list.get(position);
                row.setTextViewText(R.id.tv_data, item[0]);

                Intent intent = new Intent();
                intent.putExtra("ITEM_NAME", item[0]);
                intent.putExtra("ITEM_ID", item[1]);
                row.setOnClickFillInIntent(R.id.item_root, intent);

                return row;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}