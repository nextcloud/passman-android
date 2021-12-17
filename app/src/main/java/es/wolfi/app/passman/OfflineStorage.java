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
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import es.wolfi.utils.FileUtils;
import es.wolfi.utils.KeyStoreUtils;

public class OfflineStorage {
    /**
     * Use OfflineStorage.getInstance().? instead of direct calls!
     */

    protected static OfflineStorage offlineStorage;
    private JSONObject storage;
    public final static String LOG_TAG = "OfflineStorage";
    public final static String EMPTY_STORAGE_STRING = "{}";

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

    public static OfflineStorage getInstance() {
        return offlineStorage;
    }

    public void commit() {
        if (isEnabled() && storage != null) {
            KeyStoreUtils.putStringAndCommit(SettingValues.OFFLINE_STORAGE.toString(), storage.toString());
            Log.d(LOG_TAG, "committed");
        }
    }

    public void clear() {
        storage = new JSONObject();
        KeyStoreUtils.putStringAndCommit(SettingValues.OFFLINE_STORAGE.toString(), storage.toString());
        Log.d(LOG_TAG, "cleared and committed");
    }

    public boolean isEnabled() {
        return SettingsCache.getBoolean(SettingValues.ENABLE_OFFLINE_CACHE.toString(), true);
    }

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
