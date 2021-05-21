/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

package es.wolfi.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import es.wolfi.app.passman.CustomFieldEditAdapter;
import es.wolfi.app.passman.FileEditAdapter;
import es.wolfi.app.passman.PasswordList;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;
import es.wolfi.passman.API.Vault;

public class ResponseHandlerManager {

    /**
     * Creates and starts a unified progress dialog
     *
     * @param context App context from view
     * @return ProgressDialog required to dismiss the dialog
     */
    public static ProgressDialog showLoadingSequence(Context context) {
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle(context.getString(R.string.loading));
        progress.setMessage(context.getString(R.string.wait_while_loading));
        progress.setCancelable(false);
        progress.show();

        return progress;
    }

    public static AsyncHttpResponseHandler getCredentialAddResponseHandler(ProgressDialog progress, AtomicBoolean alreadySaving, View view, PasswordList passwordListActivity, FragmentManager fragmentManager) {
        return new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                String result = new String(responseBody);
                if (statusCode == 200 && !result.equals("")) {
                    try {
                        JSONObject credentialObject = new JSONObject(result);
                        Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
                        if (credentialObject.has("credential_id") && credentialObject.getInt("vault_id") == v.vault_id) {
                            Credential currentCredential = Credential.fromJSON(credentialObject, v);

                            Snackbar.make(view, R.string.successfully_saved, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();

                            Objects.requireNonNull(passwordListActivity).addCredentialToCurrentLocalVaultList(currentCredential);
                            Objects.requireNonNull(passwordListActivity).showAddCredentialsButton();
                            alreadySaving.set(false);
                            progress.dismiss();
                            fragmentManager.popBackStack();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    alreadySaving.set(false);
                    progress.dismiss();
                    Snackbar.make(view, R.string.error_occurred, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
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
        };
    }

    public static AsyncHttpResponseHandler getCredentialAddFileResponseHandler(ProgressDialog progress, String fileName, int requestCode, FileEditAdapter fed, CustomFieldEditAdapter cfed) {
        return new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                String result = new String(responseBody);
                if (statusCode == 200 && !result.equals("")) {
                    try {
                        JSONObject fileObject = new JSONObject(result);
                        if (fileObject.has("message") && fileObject.getString("message").equals("Current user is not logged in")) {
                            return;
                        }
                        if (fileObject.has("file_id") && fileObject.has("filename") && fileObject.has("guid")
                                && fileObject.has("size") && fileObject.has("created") && fileObject.has("mimetype")) {
                            fileObject.put("filename", fileName);
                            File file = new File(fileObject);

                            if (requestCode == FileUtils.activityRequestFileCodes.get("credentialAddFile")) {
                                fed.addFile(file);
                                fed.notifyDataSetChanged();
                            }
                            if (requestCode == FileUtils.activityRequestFileCodes.get("credentialAddCustomFieldFile")) {
                                CustomField cf = new CustomField();
                                cf.setLabel("newLabel" + cfed.getItemCount() + 1);
                                cf.setSecret(false);
                                cf.setFieldType("file");
                                cf.setJValue(file.getAsJSONObject());

                                cfed.addCustomField(cf);
                                cfed.notifyDataSetChanged();
                            }
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
                progress.dismiss();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
                progress.dismiss();
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        };
    }
}
