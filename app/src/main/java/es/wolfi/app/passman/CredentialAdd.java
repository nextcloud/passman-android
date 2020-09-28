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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialAdd#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialAdd extends Fragment implements View.OnClickListener {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.add_credential_label_header)
    TextView label_header;
    @BindView(R.id.add_credential_label)
    EditText label;
    @BindView(R.id.add_credential_user)
    EditText user;
    @BindView(R.id.add_credential_password)
    EditText password;
    @BindView(R.id.add_credential_email)
    EditText email;
    @BindView(R.id.add_credential_url)
    EditText url;
    @BindView(R.id.add_credential_description)
    EditText description;

    private OnCredentialFragmentInteraction mListener;
    private Credential credential;
    private boolean alreadySaving = false;

    public CredentialAdd() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment CredentialAdd.
     */
    public static CredentialAdd newInstance() {
        return new CredentialAdd();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_credential_add, container, false);

        Button saveCredentialButton = (Button) view.findViewById(R.id.SaveCredentialButton);
        saveCredentialButton.setOnClickListener(this);
        saveCredentialButton.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCredentialFragmentInteraction) {
            mListener = (OnCredentialFragmentInteraction) context;
        } else {
            //throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        this.credential = new Credential();
        this.credential.setVault(v);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

        this.credential.setLabel(label.getText().toString());
        this.credential.setUsername(user.getText().toString());
        this.credential.setPassword(password.getText().toString());
        this.credential.setEmail(email.getText().toString());
        this.credential.setUrl(url.getText().toString());
        this.credential.setDescription(description.getText().toString());
        this.credential.setOtp("{}");
        this.credential.setFiles("[]");
        this.credential.setCustomFields("[]");
        this.credential.setTags("");
        this.credential.setFavicon("");
        this.credential.setCompromised("");
        this.credential.setHidden(false);

        alreadySaving = true;

        AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                String result = new String(responseBody);
                if (statusCode == 200 && !result.equals("")) {
                    Snackbar.make(view, R.string.successfully_saved, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    assert getFragmentManager() != null;
                    Objects.requireNonNull(((PasswordList) getActivity())).refreshVault();
                    Objects.requireNonNull(((PasswordList) getActivity())).showAddCredentialsButton();
                    alreadySaving = false;
                    progress.dismiss();
                    getFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                alreadySaving = false;
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
                        //ex = e1;
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

        this.credential.save(context, responseHandler);
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
    public interface OnCredentialFragmentInteraction {
        // TODO: Update argument type and name
        void onCredentialFragmentInteraction(Credential credential);

        void saveCredential(Credential credential, Context c, FutureCallback<String> cb);
    }
}
