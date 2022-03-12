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

package es.wolfi.app.ResponseHandlers;

import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import es.wolfi.utils.JSONUtils;

public class CoreAPIGETResponseHandler extends AsyncHttpResponseHandler {

    private final FutureCallback<String> callback;

    public CoreAPIGETResponseHandler(final FutureCallback<String> callback) {
        super();

        this.callback = callback;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        String result = new String(responseBody);
        if (statusCode == 200) {
            if (JSONUtils.isJSONObject(result)) {
                try {
                    JSONObject o = new JSONObject(result);
                    if (o.has("message") && o.getString("message").equals("Current user is not logged in")) {
                        callback.onCompleted(new Exception("401"), null);
                        return;
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        }
        callback.onCompleted(null, result);
    }

    @Override
    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
        String errorMessage = error.getMessage();
        if (errorMessage == null) {
            error.printStackTrace();
            errorMessage = "Unknown error";
        }
        if (statusCode == 401) {
            callback.onCompleted(new Exception("401"), null);
        } else {
            callback.onCompleted(new Exception(errorMessage), null);
        }
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
