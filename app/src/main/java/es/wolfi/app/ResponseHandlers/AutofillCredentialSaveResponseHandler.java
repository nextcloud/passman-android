package es.wolfi.app.ResponseHandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;

public class AutofillCredentialSaveResponseHandler extends AsyncHttpResponseHandler {

    private final Vault vault;
    private final Context baseContext;
    private final Context applicationContext;
    private final SingleTon ton;
    private final String TAG;

    public AutofillCredentialSaveResponseHandler(Vault vault, Context baseContext, Context applicationContext, SingleTon ton, String TAG) {
        super();

        this.vault = vault;
        this.baseContext = baseContext;
        this.applicationContext = applicationContext;
        this.ton = ton;
        this.TAG = TAG;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        String result = new String(responseBody);
        if (statusCode == 200) {
            try {
                JSONObject credentialObject = new JSONObject(result);

                if (credentialObject.has("credential_id") && credentialObject.getInt("vault_id") == vault.vault_id) {
                    Credential currentCredential = Credential.fromJSON(credentialObject, vault);
                    vault.addCredential(currentCredential);
                    ((HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString())).put(vault.guid, vault);
                    Vault activeVault = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
                    if (vault.guid.equals(activeVault.guid)) {
                        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), vault);
                    }

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(baseContext);
                    Vault.updateAutofillVault(vault, settings);

                    Toast.makeText(applicationContext, applicationContext.getString(R.string.successfully_saved), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, applicationContext.getString(R.string.successfully_saved));
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast.makeText(applicationContext, applicationContext.getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onSaveRequest(), failed to save: " + R.string.error_occurred);
        }
    }

    @Override
    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
        String response = "";

        if (responseBody != null && responseBody.length > 0) {
            response = new String(responseBody);
        }

        if (!response.equals("") && JSONUtils.isJSONObject(response)) {
            try {
                JSONObject o = new JSONObject(response);
                if (o.has("message") && o.getString("message").equals("Current user is not logged in")) {

                    Toast.makeText(applicationContext, o.getString("message"), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onSaveRequest(), failed to save: " + o.getString("message"));
                    return;
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        if (error != null && error.getMessage() != null && statusCode != 302) {
            error.printStackTrace();
            Log.e("async http response", response);
            Toast.makeText(applicationContext, error.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, error.getMessage());
        } else {
            Toast.makeText(applicationContext, applicationContext.getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
            Log.d(TAG, applicationContext.getString(R.string.error_occurred));
        }
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
