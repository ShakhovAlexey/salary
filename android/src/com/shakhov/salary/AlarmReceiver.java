package com.shakhov.salary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent in) {
        Intent s = new Intent(c, AlarmService.class).setAction(AlarmService.START).putExtra("title", in.getStringExtra("title"));
        c.startForegroundService(s);
    }
}
