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

package es.wolfi.passman.API;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import es.wolfi.app.ResponseHandlers.CoreAPIGETResponseHandler;
import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.OfflineStorageValues;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;

public abstract class Core {
    protected static final String LOG_TAG = "API_LIB";

    protected static String host;
    protected static String host_internal;
    protected static String username;
    protected static String password;
    protected static String version_name;
    protected static int version_number = 0;


    public static void setUpAPI(String host, String username, String password) {
        Core.setAPIHost(host);
        Core.username = username;
        Core.password = password;
    }

    public static String getAPIHost() {
        return host;
    }

    public static void setAPIHost(String host) {
        Core.host = host.concat("/index.php/apps/passman/api/v2/");
        Core.host_internal = host.concat("/index.php/apps/passman/api/internal/");
    }

    public static String getAPIUsername() {
        return username;
    }

    public static void setAPIUsername(String username) {
        Core.username = username;
    }

    public static String getAPIPassword() {
        return password;
    }

    public static void setAPIPassword(String password) {
        Core.password = password;
    }

    public static int getConnectTimeout(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getInt(SettingValues.REQUEST_CONNECT_TIMEOUT.toString(), 15) * 1000;
    }

    public static int getConnectRetries(Context c) {
        return 0;
    }

    public static int getResponseTimeout(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getInt(SettingValues.REQUEST_RESPONSE_TIMEOUT.toString(), 120) * 1000;
    }

    public static void requestInternalAPIGET(Context c, String endpoint, final FutureCallback<String> callback) {
        final AsyncHttpResponseHandler responseHandler = new CoreAPIGETResponseHandler(callback);
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(username, password);
        client.setConnectTimeout(getConnectTimeout(c));
        client.setResponseTimeout(getResponseTimeout(c));
        client.setMaxRetriesAndTimeout(getConnectRetries(c), getConnectTimeout(c));
        client.addHeader("Content-Type", "application/json");
        client.get(host_internal.concat(endpoint), responseHandler);
    }

    public static void requestAPIGET(Context c, String endpoint, final FutureCallback<String> callback) {
        final AsyncHttpResponseHandler responseHandler = new CoreAPIGETResponseHandler(callback);
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(username, password);
        client.setConnectTimeout(getConnectTimeout(c));
        client.setResponseTimeout(getResponseTimeout(c));
        client.setMaxRetriesAndTimeout(getConnectRetries(c), getConnectTimeout(c));
        client.addHeader("Content-Type", "application/json");
        client.get(host.concat(endpoint), responseHandler);
    }

    public static void requestAPI(Context c, String endpoint, RequestParams postDataParams, String requestType, final AsyncHttpResponseHandler responseHandler)
            throws MalformedURLException {

        URL url = new URL(host.concat(endpoint));

        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(username, password);
        client.setConnectTimeout(getConnectTimeout(c));
        client.setResponseTimeout(getResponseTimeout(c));
        client.setMaxRetriesAndTimeout(getConnectRetries(c), getConnectTimeout(c));
        //client.addHeader("Content-Type", "application/json; utf-8");
        client.addHeader("Accept", "application/json, text/plain, */*");


        if (requestType.equals("POST")) {
            client.post(url.toString(), postDataParams, responseHandler);
        } else if (requestType.equals("PATCH")) {
            client.patch(url.toString(), postDataParams, responseHandler);
        } else if (requestType.equals("DELETE")) {
            client.delete(url.toString(), postDataParams, responseHandler);
        }
    }

    // TODO Test this method once the server response works!
    public static void getAPIVersion(final Context c, FutureCallback<String> cb) {
        if (version_name != null) {
            cb.onCompleted(null, version_name);
            return;
        }

        requestInternalAPIGET(c, "version", new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (result != null) {
                    Log.d("getApiVersion", result);
                    if (applyVersionJSON(result)) {
                        OfflineStorage.getInstance().putObject(OfflineStorageValues.VERSION.toString(), result);
                        cb.onCompleted(null, version_name);
                    }
                } else {
                    Log.d("getApiVersion", "Failure while getting api version, maybe offline?");
                    if (OfflineStorage.getInstance().isEnabled() && OfflineStorage.getInstance().has(OfflineStorageValues.VERSION.toString())) {
                        if (applyVersionJSON(OfflineStorage.getInstance().getString(OfflineStorageValues.VERSION.toString()))) {
                            cb.onCompleted(null, version_name);
                            return;
                        }
                    }
                    cb.onCompleted(e, null);
                }
            }
        });
    }

    public static boolean applyVersionJSON(String version) {
        try {
            JSONObject parsedResult = new JSONObject(version);
            if (parsedResult.has("version")) {
                version_name = parsedResult.getString("version");
                version_number = Integer.parseInt(version_name.replace(".", ""));
                return true;
            }
        } catch (JSONException | NumberFormatException jsonException) {
            jsonException.printStackTrace();
        }
        return false;
    }

    /**
     * Check if the user has logged in, also sets up the API
     *
     * @param c     The context where this should be run
     * @param toast Whether we want or not a toast! Yum!
     * @param cb    The callback
     */
    public static void checkLogin(final Context c, final boolean toast, final FutureCallback<Boolean> cb) {
        SingleTon ton = SingleTon.getTon();

        if (ton.getString(SettingValues.HOST.toString()) == null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
            String url = settings.getString(SettingValues.HOST.toString(), null);

            // If the url is null app has not yet been configured!
            if (url == null) {
                cb.onCompleted(null, false);
                return;
            }

            // Load the server settings
            ton.addString(SettingValues.HOST.toString(), url);
            ton.addString(SettingValues.USER.toString(), settings.getString(SettingValues.USER.toString(), ""));
            ton.addString(SettingValues.PASSWORD.toString(), settings.getString(SettingValues.PASSWORD.toString(), ""));
        }

        String host = ton.getString(SettingValues.HOST.toString());
        String user = ton.getString(SettingValues.USER.toString());
        String pass = ton.getString(SettingValues.PASSWORD.toString());
        Toast.makeText(c, host, Toast.LENGTH_LONG).show();
        Log.d(LOG_TAG, "Host: " + host);
        Log.d(LOG_TAG, "User: " + user);
        //Log.d(LOG_TAG, "Pass: " + pass);
        Log.d(LOG_TAG, "Pass: " + pass.replaceAll("(?s).", "*"));

        setUpAPI(host, user, pass);
        getAPIVersion(c, new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                boolean ret = true;

                if (e != null) {
                    if (e.getMessage().equals("401")) {
                        if (toast) {
                            Toast.makeText(c, c.getString(R.string.wrongNCSettings), Toast.LENGTH_LONG).show();
                        }
                        ret = false;
                    } else if (e.getMessage().contains("Unable to resolve host") || e.getMessage().contains("Invalid URI")) {
                        if (toast) {
                            Toast.makeText(c, c.getString(R.string.wrongNCUrl), Toast.LENGTH_LONG).show();
                        }
                        ret = false;
                    } else {
                        Log.e(LOG_TAG, "Error: " + e.getMessage(), e);
                        if (toast) {
                            Toast.makeText(c, c.getString(R.string.net_error) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        ret = false;
                    }
                }

                cb.onCompleted(e, ret);
            }
        });
    }
}
