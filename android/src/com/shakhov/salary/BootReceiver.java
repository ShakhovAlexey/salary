package com.shakhov.salary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки и смены времени будильники ставятся заново. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent in) {
        Alarms.reschedule(c);
        ShiftWidget.updateAll(c);
    }
}
