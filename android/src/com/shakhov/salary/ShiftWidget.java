package com.shakhov.salary;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Виджет: следующая смена, будильник, нетто за месяц. */
public class ShiftWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { update(c, m, ids); }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        update(c, m, m.getAppWidgetIds(new ComponentName(c, ShiftWidget.class)));
    }

    static void update(Context c, AppWidgetManager m, int[] ids) {
        JSONObject d = Store.load(c);
        String next = "Нет смен впереди", alarm = "";
        long now = System.currentTimeMillis();
        JSONArray s = Store.arr(d, "shifts");
        for (int i = 0; i < s.length(); i++) {
            JSONObject x = s.optJSONObject(i);
            if (x != null && x.optLong("end", 0) > now) { next = x.optString("label", ""); alarm = x.optString("alarm", ""); break; }
        }
        if (s.length() == 0) next = "Открой приложение";
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.next, next);
        v.setTextViewText(R.id.alarm, alarm);
        v.setTextViewText(R.id.net, d.optString("net", ""));
        PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, open);
        for (int id : ids) m.updateAppWidget(id, v);
    }
}
