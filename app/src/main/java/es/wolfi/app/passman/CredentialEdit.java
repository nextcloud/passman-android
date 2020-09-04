/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package es.wolfi.app.passman;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.koushikdutta.async.future.FutureCallback;

import net.bierbaumer.otp_authenticator.TOTPHelper;

import org.apache.commons.codec.binary.Base32;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialEdit#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialEdit extends Fragment implements View.OnClickListener {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.edit_credential_label_header) TextView label_header;
    @BindView(R.id.edit_credential_label) EditText label;
    @BindView(R.id.edit_credential_user) EditText user;
    @BindView(R.id.edit_credential_password) EditText password;
    @BindView(R.id.edit_credential_email) EditText email;
    @BindView(R.id.edit_credential_url) EditText url;
    @BindView(R.id.edit_credential_description) EditText description;

    private OnCredentialFragmentInteraction mListener;
    private Credential credential;
    private boolean alreadySaving = false;

    public CredentialEdit() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment CredentialDisplay.
     */
    // TODO: Rename and change types and number of parameters
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

        label.setText(this.credential.getLabel());
        user.setText(this.credential.getUsername());
        password.setText(this.credential.getPassword());
        email.setText(this.credential.getEmail());
        url.setText(this.credential.getUrl());
        description.setText(this.credential.getDescription());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
        super.onCreate(savedInstanceState);
        Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        this.credential = new Credential();
        this.credential.setVault(v);
         */

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
            credential = v.findCredentialByGUID(getArguments().getString(CREDENTIAL));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        if(alreadySaving){
            return;
        }

        if (label.getText().toString().equals("")){
            label_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        }

        this.credential.setLabel(label.getText().toString());
        this.credential.setUsername(user.getText().toString());
        this.credential.setPassword(password.getText().toString());
        this.credential.setEmail(email.getText().toString());
        this.credential.setUrl(url.getText().toString());
        this.credential.setDescription(description.getText().toString());
        this.credential.setOtp("{}");
        this.credential.setFiles("");
        this.credential.setCustomFields("");
        this.credential.setTags("");
        this.credential.setFavicon("");
        this.credential.setCompromised("");
        this.credential.setHidden(false);

        alreadySaving = true;
        this.credential.update(view.getContext(), new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (e == null && !result.equals("")){
                    //Log.v("Credential saved", result);
                    Snackbar.make(view, R.string.successfully_updated, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    assert getFragmentManager() != null;
                    alreadySaving = false;
                    getFragmentManager().popBackStack();
                    Objects.requireNonNull(((PasswordList)getActivity())).refreshVault();
                } else {
                    if(e != null && e.getMessage() != null){
                        e.printStackTrace();
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else {
                        Snackbar.make(view, R.string.error_occurred, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            }
        });
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
