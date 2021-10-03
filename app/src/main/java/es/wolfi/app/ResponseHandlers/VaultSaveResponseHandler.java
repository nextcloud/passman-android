package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;

public class VaultSaveResponseHandler extends AsyncHttpResponseHandler {

    private final AtomicBoolean alreadySaving;
    private final boolean updateVault;
    private final Vault vault;
    private final int keyStrength;
    private final ProgressDialog progress;
    private final View view;
    private final PasswordListActivity passwordListActivity;
    private final FragmentManager fragmentManager;

    public VaultSaveResponseHandler(AtomicBoolean alreadySaving, boolean updateVault, Vault vault, int keyStrength, ProgressDialog progress, View view, PasswordListActivity passwordListActivity, FragmentManager fragmentManager) {
        super();

        this.alreadySaving = alreadySaving;
        this.updateVault = updateVault;
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
                if (updateVault) {
                    alreadySaving.set(false);
                    progress.dismiss();
                    fragmentManager.popBackStack();
                } else {
                    JSONObject vaultObject = new JSONObject(result);
                    Vault v = Vault.fromJSON(vaultObject);
                    if (vaultObject.has("vault_id") && vaultObject.has("name") && vaultObject.getString("name").equals(vault.getName())) {
                        v.setEncryptionKey(vault.getEncryptionKey());

                        Toast.makeText(view.getContext(), "Vault created", Toast.LENGTH_LONG).show();

                        //create test credential
                        Credential testCred = new Credential();
                        testCred.setVault(v);

                        testCred.setLabel("Test key for vault " + v.getName());
                        testCred.setPassword("lorem ipsum");
                        testCred.setOtp("{}");
                        testCred.setTags("");
                        testCred.setFavicon("");
                        testCred.setUsername("");
                        testCred.setEmail("");
                        testCred.setUrl("");
                        testCred.setDescription("");
                        testCred.setFiles("[]");
                        testCred.setCustomFields("[]");
                        testCred.setCompromised(false);
                        testCred.setHidden(true);

                        final AsyncHttpResponseHandler responseHandler = new CredentialSaveForNewVaultResponseHandler(alreadySaving, v, keyStrength, progress, view, passwordListActivity, fragmentManager);
                        testCred.save(view.getContext(), responseHandler);

                        return;
                    }
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
