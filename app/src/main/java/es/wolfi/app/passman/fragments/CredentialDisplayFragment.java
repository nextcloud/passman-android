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

package es.wolfi.app.passman.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.bierbaumer.otp_authenticator.TOTPHelper;

import org.apache.commons.codec.binary.Base32;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.app.passman.CopyTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.adapters.CustomFieldViewAdapter;
import es.wolfi.app.passman.adapters.FileViewAdapter;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.File;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.IconUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialDisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialDisplayFragment extends Fragment {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.credentialIcon)
    ImageView credentialIcon;
    @BindView(R.id.credential_label)
    TextView label;
    @BindView(R.id.credential_user)
    CopyTextItem user;
    @BindView(R.id.credential_password)
    CopyTextItem password;
    @BindView(R.id.credential_email)
    CopyTextItem email;
    @BindView(R.id.credential_url)
    CopyTextItem url;
    @BindView(R.id.credential_description)
    TextView description;
    @BindView(R.id.credential_otp)
    CopyTextItem otp;
    @BindView(R.id.credential_otp_progress)
    ProgressBar otp_progress;
    @BindView(R.id.filesList)
    RecyclerView filesList;
    @BindView(R.id.customFieldsList)
    RecyclerView customFieldsList;

    private Credential credential;
    private Handler handler;
    private Runnable otp_refresh = null;
    private View fragmentView;

    private OnCredentialFragmentInteraction mListener;
    private OnListFragmentInteractionListener filelistListener;

    public CredentialDisplayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param credentialGUID The guid of the credential to display.
     * @return A new instance of fragment CredentialDisplayFragment.
     */
    public static CredentialDisplayFragment newInstance(String credentialGUID) {
        CredentialDisplayFragment fragment = new CredentialDisplayFragment();

        Bundle b = new Bundle();
        b.putString(CREDENTIAL, credentialGUID);
        fragment.setArguments(b);

        return fragment;
    }

    public void reloadCredentialFromActiveVaultIfPossible() {
        if (getArguments() != null) {
            Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
            if (v != null) {
                Credential credential = v.findCredentialByGUID(getArguments().getString(CREDENTIAL));
                if (credential != null) {   // credential may have been removed from vault in the meantime
                    this.credential = credential;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        reloadCredentialFromActiveVaultIfPossible();

        if (credential != null) {
            handler = new Handler();
            try {
                JSONObject otpObj = new JSONObject(credential.getOtp());
                if (otpObj.has("secret") && otpObj.getString("secret").length() > 4) {
                    String otpSecret = otpObj.getString("secret");
                    otp_refresh = new Runnable() {
                        @Override
                        public void run() {
                            int progress = (int) (System.currentTimeMillis() / 1000) % 30;
                            otp_progress.setProgress(progress * 100);

                            ObjectAnimator animation = ObjectAnimator.ofInt(otp_progress, "progress", (progress + 1) * 100);
                            animation.setDuration(1000);
                            animation.setInterpolator(new LinearInterpolator());
                            animation.start();

                            otp.setText(TOTPHelper.generate(new Base32().decode(otpSecret)));
                            handler.postDelayed(this, 1000);
                        }
                    };
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getContext(), getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
            Log.e("CredentialDisplayFrag", "credential or vault is null");
        }
    }

    public String getGuid() {
        return this.credential.getGuid();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (otp_refresh != null) {
            handler.post(otp_refresh);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (otp_refresh != null) {
            handler.removeCallbacks(otp_refresh);
        }
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
        if (context instanceof OnListFragmentInteractionListener) {
            filelistListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        fragmentView = view;
        updateViewContent();
    }

    public void updateViewContent() {
        if (credential != null) {
            FloatingActionButton editCredentialButton = fragmentView.findViewById(R.id.editCredentialButton);
            editCredentialButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getParentFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.content_password_list, CredentialEditFragment.newInstance(credential.getGuid()), "credentialEdit")
                            .addToBackStack(null)
                            .commit();
                }
            });
            editCredentialButton.setVisibility(View.VISIBLE);


            RecyclerView filesListRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.filesList);
            filesListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            filesListRecyclerView.setAdapter(new FileViewAdapter(credential.getFilesList(), filelistListener));

            RecyclerView customFieldsListRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.customFieldsList);
            customFieldsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            customFieldsListRecyclerView.setAdapter(new CustomFieldViewAdapter(credential.getCustomFieldsList(), filelistListener));

            if (credential.getCompromised().equals("true")) {
                TextView passwordLabel = fragmentView.findViewById(R.id.credential_password_label);
                passwordLabel.setBackgroundColor(getResources().getColor(R.color.compromised));
            }

            label.setText(credential.getLabel());
            user.setText(credential.getUsername());
            password.setModePassword();
            password.setText(credential.getPassword());
            email.setModeEmail();
            email.setText(credential.getEmail());
            url.setText(credential.getUrl());
            description.setText(credential.getDescription());
            otp.setEnabled(false);
            IconUtils.loadIconToImageView(credential.getFavicon(), credentialIcon);

            if (URLUtil.isValidUrl(credential.getUrl())) {
                url.setModeURL();
            }

            if (otp_refresh == null) {
                otp_progress.setProgress(0);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        filelistListener = null;
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

    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(File item);
    }
}
