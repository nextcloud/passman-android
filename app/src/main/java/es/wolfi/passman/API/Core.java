/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2022, Timo Triebensky (timo@binsky.org)
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

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.api.AidlNetworkRequest;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.api.Response;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.ParseException;
import cz.msebera.android.httpclient.entity.StringEntity;
import es.wolfi.app.ResponseHandlers.CoreAPIGETResponseHandler;
import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.OfflineStorageValues;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SettingsCache;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.utils.KeyStoreUtils;

public abstract class Core {
    protected static final String LOG_TAG = "API_LIB";
    protected static final String JSON_CONTENT_TYPE = "application/json";

    protected static SingleSignOnAccount ssoAccount;
    protected static String host;
    protected static String host_internal;
    protected static String username;
    protected static String password;
    protected static String version_name;
    protected static String API_URL = "/index.php/apps/passman/api/v2/";
    protected static String API_URL_INTERNAL = "/index.php/apps/passman/api/internal/";
    protected static int version_number = 0;


    public static void setUpAPI(Context c, String host, String username, String password) {
        Core.setAPIHost(host);
        Core.username = username;
        Core.password = password;

        try {
            Core.ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(c);
        } catch (java.lang.NoSuchMethodError |
                NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
            e.printStackTrace();
        }
    }

    public static String getAPIHost() {
        return host;
    }

    public static void setAPIHost(String host) {
        Core.host = host.concat(API_URL);
        Core.host_internal = host.concat(API_URL_INTERNAL);
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
        return SettingsCache.getInt(SettingValues.REQUEST_CONNECT_TIMEOUT.toString(), 15) * 1000;
    }

    public static int getConnectRetries(Context c) {
        return 0;
    }

    public static int getResponseTimeout(Context c) {
        return SettingsCache.getInt(SettingValues.REQUEST_RESPONSE_TIMEOUT.toString(), 120) * 1000;
    }

    public static void requestInternalAPIGET(Context c, String endpoint, final FutureCallback<String> callback) {
        final AsyncHttpResponseHandler responseHandler = new CoreAPIGETResponseHandler(callback);
        if (ssoAccount != null) {
            final Map<String, List<String>> header = new HashMap<>();
            header.put("Content-Type", Collections.singletonList(JSON_CONTENT_TYPE));

            NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                    .setMethod("GET")
                    .setUrl(Uri.encode(API_URL_INTERNAL.concat(endpoint), "/"))
                    .build();
            new SyncedRequestTask(nextcloudRequest, ssoAccount, responseHandler, c).execute();
        } else {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(username, password);
            client.setConnectTimeout(getConnectTimeout(c));
            client.setResponseTimeout(getResponseTimeout(c));
            client.setMaxRetriesAndTimeout(getConnectRetries(c), getConnectTimeout(c));
            client.addHeader("Content-Type", JSON_CONTENT_TYPE);

            try {
                client.get(host_internal.concat(endpoint), responseHandler);
            } catch (Exception e) {
                responseHandler.onFailure(0, null, null, e);
            }
        }
    }

    public static void requestAPIGET(Context c, String endpoint, final FutureCallback<String> callback) {
        final AsyncHttpResponseHandler responseHandler = new CoreAPIGETResponseHandler(callback);
        if (ssoAccount != null) {
            final Map<String, List<String>> header = new HashMap<>();
            header.put("Content-Type", Collections.singletonList(JSON_CONTENT_TYPE));

            NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                    .setMethod("GET")
                    .setUrl(Uri.encode(API_URL.concat(endpoint), "/"))
                    .build();
            new SyncedRequestTask(nextcloudRequest, ssoAccount, responseHandler, c).execute();
        } else {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(username, password);
            client.setConnectTimeout(getConnectTimeout(c));
            client.setResponseTimeout(getResponseTimeout(c));
            client.setMaxRetriesAndTimeout(getConnectRetries(c), getConnectTimeout(c));
            client.addHeader("Content-Type", JSON_CONTENT_TYPE);

            try {
                client.get(host.concat(endpoint), responseHandler);
            } catch (Exception e) {
                responseHandler.onFailure(0, null, null, e);
            }
        }
    }

