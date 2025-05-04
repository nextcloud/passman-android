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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONException;
import org.json.JSONObject;

import es.wolfi.app.passman.CopyTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.adapters.CustomFieldViewAdapter;
import es.wolfi.app.passman.adapters.FileViewAdapter;
import es.wolfi.app.passman.databinding.FragmentCredentialDisplayBinding;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CredentialACL;
import es.wolfi.passman.API.File;
import es.wolfi.passman.API.SharingACL;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.IconUtils;
import es.wolfi.utils.otp.HashingAlgorithm;
import es.wolfi.utils.otp.TOTPHelper;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialDisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialDisplayFragment extends Fragment {
    public static String CREDENTIAL = "credential";

    private FragmentCredentialDisplayBinding binding;

    ImageView credentialIcon;
    TextView label;
    CopyTextItem user;
    CopyTextItem password;
    CopyTextItem email;
    CopyTextItem url;
    TextView description;
    CopyTextItem otp;
    ProgressBar otp_progress;

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
        handler = new Handler();

        reloadCredentialFromActiveVaultIfPossible();

        if (credential == null) {
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
        binding = FragmentCredentialDisplayBinding.inflate(inflater, container, false);

        credentialIcon = binding.credentialIcon;
        label = binding.credentialLabel;
        user = binding.credentialUser;
        password = binding.credentialPassword;
        email = binding.credentialEmail;
        url = binding.credentialUrl;
        description = binding.credentialDescription;
        otp = binding.credentialOtp;
        otp_progress = binding.contentOtpProgress.credentialOtpProgress;

        return binding.getRoot();
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
        fragmentView = view;
        updateViewContent();
    }

    public void updateViewContent() {
        if (credential != null) {
            if (fragmentView == null) {
                Log.d("updateViewContent", "fragmentView is null (due to a previous activity unload)");
                return;
            }

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
            CredentialACL acl = credential.getCredentialACL();
            if (acl == null || (acl.getPermissions() != null && acl.getPermissions().hasPermission(SharingACL.PERMISSION.WRITE))) {
                editCredentialButton.setVisibility(View.VISIBLE);
            } else {
                editCredentialButton.setVisibility(View.INVISIBLE);
            }


            RecyclerView filesListRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.filesList);
            filesListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            filesListRecyclerView.setAdapter(new FileViewAdapter(credential.getFilesList(), filelistListener));

            RecyclerView customFieldsListRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.customFieldsList);
            customFieldsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            customFieldsListRecyclerView.setAdapter(new CustomFieldViewAdapter(credential, filelistListener));

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

            // overwrite real credential icon for every shared credential
            if (credential.getCredentialACL() != null) {
                // shared with me
                credentialIcon.setImageResource(R.drawable.ic_baseline_share_24);
            } else if (credential.isASharedCredential()) {
                // shared with other (use as alternative to fa-share-alt-square)
                credentialIcon.setImageResource(R.drawable.ic_baseline_share_24);
                credentialIcon.setBackgroundColor(getResources().getColor(R.color.cardview_dark_background));
                credentialIcon.setColorFilter(getResources().getColor(R.color.white));
            } else {
                IconUtils.loadIconToImageView(credential.getFavicon(), credentialIcon);
            }

            if (URLUtil.isValidUrl(credential.getUrl())) {
                url.setModeURL();
            }

            otp_progress.setProgress(0);

            try {
                JSONObject otpObj = new JSONObject(credential.getOtp());
                if (otpObj.has("secret") && otpObj.getString("secret").length() > 4) {
                    String otpSecret = otpObj.getString("secret");

                    int otpDigits = 6;
                    if (otpObj.has("digits")) {
                        otpDigits = otpObj.getInt("digits");
                    }

                    int otpPeriod = 30;
                    if (otpObj.has("period")) {
                        otpPeriod = otpObj.getInt("period");
                    }

                    HashingAlgorithm hashingAlgorithm = HashingAlgorithm.SHA1;
                    if (otpObj.has("algorithm")) {
                        hashingAlgorithm = HashingAlgorithm.fromStringOrSha1(otpObj.getString("algorithm"));
                    }

                    int finalOtpDigits = otpDigits;
                    int finalOtpPeriod = otpPeriod;
                    otp_refresh = TOTPHelper.run(handler, otp_progress, otp.getTextView(), finalOtpDigits, finalOtpPeriod, otpSecret, hashingAlgorithm);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
