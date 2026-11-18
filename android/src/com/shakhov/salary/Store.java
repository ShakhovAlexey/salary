package com.shakhov.salary;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Данные от веб-приложения: будильники, смены для виджета, нетто. */
final class Store {
    private static SharedPreferences p(Context c) { return c.getSharedPreferences("salary", Context.MODE_PRIVATE); }
    static void save(Context c, String json) { p(c).edit().putString("data", json).apply(); }
    static JSONObject load(Context c) {
        try { return new JSONObject(p(c).getString("data", "{}")); } catch (Exception e) { return new JSONObject(); }
    }
    static int getCount(Context c) { return p(c).getInt("count", 0); }
    static void setCount(Context c, int n) { p(c).edit().putInt("count", n).apply(); }
    static JSONArray arr(JSONObject o, String k) { JSONArray a = o.optJSONArray(k); return a == null ? new JSONArray() : a; }
}
