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
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;
import es.wolfi.utils.ProgressUtils;

public class CredentialSaveResponseHandler extends AsyncHttpResponseHandler {

    private final AtomicBoolean alreadySaving;
    private final boolean updateCredential;
    private final ProgressDialog progress;
    private final View view;
    private final PasswordListActivity passwordListActivity;
    private final FragmentManager fragmentManager;

    public CredentialSaveResponseHandler(AtomicBoolean alreadySaving, boolean updateCredential, ProgressDialog progress, View view, PasswordListActivity passwordListActivity, FragmentManager fragmentManager) {
        super();

        this.alreadySaving = alreadySaving;
        this.updateCredential = updateCredential;
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

                    passwordListActivity.runOnUiThread(() -> {
                        Toast.makeText(view.getContext(), R.string.successfully_saved, Toast.LENGTH_LONG).show();
                    });

                    if (updateCredential) {
                        Objects.requireNonNull(passwordListActivity).editCredentialInCurrentLocalVaultList(currentCredential);
                    } else {
                        Objects.requireNonNull(passwordListActivity).addCredentialToCurrentLocalVaultList(currentCredential);
                    }

                    alreadySaving.set(false);
                    ProgressUtils.dismiss(progress);
                    fragmentManager.popBackStack();
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        alreadySaving.set(false);
        ProgressUtils.dismiss(progress);
        passwordListActivity.runOnUiThread(() -> {
            Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
        alreadySaving.set(false);
        ProgressUtils.dismiss(progress);
        String response = "";

        if (responseBody != null && responseBody.length > 0) {
            response = new String(responseBody);
        }

        final String finalResponse = response;
        passwordListActivity.runOnUiThread(() -> {
            if (!finalResponse.equals("") && JSONUtils.isJSONObject(finalResponse)) {
                try {
                    JSONObject o = new JSONObject(finalResponse);
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
        });
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
