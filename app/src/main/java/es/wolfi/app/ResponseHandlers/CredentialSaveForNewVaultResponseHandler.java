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

import cz.msebera.android.httpclient.Header;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;

public class CredentialSaveForNewVaultResponseHandler extends AsyncHttpResponseHandler {

    private final AtomicBoolean alreadySaving;
    private final Vault vault;
    private final int keyStrength;
    private final ProgressDialog progress;
    private final View view;
    private final PasswordListActivity passwordListActivity;
    private final FragmentManager fragmentManager;

    public CredentialSaveForNewVaultResponseHandler(AtomicBoolean alreadySaving, Vault vault, int keyStrength, ProgressDialog progress, View view, PasswordListActivity passwordListActivity, FragmentManager fragmentManager) {
        super();

        this.alreadySaving = alreadySaving;
        this.vault = vault;
        this.keyStrength = keyStrength;
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
                JSONObject credentialObject = new JSONObject(result);
                if (credentialObject.has("credential_id") && credentialObject.getInt("vault_id") == vault.vault_id) {

                    AsyncHttpResponseHandler createInitialSharingKeysResponseHandler = new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            if (statusCode == 200) {
                                Toast.makeText(view.getContext(), R.string.successfully_saved, Toast.LENGTH_LONG).show();
                                Objects.requireNonNull(passwordListActivity).addVaultToCurrentLocalVaultList(vault);
                                fragmentManager.popBackStack();
                            } else {
                                Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
                            }

                            alreadySaving.set(false);
                            progress.dismiss();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                        }
                    };

                    //create initial sharing keys
                    vault.updateSharingKeys(keyStrength, view.getContext(), createInitialSharingKeysResponseHandler);

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
        String response = new String(responseBody);

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
            Log.e("async http response", new String(responseBody));
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
