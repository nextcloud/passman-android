package es.wolfi.app.passman;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsCache {
    protected static SharedPreferences sharedPreferences = null;
    protected static JSONObject cache = new JSONObject();

    public void loadSharedPreferences(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    public static void clear() {
        cache = new JSONObject();
    }

    public static String getString(String key, String fallback) {
        try {
            if (!cache.has(key)) {
                cache.put(key, sharedPreferences.getString(key, fallback));
            }

            return cache.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return fallback;
    }

    public static int getInt(String key, int fallback) {
        try {
            if (!cache.has(key)) {
                cache.put(key, sharedPreferences.getInt(key, fallback));
            }

            return cache.getInt(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return fallback;
    }
}
