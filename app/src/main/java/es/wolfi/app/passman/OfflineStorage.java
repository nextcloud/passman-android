/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import es.wolfi.utils.FileUtils;
import es.wolfi.utils.KeyStoreUtils;

/**
 * The OfflineStorage class can be used to store data completely encrypted in Androids SharedPreferences.
 * This class uses KeyStoreUtils to encrypt its data.
 * <p>
 * <b>Use OfflineStorage.getInstance().? instead of direct calls!</b>
 * <p>
 * It has getter and setter methods to store and fetch data of type string/object and int.
 * <p>
 * It works like a "full" layer between the Androids SharedPreferences storage engine and the running Passman app.
 * It should be used instead of calling SharedPreferences directly.
 */
public class OfflineStorage {

    protected static OfflineStorage offlineStorage;
    private JSONObject storage;
    public final static String LOG_TAG = "OfflineStorage";
    public final static String EMPTY_STORAGE_STRING = "{}";

    /**
     * Call the initialization of the OfflineStorage at the top of each activity you want to use it.
     * Example usage: new OfflineStorage(getBaseContext());
     *
     * @param context - base context
     */
    public OfflineStorage(Context context) {
        try {
            storage = new JSONObject(getOfflineStorageString());
        } catch (JSONException | NullPointerException e) {
            storage = new JSONObject();
        }
        offlineStorage = this;
    }

    private String getOfflineStorageString() {
        return KeyStoreUtils.getString(SettingValues.OFFLINE_STORAGE.toString(), EMPTY_STORAGE_STRING);
    }

    /**
     * Use OfflineStorage.getInstance().? instead of direct calls!
     * Replace ? with any other public method of this class.
     *
     * @return OfflineStorage
     */
    public static OfflineStorage getInstance() {
        return offlineStorage;
    }

    /**
     * Call commit on closing the app to make the changes persistent.
     * Example usage: OfflineStorage.getInstance().commit();
     */
    public void commit() {
        if (isEnabled() && storage != null) {
            KeyStoreUtils.putStringAndCommit(SettingValues.OFFLINE_STORAGE.toString(), storage.toString());
            Log.d(LOG_TAG, "committed");
        }
    }

    /**
     * Clear offline storage persistent.
     * Should be only used if errors occur or in the app settings.
     */
    public void clear() {
        storage = new JSONObject();
        KeyStoreUtils.putStringAndCommit(SettingValues.OFFLINE_STORAGE.toString(), storage.toString());
        Log.d(LOG_TAG, "cleared and committed");
    }

    /**
     * Returns true if the offline storage feature is enabled (in the settings).
     *
     * @return boolean
     */
    public boolean isEnabled() {
        return SettingsCache.getBoolean(SettingValues.ENABLE_OFFLINE_CACHE.toString(), true);
    }

    /**
     * Calculate a human readable output of the current offline storage size.
     *
     * @return String
     */
    public String getSize() {
        int bytes = getOfflineStorageString().getBytes().length;
        if (isEnabled() && bytes <= EMPTY_STORAGE_STRING.length()) {
            bytes = storage.toString().getBytes().length;
        }
        if (bytes >= EMPTY_STORAGE_STRING.length()) {
            bytes -= EMPTY_STORAGE_STRING.length();
        }
        return FileUtils.humanReadableByteCount((Double.valueOf(bytes)).longValue(), true);
    }

    /**
     * Checks if a key is already saved in the offline storage.
     *
     * @param key to check existence for
     * @return boolean
     */
    public boolean has(String key) {
        return storage.has(key);
    }

    /**
     * Stores an Object / String in the offline storage.
     * Example usage: OfflineStorage.getInstance().putObject(key, value);
     *
     * @param key   String
     * @param value Object
     */
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

    /**
     * Returns an already stored Object from the offline storage.
     * Example usage: OfflineStorage.getInstance().getObject(key, defaultObject);
     *
     * @param key           String
     * @param defaultObject Object - fallback if the key or it's value does not exist
     * @return Object
     */
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

    /**
     * Returns an already stored String from the offline storage.
     * Example usage: OfflineStorage.getInstance().getString(key, defaultString);
     *
     * @param key           String
     * @param defaultString String - fallback if the key or it's value does not exist
     * @return String
     */
    public String getString(String key, String defaultString) {
        Log.d(LOG_TAG, "getString " + key);
        try {
            return storage.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultString;
    }

    /**
     * Stores an int in the offline storage.
     * Example usage: OfflineStorage.getInstance().putInt(key, value);
     *
     * @param key   String
     * @param value int
     */
    public void putInt(String key, int value) {
        try {
            storage.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * Returns an already stored int from the offline storage.
     * Example usage: OfflineStorage.getInstance().getInt(key, defaultInt);
     *
     * @param key        String
     * @param defaultInt int - fallback if the key or it's value does not exist
     * @return int
     */
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
