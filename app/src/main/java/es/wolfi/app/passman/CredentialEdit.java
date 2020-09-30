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

package es.wolfi.app.passman;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialEdit#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialEdit extends Fragment implements View.OnClickListener {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.edit_credential_label_header)
    TextView label_header;
    @BindView(R.id.edit_credential_label)
    EditText label;
    @BindView(R.id.edit_credential_user)
    EditText user;
    @BindView(R.id.edit_credential_password)
    EditText password;
    @BindView(R.id.edit_credential_email)
    EditText email;
    @BindView(R.id.edit_credential_url)
    EditText url;
    @BindView(R.id.edit_credential_description)
    EditText description;
    @BindView(R.id.filesList)
    RecyclerView filesList;
    @BindView(R.id.customFieldsList)
    RecyclerView customFieldsList;
    @BindView(R.id.customFieldType)
    Spinner customFieldType;

    private Credential credential;
    private boolean alreadySaving = false;
    private FileEditAdapter fed;
    private CustomFieldEditAdapter cfed;
    private RecyclerView filesListRecyclerView;
    private RecyclerView customFieldsListRecyclerView;

    public CredentialEdit() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment CredentialEdit.
     */
    public static CredentialEdit newInstance(String credentialGUID) {
        CredentialEdit fragment = new CredentialEdit();

        Bundle b = new Bundle();
        b.putString(CREDENTIAL, credentialGUID);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_credential_edit, container, false);

        Button updateCredentialButton = (Button) view.findViewById(R.id.UpdateCredentialButton);
        updateCredentialButton.setOnClickListener(this);
        updateCredentialButton.setVisibility(View.VISIBLE);

        Button deleteCredentialButton = (Button) view.findViewById(R.id.DeleteCredentialButton);
        deleteCredentialButton.setOnClickListener(this.getDeleteButtonListener());
        deleteCredentialButton.setVisibility(View.VISIBLE);

        Button addFileButton = (Button) view.findViewById(R.id.AddFileButton);
        addFileButton.setOnClickListener(this.getAddFileButtonListener());
        addFileButton.setVisibility(View.VISIBLE);

        Button addCustomFieldButton = (Button) view.findViewById(R.id.AddCustomFieldButton);
        addCustomFieldButton.setOnClickListener(this.getAddCustomFieldButtonListener());
        addCustomFieldButton.setVisibility(View.VISIBLE);

        fed = new FileEditAdapter(credential);
        cfed = new CustomFieldEditAdapter(credential);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.filesList);
        filesListRecyclerView.setHasFixedSize(true);
        filesListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        filesListRecyclerView.setAdapter(fed);

        customFieldsListRecyclerView = (RecyclerView) view.findViewById(R.id.customFieldsList);
        customFieldsListRecyclerView.setHasFixedSize(true);
        customFieldsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        customFieldsListRecyclerView.setAdapter(cfed);

        label.setText(this.credential.getLabel());
        user.setText(this.credential.getUsername());
        password.setText(this.credential.getPassword());
        email.setText(this.credential.getEmail());
        url.setText(this.credential.getUrl());
        description.setText(this.credential.getDescription());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
            credential = v.findCredentialByGUID(getArguments().getString(CREDENTIAL));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void addSelectedFile(String encodedFile, String fileName, String mimeType, int fileSize, int requestCode) throws JSONException {
        Context context = getContext();
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle(context.getString(R.string.loading));
        progress.setMessage(context.getString(R.string.wait_while_loading));
        progress.setCancelable(false);
        progress.show();

        AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
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

                            if (requestCode == 2) {
                                fed.addFile(file);
                                fed.notifyDataSetChanged();
                            }
                            if (requestCode == 3) {
                                JSONObject customFieldJSONObject = new JSONObject();
                                customFieldJSONObject.put("label", "newLabel" + cfed.getItemCount() + 1);
                                customFieldJSONObject.put("secret", false);
                                customFieldJSONObject.put("field_type", "file");
                                customFieldJSONObject.put("value", file.getAsJSONObject());

                                CustomField cf = new CustomField(customFieldJSONObject);
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

        // Start encryption a little later so that the main thread does not get stuck in the file selection dialog and it can close.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                credential.uploadFile(context, encodedFile, fileName, mimeType, fileSize, responseHandler, progress);
            }
        }, 100);
    }

    public View.OnClickListener getAddFileButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((PasswordList) Objects.requireNonNull(getActivity())).selectFileToAdd(2);
            }
        };
    }

    public View.OnClickListener getAddCustomFieldButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (customFieldType.getSelectedItem().toString().equals("File")) {
                    ((PasswordList) Objects.requireNonNull(getActivity())).selectFileToAdd(3);
                } else {
                    try {
                        JSONObject customFieldJSONObject = new JSONObject();
                        customFieldJSONObject.put("label", "newLabel" + (cfed.getItemCount() + 1));
                        customFieldJSONObject.put("secret", customFieldType.getSelectedItem().toString().toLowerCase().equals("password"));
                        customFieldJSONObject.put("field_type", customFieldType.getSelectedItem().toString().toLowerCase());
                        customFieldJSONObject.put("value", "");

                        CustomField cf = new CustomField(customFieldJSONObject);
                        cfed.addCustomField(cf);
                        cfed.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public View.OnClickListener getDeleteButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (alreadySaving) {
                    return;
                }

                AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                        String result = new String(responseBody);
                        if (statusCode == 200 && !result.equals("")) {
                            Snackbar.make(view, R.string.successfully_deleted, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            assert getFragmentManager() != null;
                            alreadySaving = false;

                            Objects.requireNonNull(((PasswordList) getActivity())).refreshVault();
                            Objects.requireNonNull(((PasswordList) getActivity())).showAddCredentialsButton();
                            Objects.requireNonNull(((PasswordList) getActivity())).showLockVaultButton();

                            int backStackCount = getFragmentManager().getBackStackEntryCount();
                            int backStackId = getFragmentManager().getBackStackEntryAt(backStackCount - 2).getId();
                            getFragmentManager().popBackStack(backStackId,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                        alreadySaving = false;
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
                                //ex = e1;
                            }
                        }

                        if (error != null && error.getMessage() != null) {
                            error.printStackTrace();
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

                alreadySaving = true;
                Date date = new Date();
                credential.setDeleteTime(date.getTime());
                credential.update(view.getContext(), responseHandler);
            }
        };
    }

    @Override
    public void onClick(View view) {
        if (alreadySaving) {
            return;
        }

        if (label.getText().toString().equals("")) {
            label_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        }

        Context context = getContext();
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle(context.getString(R.string.loading));
        progress.setMessage(context.getString(R.string.wait_while_loading));
        progress.setCancelable(false);
        progress.show();

        if (!this.credential.getPassword().equals(password.getText().toString())) {
            this.credential.setCompromised(false);
        }

        this.credential.setLabel(label.getText().toString());
        this.credential.setUsername(user.getText().toString());
        this.credential.setPassword(password.getText().toString());
        this.credential.setEmail(email.getText().toString());
        this.credential.setUrl(url.getText().toString());
        this.credential.setDescription(description.getText().toString());
        this.credential.setFiles(fed.getFilesString());
        this.credential.setCustomFields(cfed.getCustomFieldsString());

        alreadySaving = true;

        AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                String result = new String(responseBody);
                if (statusCode == 200 && !result.equals("")) {
                    Snackbar.make(view, R.string.successfully_updated, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    assert getFragmentManager() != null;
                    alreadySaving = false;
                    progress.dismiss();
                    getFragmentManager().popBackStack();
                    Objects.requireNonNull(((PasswordList) getActivity())).refreshVault();
                    Objects.requireNonNull(((PasswordList) getActivity())).showCredentialEditButton();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                alreadySaving = false;
                progress.dismiss();
                if (responseBody != null && responseBody.length > 0) {
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
                            //ex = e1;
                        }
                    }
                }

                if (error != null && error.getMessage() != null) {
                    error.printStackTrace();
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

        this.credential.update(context, responseHandler);
    }
}
