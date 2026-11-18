package com.shakhov.salary;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Звонит: громкий звук будильника + вибрация + полноэкранное окно. */
public class AlarmService extends Service {
    static final String START = "start", STOP = "stop", SNOOZE = "snooze", CH = "alarm2";
    private MediaPlayer mp;
    private Vibrator vib;
    private PowerManager.WakeLock wl;
    private final Handler h = new Handler(Looper.getMainLooper());
    private String title = "Смена";

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent in, int flags, int id) {
        String a = in == null ? START : in.getAction();
        if (STOP.equals(a)) { finishAll(); return START_NOT_STICKY; }
        if (SNOOZE.equals(a)) { Alarms.snooze(this, title, 10); finishAll(); return START_NOT_STICKY; }
        if (in != null && in.getStringExtra("title") != null) title = in.getStringExtra("title");
        Notification n = build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(7, n);
        ring();
        try { startActivity(new Intent(this, AlarmActivity.class).putExtra("title", title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) {}
        h.postDelayed(this::finishAll, 10 * 60 * 1000L);   // сам замолкает через 10 минут
        return START_NOT_STICKY;
    }

    private PendingIntent svc(String action, int code) {
        return PendingIntent.getService(this, code, new Intent(this, AlarmService.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification build() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CH, "Будильник на смену", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        ch.setBypassDnd(true);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
        PendingIntent full = PendingIntent.getActivity(this, 2, new Intent(this, AlarmActivity.class).putExtra("title", title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ Подъём на смену")
                .setContentText(title)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .addAction(new Notification.Action.Builder(null, "Выключить", svc(STOP, 11)).build())
                .addAction(new Notification.Action.Builder(null, "Через 10 мин", svc(SNOOZE, 12)).build())
                .build();
    }

    private void ring() {
        stopSound();
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "salary:alarm");
            wl.acquire(11 * 60 * 1000L);
        } catch (Exception ignored) {}
        try {
            Uri u = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            mp.setDataSource(this, u);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (Exception ignored) {}
        try {
            vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 800, 600}, 0),
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        } catch (Exception ignored) {}
    }

    private void stopSound() {
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception ignored) {}
        mp = null;
        try { if (vib != null) vib.cancel(); } catch (Exception ignored) {}
    }

    private void finishAll() {
        h.removeCallbacksAndMessages(null);
        stopSound();
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) {}
        sendBroadcast(new Intent(AlarmActivity.CLOSE).setPackage(getPackageName()));
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public void onDestroy() { stopSound(); super.onDestroy(); }
}
