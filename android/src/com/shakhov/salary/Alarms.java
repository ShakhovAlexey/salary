package com.shakhov.salary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

/** Ставит системные будильники (setAlarmClock) на ближайшие смены. */
final class Alarms {
    static final int BASE = 1000, MAX = 40, SNOOZE = 9999;

    static PendingIntent fire(Context c, int code, String title) {
        Intent i = new Intent(c, AlarmReceiver.class).putExtra("title", title);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static PendingIntent show(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        return PendingIntent.getActivity(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void reschedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        int old = Store.getCount(c);
        for (int i = 0; i < Math.max(old, MAX); i++) am.cancel(fire(c, BASE + i, ""));
        JSONObject d = Store.load(c);
        int n = 0;
        if (d.optBoolean("on", true)) {
            JSONArray a = Store.arr(d, "alarms");
            long now = System.currentTimeMillis();
            for (int i = 0; i < a.length() && n < MAX; i++) {
                JSONObject x = a.optJSONObject(i);
                if (x == null) continue;
                long at = x.optLong("at", 0);
                if (at <= now) continue;
                set(c, am, at, BASE + n, x.optString("title", "Смена"));
                n++;
            }
        }
        Store.setCount(c, n);
    }
    static void set(Context c, AlarmManager am, long at, int code, String title) {
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show(c)), fire(c, code, title));
        } catch (SecurityException e) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire(c, code, title));
        }
    }
    static void snooze(Context c, String title, int minutes) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        set(c, am, System.currentTimeMillis() + minutes * 60000L, SNOOZE, title);
    }
}
