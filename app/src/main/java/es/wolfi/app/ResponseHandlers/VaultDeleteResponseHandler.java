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
import android.os.Handler;
import android.os.Looper;
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
import es.wolfi.utils.ProgressUtils;

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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
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
                                ProgressUtils.dismiss(progress);
                                fragmentManager.popBackStack();
                            }
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                alreadySaving.set(false);
                ProgressUtils.dismiss(progress);
                Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
            }
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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
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
                    Log.e("async http response", finalResponse);
                    Toast.makeText(view.getContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
