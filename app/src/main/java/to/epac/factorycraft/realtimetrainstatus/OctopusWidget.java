package to.epac.factorycraft.realtimetrainstatus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.RemoteViews;

public class OctopusWidget extends AppWidgetProvider {

    public static final String OCTOPUS_PACKAGE = "com.octopuscards.nfc_reader";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_octopus);

            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(OCTOPUS_PACKAGE);

            if (intent != null) {
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
            } else {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + OCTOPUS_PACKAGE));
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 0, marketIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}