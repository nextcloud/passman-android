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
        }
        callback.onCompleted(new Exception(errorMessage), null);
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
