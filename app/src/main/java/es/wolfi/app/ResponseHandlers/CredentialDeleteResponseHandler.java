package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import es.wolfi.app.passman.PasswordList;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;

public class CredentialDeleteResponseHandler extends AsyncHttpResponseHandler {

    private final AtomicBoolean alreadySaving;
    private final ProgressDialog progress;
    private final View view;
    private final PasswordList passwordListActivity;
    private final FragmentManager fragmentManager;

    public CredentialDeleteResponseHandler(AtomicBoolean alreadySaving, ProgressDialog progress, View view, PasswordList passwordListActivity, FragmentManager fragmentManager) {
        super();

        this.alreadySaving = alreadySaving;
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
                Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
                if (credentialObject.has("credential_id") && credentialObject.getInt("vault_id") == v.vault_id) {
                    Credential currentCredential = Credential.fromJSON(credentialObject, v);

                    Snackbar.make(view, R.string.successfully_deleted, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    Objects.requireNonNull(passwordListActivity).deleteCredentialInCurrentLocalVaultList(currentCredential);
                    Objects.requireNonNull(passwordListActivity).showAddCredentialsButton();
                    Objects.requireNonNull(passwordListActivity).showLockVaultButton();

                    int backStackCount = fragmentManager.getBackStackEntryCount();
                    int backStackId = fragmentManager.getBackStackEntryAt(backStackCount - 2).getId();
                    alreadySaving.set(false);
                    fragmentManager.popBackStack(backStackId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        alreadySaving.set(false);
        progress.dismiss();
        Snackbar.make(view, R.string.error_occurred, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
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
                    Snackbar.make(view, o.getString("message"), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        if (error != null && error.getMessage() != null) {
            error.printStackTrace();
            Log.e("async http response", new String(responseBody));
            Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            Snackbar.make(view, R.string.error_occurred, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
