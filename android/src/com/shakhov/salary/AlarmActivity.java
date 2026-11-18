package com.shakhov.salary;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Окно будильника поверх блокировки. */
public class AlarmActivity extends Activity {
    static final String CLOSE = "com.shakhov.salary.CLOSE_ALARM";
    private BroadcastReceiver closer;

    static final class Closer extends BroadcastReceiver {
        private final Activity a;
        Closer(Activity a) { this.a = a; }
        @Override public void onReceive(Context c, Intent i) { a.finish(); }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String title = getIntent().getStringExtra("title");
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0B1120"));
        root.setPadding((int) (24 * dp), (int) (24 * dp), (int) (24 * dp), (int) (24 * dp));

        TextView time = new TextView(this);
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        time.setTextColor(Color.WHITE); time.setTextSize(72); time.setGravity(Gravity.CENTER);
        TextView cap = new TextView(this);
        cap.setText("⏰ Подъём на смену"); cap.setTextColor(Color.parseColor("#94A3B8")); cap.setTextSize(20); cap.setGravity(Gravity.CENTER);
        TextView t = new TextView(this);
        t.setText(title == null ? "" : title); t.setTextColor(Color.parseColor("#34D399")); t.setTextSize(24); t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (8 * dp), 0, (int) (40 * dp));
        root.addView(time); root.addView(cap); root.addView(t);

        root.addView(button("Выключить", "#059669", AlarmService.STOP, dp));
        root.addView(button("Отложить на 10 минут", "#334155", AlarmService.SNOOZE, dp));
        setContentView(root);
        closer = new Closer(this);
        IntentFilter f = new IntentFilter(CLOSE);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(closer, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(closer, f);
    }

    private Button button(String text, String color, String action, float dp) {
        Button b = new Button(this);
        b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(20); b.setAllCaps(false);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(16 * dp);
        b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (64 * dp));
        lp.topMargin = (int) (12 * dp);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> { startService(new Intent(this, AlarmService.class).setAction(action)); finish(); });
        return b;
    }

    @Override public void onBackPressed() { /* не закрывать случайно — только кнопками */ }

    @Override protected void onDestroy() {
        try { unregisterReceiver(closer); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
