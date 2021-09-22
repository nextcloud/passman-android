/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

public class OfflineStorage {
    /**
     * Use OfflineStorage.getInstance().? instead of direct calls!
     */

    protected static OfflineStorage offlineStorage;
    protected static SharedPreferences sharedPreferences = null;
    private JSONObject storage;
    public final static String LOG_TAG = "OfflineStorage";

    public OfflineStorage(Context context) {
        loadSharedPreferences(context);
        String offlineStorageString = sharedPreferences.getString(SettingValues.OFFLINE_STORAGE.toString(), null);
        try {
            storage = new JSONObject(offlineStorageString);
        } catch (JSONException | NullPointerException e) {
            storage = new JSONObject();
        }
        offlineStorage = this;
    }

    public static OfflineStorage getInstance() {
        return offlineStorage;
    }

    private void loadSharedPreferences(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    public void commit() {
        if (isEnabled() && storage != null) {
            sharedPreferences.edit().putString(SettingValues.OFFLINE_STORAGE.toString(), storage.toString()).commit();
            Log.d(LOG_TAG, "committed");
        }
    }

    public void clear() {
        storage = new JSONObject();
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean has(String name) {
        return storage.has(name);
    }

    public void putObject(String key, Object value) {
        try {
            storage.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Object getObject(String key) {
        return getObject(key, null);
    }

    public Object getObject(String key, Object defaultObject) {
        Log.d(LOG_TAG, "getObject " + key);
        try {
            return storage.get(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultObject;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultString) {
        Log.d(LOG_TAG, "getString " + key);
        try {
            return storage.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultString;
    }

    public void putInt(String key, int value) {
        try {
            storage.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getInt(String key, int defaultInt) {
        Log.d(LOG_TAG, "getInt " + key);
        try {
            return storage.getInt(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultInt;
    }
}
