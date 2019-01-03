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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.apache.commons.codec.binary.Base32;

import net.bierbaumer.otp_authenticator.TOTPHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialDisplay#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialDisplay extends Fragment {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.credential_label) TextView label;
    @BindView(R.id.credential_user) CopyTextItem user;
    @BindView(R.id.credential_password) CopyTextItem password;
    @BindView(R.id.credential_email) CopyTextItem email;
    @BindView(R.id.credential_url) TextView url;
    @BindView(R.id.credential_description) TextView description;
    @BindView(R.id.credential_otp) CopyTextItem otp;
    @BindView(R.id.credential_otp_progress) ProgressBar otp_progress;

    private Credential credential;
    private Handler handler;
    private Runnable otp_refresh;

    private OnCredentialFragmentInteraction mListener;

    public CredentialDisplay() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param credentialGUID The guid of the credential to display.
     * @return A new instance of fragment CredentialDisplay.
     */
    // TODO: Rename and change types and number of parameters
    public static CredentialDisplay newInstance(String credentialGUID) {
        CredentialDisplay fragment = new CredentialDisplay();

        Bundle b = new Bundle();
        b.putString(CREDENTIAL, credentialGUID);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
            if (v != null) {
                credential = v.findCredentialByGUID(getArguments().getString(CREDENTIAL));
            }
        }

        handler = new Handler();
        otp_refresh = new Runnable() {
            @Override
            public void run() {
                int progress =  (int) (System.currentTimeMillis() / 1000) % 30 ;
                otp_progress.setProgress(progress*100);

                ObjectAnimator animation = ObjectAnimator.ofInt(otp_progress, "progress", (progress+1)*100);
                animation.setDuration(1000);
                animation.setInterpolator(new LinearInterpolator());
                animation.start();

                otp.setText(TOTPHelper.generate(new Base32().decode(credential.getOtp())));
                handler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(otp_refresh);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(otp_refresh);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_credential_display, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCredentialFragmentInteraction) {
            mListener = (OnCredentialFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        if (credential != null) {
            label.setText(credential.getLabel());
            user.setText(credential.getUsername());
            password.setModePassword();
            password.setText(credential.getPassword());
            email.setModeEmail();
            email.setText(credential.getEmail());
            url.setText(credential.getUrl());
            description.setText(credential.getDescription());
            otp.setEnabled(false);
        }
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
    public interface OnCredentialFragmentInteraction {
        // TODO: Update argument type and name
        void onCredentialFragmentInteraction(Credential credential);
    }
}