    public static void requestAPI(Context c, String endpoint, JSONObject jsonPostData, String requestType, final AsyncHttpResponseHandler responseHandler)
            throws MalformedURLException, UnsupportedEncodingException {

        if (ssoAccount != null) {
            final Map<String, List<String>> header = new HashMap<>();
            header.put("Accept", Collections.singletonList("application/json, text/plain, */*"));
            //header.put("Content-Type", Collections.singletonList(JSON_CONTENT_TYPE));

            NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                    .setMethod(requestType)
                    .setUrl(Uri.encode(API_URL.concat(endpoint), "/"))
                    .setRequestBody(jsonPostData.toString())
                    .setHeader(header)
                    .build();

            new SyncedRequestTask(nextcloudRequest, ssoAccount, responseHandler, c).execute();
        } else {
            URL url = new URL(host.concat(endpoint));
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(username, password);
            client.setConnectTimeout(getConnectTimeout(c));
            client.setResponseTimeout(getResponseTimeout(c));
            client.setMaxRetriesAndTimeout(getConnectRetries(c), getConnectTimeout(c));
            client.addHeader("Accept", "application/json, text/plain, */*");

            StringEntity entity = new StringEntity(jsonPostData.toString());

            try {
                if (requestType.equals("POST")) {
                    client.post(c, url.toString(), entity, JSON_CONTENT_TYPE, responseHandler);
                } else if (requestType.equals("PATCH")) {
                    client.patch(c, url.toString(), entity, JSON_CONTENT_TYPE, responseHandler);
                } else if (requestType.equals("DELETE")) {
                    client.delete(c, url.toString(), entity, JSON_CONTENT_TYPE, responseHandler);
                }
            } catch (Exception e) {
                responseHandler.onFailure(0, null, null, e);
            }
        }
    }

