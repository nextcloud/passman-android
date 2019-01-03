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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialCreate#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialCreate extends Fragment {
    //public static String CREDENTIAL = "credential";

    @BindView(R.id.credential_label) TextView label;
    @BindView(R.id.credential_user) EditText user;
    @BindView(R.id.credential_password) EditText password;
    @BindView(R.id.credential_email) EditText email;
    @BindView(R.id.credential_url) EditText url;
    @BindView(R.id.credential_description)
    EditText description;

    private Credential credential;
    private Handler handler;

    private OnCredentialCreateInteraction mListener;

    public CredentialCreate() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CredentialCreate.
     */
    // TODO: Rename and change types and number of parameters
    public static CredentialCreate newInstance() {
        CredentialCreate fragment = new CredentialCreate();

        Bundle b = new Bundle();
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        return inflater.inflate(R.layout.fragment_credential_create, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCredentialCreateInteraction) {
            mListener = (OnCredentialCreateInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCredentialCreateInteraction");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

/*
        if (credential != null) {
            label.setText(credential.getLabel());
            user.setText(credential.getUsername());
            password.setModePassword();
            password.setText(credential.getPassword());
            email.setText(credential.getEmail());
            url.setText(credential.getUrl());
            description.setText(credential.getDescription());
        }
*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnCredentialCreateInteraction {
        // TODO: Update argument type and name
        void OnCredentialCreated(Vault v, Credential credential);
    }
}
