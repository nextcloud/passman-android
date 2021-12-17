/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
 * @license GNU AGPL version 3 or any later version
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.wolfi.app.passman;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * High frequently required SettingValues from SharedPreferences should be requested using the SettingsCache.
 * It returns/caches data directly from SharedPreferences without checking possible encryption on it.
 * <p>
 * Call the initialization of the SettingsCache at the top of each activity you want to use it
 * - SettingsCache().loadSharedPreferences(getBaseContext());
 * <p>
 * The SettingsCache needs to be cleared manually after changing already cached data.
 * SettingsCache.clear(); should only be called after changing settings in SharedPreferences
 * that are accessed through the SettingsCache.
 */
public class SettingsCache {
    protected static SharedPreferences sharedPreferences = null;
    protected static JSONObject cache = new JSONObject();

    public void loadSharedPreferences(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
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

    public static boolean getBoolean(String key, boolean fallback) {
        try {
            if (!cache.has(key)) {
                cache.put(key, sharedPreferences.getBoolean(key, fallback));
            }

            return cache.getBoolean(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return fallback;
    }

    public static void runTimingTest(Context context) {
        SettingsCache.clear();

        long beforeOldMethod = System.nanoTime();
        PreferenceManager.getDefaultSharedPreferences(context).getString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), null);
        long diffOldMethod = System.nanoTime() - beforeOldMethod;

        long beforeNewMethodFirst = System.nanoTime();
        SettingsCache.getString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), null);
        long diffNewMethodFirst = System.nanoTime() - beforeNewMethodFirst;

        long beforeNewMethodSecond = System.nanoTime();
        SettingsCache.getString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), null);
        long diffNewMethodSecond = System.nanoTime() - beforeNewMethodSecond;

        /*
        Log.d("diffOldMethod", String.valueOf(diffOldMethod));
        Log.d("diffNewMethodFirst", String.valueOf(diffNewMethodFirst));
        Log.d("diffNewMethodSecond", String.valueOf(diffNewMethodSecond));
        */

        Log.d("First Cache Call", String.format("Speedup %s", (int) (Math.abs(1 - (double) diffNewMethodFirst / (double) diffOldMethod) * 100)) + "%");
        Log.d("Second Cache Call", String.format("Speedup %s", (int) (Math.abs(1 - (double) diffNewMethodSecond / (double) diffOldMethod) * 100)) + "%");
    }
}