    public static void getAPIVersion(final Context c, FutureCallback<String> cb) {
        if (version_name != null) {
            cb.onCompleted(null, version_name);
            return;
        }

        requestInternalAPIGET(c, "version", new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                try {
                    if (result != null && e == null) {
                        Log.d("getApiVersion", result);
                        if (applyVersionJSON(result)) {
                            OfflineStorage.getInstance().putObject(OfflineStorageValues.VERSION.toString(), result);
                            OfflineStorage.getInstance().commit();
                            cb.onCompleted(null, version_name);
                        }
                    } else {
                        Log.d("getApiVersion", "Failure while getting api version, maybe offline?");
                        Log.d("OfflineStorage state", OfflineStorage.getInstance().isEnabled() ? "enabled" : "disabled");
                        Log.d("version stored", OfflineStorage.getInstance().has(OfflineStorageValues.VERSION.toString()) ? "yes" : "no");
                        if (OfflineStorage.getInstance().isEnabled() &&
                                OfflineStorage.getInstance().has(OfflineStorageValues.VERSION.toString()) &&
                                OfflineStorage.getInstance().has(OfflineStorageValues.VAULTS.toString())) {
                            showConnectionErrorHint(c);
                            if (applyVersionJSON(OfflineStorage.getInstance().getString(OfflineStorageValues.VERSION.toString()))) {
                                cb.onCompleted(null, version_name);
                                return;
                            }
                        }
                        cb.onCompleted(e, null);
                    }
                } catch (JSONException | NumberFormatException jsonException) {
                    jsonException.printStackTrace();
                    Toast.makeText(c, c.getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
                    cb.onCompleted(jsonException, null);
                }
            }
        });
    }

    public static boolean applyVersionJSON(String version) throws JSONException, NumberFormatException {
        JSONObject parsedResult = new JSONObject(version);
        if (parsedResult.has("version")) {
            version_name = parsedResult.getString("version");
            version_number = Integer.parseInt(version_name.replace(".", ""));
            return true;
        }
        return false;
    }

    public static void checkCloudConnectionAndShowHint(View view) {
        requestInternalAPIGET(view.getContext(), "version", new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (result == null) {
                    showConnectionErrorHint(view.getContext());
                }
            }
        });
    }

    private static void showConnectionErrorHint(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.net_error_dialog_description);
        builder.setCancelable(true);
        builder.show();
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
            String url = KeyStoreUtils.getString(SettingValues.HOST.toString(), null);

            // If the url is null app has not yet been configured!
            if (url == null) {
                cb.onCompleted(null, false);
                return;
            }

            // Load the server settings
            ton.addString(SettingValues.HOST.toString(), url);
            ton.addString(SettingValues.USER.toString(), KeyStoreUtils.getString(SettingValues.USER.toString(), ""));
            ton.addString(SettingValues.PASSWORD.toString(), KeyStoreUtils.getString(SettingValues.PASSWORD.toString(), ""));
        }

        String host = ton.getString(SettingValues.HOST.toString());
        String user = ton.getString(SettingValues.USER.toString());
        String pass = ton.getString(SettingValues.PASSWORD.toString());

        Log.d(LOG_TAG, "Host: " + host);
        Log.d(LOG_TAG, "User: " + user);
        //Log.d(LOG_TAG, "Pass: " + pass);
        Log.d(LOG_TAG, "Pass: " + pass.replaceAll("(?s).", "*"));

        setUpAPI(c, host, user, pass);
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
                } else {
                    Toast.makeText(c, host, Toast.LENGTH_LONG).show();
                }

                cb.onCompleted(e, ret);
            }
        });
    }

    private static class NCHeader implements Header {
        String name, value;

        public NCHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public NCHeader(ArrayList<AidlNetworkRequest.PlainHeader> plainHeaders, cz.msebera.android.httpclient.Header[] headers) {
            Iterator<AidlNetworkRequest.PlainHeader> it = plainHeaders.iterator();
            for (int i = 0; it.hasNext(); i++) {
                AidlNetworkRequest.PlainHeader plainHeader = it.next();
                headers[i] = new NCHeader(plainHeader.getName(), plainHeader.getValue());
            }
        }

        @Override
        public HeaderElement[] getElements() throws ParseException {
            return new HeaderElement[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private static class SyncedRequestTask extends AsyncTask<Void, Void, Boolean> {
        private final NextcloudRequest nextcloudRequest;
        private final NextcloudAPI mNextcloudAPI;
        private final AsyncHttpResponseHandler responseHandler;
        private final FutureCallback<String> callback;

        SyncedRequestTask(@NonNull NextcloudRequest nextcloudRequest, @NonNull SingleSignOnAccount ssoAccount, final AsyncHttpResponseHandler responseHandler, Context c) {
            this.nextcloudRequest = nextcloudRequest;
            this.mNextcloudAPI = new NextcloudAPI(c.getApplicationContext(), ssoAccount, new GsonBuilder().create(), apiCallback);
            this.responseHandler = responseHandler;
            this.callback = null;
            Log.d("SyncedRequestTask", ssoAccount.name + " → " + nextcloudRequest.getMethod() + " " + nextcloudRequest.getUrl() + " ");
        }

        SyncedRequestTask(@NonNull NextcloudRequest nextcloudRequest, @NonNull SingleSignOnAccount ssoAccount, final FutureCallback<String> callback, Context c) {
            this.nextcloudRequest = nextcloudRequest;
            this.mNextcloudAPI = new NextcloudAPI(c.getApplicationContext(), ssoAccount, new GsonBuilder().create(), apiCallback);
            this.responseHandler = null;
            this.callback = callback;
            Log.d("SyncedRequestTask", ssoAccount.name + " → " + nextcloudRequest.getMethod() + " " + nextcloudRequest.getUrl() + " ");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Response response = mNextcloudAPI.performNetworkRequestV2(nextcloudRequest);

                StringBuilder textBuilder = new StringBuilder();
                final BufferedReader rd = new BufferedReader(new InputStreamReader(response.getBody()));
                String line;
                while ((line = rd.readLine()) != null) {
                    textBuilder.append(line);
                }
                response.getBody().close();

                cz.msebera.android.httpclient.Header[] headers = new cz.msebera.android.httpclient.Header[response.getPlainHeaders().size()];
                new NCHeader(response.getPlainHeaders(), headers);

                if (responseHandler != null) {
                    responseHandler.onSuccess(200, headers, textBuilder.toString().getBytes(StandardCharsets.UTF_8));
                } else if (callback != null) {
                    callback.onCompleted(null, textBuilder.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("error msg:", e.getMessage());
                if (responseHandler != null) {
                    responseHandler.onFailure(400, null, "".getBytes(), e);
                }
            }

            return true;
        }

        private NextcloudAPI.ApiConnectedListener apiCallback = new NextcloudAPI.ApiConnectedListener() {
            @Override
            public void onConnected() {
                // ignore this one… see 5)
                Log.i("NextcloudAPI", "SSO API connected");
            }

            @Override
            public void onError(Exception ex) {
                Log.i("NextcloudAPI", "SSO API ERROR");
                ex.printStackTrace();
                // TODO handle errors
            }
        };
    }
}
