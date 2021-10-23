package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;

public class VaultDeleteResponseHandler extends AsyncHttpResponseHandler {

    private final AtomicBoolean alreadySaving;
    private final Vault vault;
    private final boolean isDeleteVaultContentRequest;
    private final ProgressDialog progress;
    private final View view;
    private final PasswordListActivity passwordListActivity;
    private final FragmentManager fragmentManager;

    public VaultDeleteResponseHandler(AtomicBoolean alreadySaving, Vault vault, boolean isDeleteVaultContentRequest, ProgressDialog progress, View view, PasswordListActivity passwordListActivity, FragmentManager fragmentManager) {
        super();

        this.alreadySaving = alreadySaving;
        this.vault = vault;
        this.isDeleteVaultContentRequest = isDeleteVaultContentRequest;
        this.progress = progress;
        this.view = view;
        this.passwordListActivity = passwordListActivity;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        String result = new String(responseBody);
        if (statusCode == 200) {
            try {
                JSONObject responseObject = new JSONObject(result);
                if (responseObject.has("ok") && responseObject.getBoolean("ok")) {
                    if (isDeleteVaultContentRequest) {
                        final AsyncHttpResponseHandler responseHandler = new VaultDeleteResponseHandler(alreadySaving, vault, false, progress, view, passwordListActivity, fragmentManager);
                        vault.delete(view.getContext(), responseHandler);
                    } else {
                        Toast.makeText(view.getContext(), R.string.successfully_deleted, Toast.LENGTH_LONG).show();

                        Objects.requireNonNull(passwordListActivity).deleteVaultInCurrentLocalVaultList(vault);

                        alreadySaving.set(false);
                        progress.dismiss();
                        fragmentManager.popBackStack();
                    }
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        alreadySaving.set(false);
        progress.dismiss();
        Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
        alreadySaving.set(false);
        progress.dismiss();
        String response = "";

        if (responseBody != null && responseBody.length > 0) {
            response = new String(responseBody);
        }

        if (!response.equals("") && JSONUtils.isJSONObject(response)) {
            try {
                JSONObject o = new JSONObject(response);
                if (o.has("message") && o.getString("message").equals("Current user is not logged in")) {
                    Toast.makeText(view.getContext(), o.getString("message"), Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        if (error != null && error.getMessage() != null && statusCode != 302) {
            error.printStackTrace();
            Log.e("async http response", response);
            Toast.makeText(view.getContext(), error.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
