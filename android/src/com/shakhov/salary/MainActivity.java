package com.shakhov.salary;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Калькулятор зарплаты (сайт на GitHub) внутри приложения + мост для будильников и виджета. */
public class MainActivity extends Activity {
    static final String URL = "https://shakhovalexey.github.io/salary/";
    private WebView web;
    private ValueCallback<Uri[]> fileCb;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF0B1120);
        getWindow().setNavigationBarColor(0xFF0B1120);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B1120);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith(URL)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                try { startActivityForResult(Intent.createChooser(i, "Выбери файл"), 42); } catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });
        if (b != null) web.restoreState(b); else web.loadUrl(URL);
        setContentView(web);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == 42 && fileCb != null) {
            Uri[] r = null;
            if (res == RESULT_OK && data != null && data.getData() != null) r = new Uri[]{data.getData()};
            fileCb.onReceiveValue(r);
            fileCb = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }

    /** Вызывается из JS: window.Android.* */
    class Bridge {
        @JavascriptInterface public boolean isApp() { return true; }

        /** {on, alarms:[{at,title}], shifts:[{start,end,label,alarm}], net} */
        @JavascriptInterface public void sync(String json) {
            Store.save(MainActivity.this, json);
            Alarms.reschedule(MainActivity.this);
            ShiftWidget.updateAll(MainActivity.this);
        }

        @JavascriptInterface public int scheduledCount() { return Store.getCount(MainActivity.this); }

        @JavascriptInterface public void testAlarm() {
            Alarms.snooze(MainActivity.this, "Проверка будильника", 1);
            toast("Будильник прозвонит через 1 минуту");
        }

        @JavascriptInterface public String saveFile(String name, String text) {
            if (Build.VERSION.SDK_INT < 29) { shareText(name, text); return "shared"; }
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                try (OutputStream o = getContentResolver().openOutputStream(u)) { o.write(text.getBytes(StandardCharsets.UTF_8)); }
                toast("Сохранено в «Загрузки»: " + name);
                return "ok";
            } catch (Exception e) { toast("Не удалось сохранить: " + e.getMessage()); return "error"; }
        }

        @JavascriptInterface public void shareText(String title, String text) {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, title);
            startActivity(Intent.createChooser(i, title));
        }

        /** Права для будильника на Xiaomi: уведомления, полноэкранные окна, настройки приложения. */
        @JavascriptInterface public String permState() {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            boolean notif = nm.areNotificationsEnabled();
            boolean fsi = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent();
            return "{\"notif\":" + notif + ",\"fsi\":" + fsi + "}";
        }

        @JavascriptInterface public void openAppSettings() {
            runOnUiThread(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if (!nm.canUseFullScreenIntent()) {
                            startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + getPackageName())));
                            return;
                        }
                    }
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) { toast("Открой: Настройки → Приложения → Зарплата"); }
            });
        }

        private void toast(String t) { runOnUiThread(() -> Toast.makeText(MainActivity.this, t, Toast.LENGTH_LONG).show()); }
    }
}
