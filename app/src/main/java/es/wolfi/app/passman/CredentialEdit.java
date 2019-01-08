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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.GeneralUtils;
import es.wolfi.utils.JSONUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialEdit#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialEdit extends Fragment {
    public static String ARG_CREDENTIAL = "credential";

    @BindView(R.id.credential_label)
    TextView label;
    @BindView(R.id.credential_user)
    EditText user;
    @BindView(R.id.credential_password)
    EditText password;
    @BindView(R.id.credential_email)
    EditText email;
    @BindView(R.id.credential_url)
    EditText url;
    @BindView(R.id.credential_description)
    EditText description;
    @BindView(R.id.credential_linkedapppackage)
    EditText linkedAppPackage;

    private FloatingActionButton fab = null;
    private Credential credential;

    private OnCredentialChangeInteraction mListener;

    public CredentialEdit() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param editCreationGuid Guid of exiting credential, null for new credential
     * @return A new instance of fragment CredentialEdit.
     */
    // TODO: Rename and change types and number of parameters
    public static CredentialEdit newInstance(String editCreationGuid) {
        CredentialEdit fragment = new CredentialEdit();

        Bundle b = new Bundle();
        b.putString(ARG_CREDENTIAL, editCreationGuid);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            if (!TextUtils.isEmpty(getArguments().getString(ARG_CREDENTIAL))) {
                Vault v = Vault.getActiveVault();
                if (v != null) {
                    credential = v.findCredentialByGUID(getArguments().getString(ARG_CREDENTIAL));
                }
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_credential_edit, container, false);
        View fabView = view.findViewById(R.id.savecredfab);

        if (fabView instanceof FloatingActionButton) {
            fab = (FloatingActionButton) fabView;
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fab.setEnabled(false);

                    Vault v = Vault.getActiveVault();
                    if (v != null && credential == null) {
                        Credential newCred = buildNewCredential(v);

                        GeneralUtils.debugAndToast(true, getActivity(), "Creating");

                        Vault.addCredential(
                                false,
                                getContext(),
                                v,
                                newCred,
                                new FutureCallback<Credential>() {

                                    @Override
                                    public void onCompleted(Exception e, Credential result) {
                                        Log.d("API", "Back");
                                        if (e != null) {
                                            GeneralUtils.debugAndToast(true, getActivity(), "Creating Failed: " + e.toString());
                                        } else {
                                            GeneralUtils.debugAndToast(true, getActivity(), "Created");
                                            if (credential != null && credential.getVault() != null) {
                                                mListener.OnCredentialChanged(credential.getVault().guid, credential.getGuid());
                                            }
                                        }
                                    }
                                });
                    } else if (v != null) {
                        Credential oldCred = buildOldCredential(v, credential);

                        GeneralUtils.debugAndToast(true, getActivity(), "Saving");

                        Vault.saveCredential(
                                false,
                                getContext(),
                                v,
                                oldCred,
                                new FutureCallback<Credential>() {

                                    @Override
                                    public void onCompleted(Exception e, Credential result) {
                                        Log.d("API", "Back");
                                        if (e != null) {
                                            GeneralUtils.debugAndToast(true, getActivity(), "Saving Failed: " + e.toString());
                                        } else {
                                            GeneralUtils.debugAndToast(true, getActivity(), "Saved");
                                            if (result != null && result.getVault() != null) {
                                                mListener.OnCredentialChanged(credential.getVault().guid, credential.getGuid());
                                            }
                                        }
                                    }
                                });
                    }
                }
            });
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCredentialChangeInteraction) {
            mListener = (OnCredentialChangeInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCredentialChangeInteraction");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);


        if (credential != null) {
            label.setText(credential.getLabel());
            user.setText(credential.getUsername());
            password.setText(credential.getPassword());
            email.setText(credential.getEmail());
            url.setText(credential.getUrl());
            description.setText(credential.getDescription());
            String customFields = credential.getCustomFields();
            try {
                if (!TextUtils.isEmpty(customFields)) {
                    if (JSONUtils.isJSONArray(customFields)) {
                        JSONArray customFieldArray = new JSONArray(customFields);
                        for (int i = 0; i < customFieldArray.length(); i++) {
                            JSONObject currentFieldObject = customFieldArray.getJSONObject(i);
                            String label = currentFieldObject.getString("label");
                            String value = currentFieldObject.getString("value");
                            if (!TextUtils.isEmpty(label) && !TextUtils.isEmpty(value)) {
                                if (label.equalsIgnoreCase("androidCredPackageName")) {
                                    linkedAppPackage.setText(value);
                                }
                            }
                        }
                    }
                }
            } catch (JSONException ex) {
                GeneralUtils.debug(ex.toString());
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public Credential buildNewCredential(Vault v) {
        String customFieldString = "";
        try {
            JSONArray customFields = new JSONArray();
            JSONObject customField = new JSONObject();
            customField.put("label", "androidCredPackageName");
            customField.put("value", linkedAppPackage.getText());
            customField.put("secret", false);
            customField.put("field_type", "text");
            customFields.put(customField);
            customFieldString = customFields.toString();
        } catch (JSONException e) {
        }

        GeneralUtils.debug("Building credential");

        Credential newCred = new Credential();
        newCred
                .setVault(v)
                .setDescription(description.getText().toString())
                .setEmail(email.getText().toString())
                .setLabel(label.getText().toString())
                .setCustomFields(customFieldString)
                .setUsername(user.getText().toString())
                .setPassword(password.getText().toString())
                .setFiles((new JSONArray()).toString())
                .setTags((new JSONArray()).toString())
                .setOtp((new JSONObject()).toString())
                .setUrl(url.getText().toString());

        return newCred;
    }

    public Credential buildOldCredential(Vault v, Credential c) {
        String customFields = c.getCustomFields();
        try {
            if (!TextUtils.isEmpty(customFields)) {
                if (JSONUtils.isJSONArray(customFields)) {
                    JSONArray customFieldArray = new JSONArray(customFields);
                    for (int i = 0; i < customFieldArray.length(); i++) {
                        JSONObject currentFieldObject = customFieldArray.getJSONObject(i);
                        String label = currentFieldObject.getString("label");
                        String value = currentFieldObject.getString("value");
                        if (!TextUtils.isEmpty(label) && !TextUtils.isEmpty(value)) {
                            if (label.equalsIgnoreCase("androidCredPackageName")) {
                                currentFieldObject.put("value", linkedAppPackage.getText().toString());
                                customFieldArray.put(i, currentFieldObject);
                                customFields = customFieldArray.toString();
                                break;
                            }
                        }
                    }
                }
            }
        } catch (JSONException ex) {
            GeneralUtils.debug(ex.toString());
        }

        c
                .setCustomFields(customFields)
                .setDescription(description.getText().toString())
                .setEmail(email.getText().toString())
                .setLabel(label.getText().toString())
                .setUsername(user.getText().toString())
                .setPassword(password.getText().toString())
                .setUrl(url.getText().toString());

        return c;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnCredentialChangeInteraction {
        // TODO: Update argument type and name
        void OnCredentialChanged(String vaultGuid, String credentialGuid);
    }
}
