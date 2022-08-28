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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpResponseHandler;

import net.bierbaumer.otp_authenticator.TOTPHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.app.ResponseHandlers.CredentialAddFileResponseHandler;
import es.wolfi.app.ResponseHandlers.CredentialDeleteResponseHandler;
import es.wolfi.app.ResponseHandlers.CredentialSaveResponseHandler;
import es.wolfi.app.passman.EditPasswordTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.app.passman.adapters.CustomFieldEditAdapter;
import es.wolfi.app.passman.adapters.FileEditAdapter;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.FileUtils;
import es.wolfi.utils.ProgressUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialEditFragment extends Fragment implements View.OnClickListener {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.edit_credential_label_header)
    TextView label_header;
    @BindView(R.id.edit_credential_label)
    EditText label;
    @BindView(R.id.edit_credential_user)
    EditText user;
    @BindView(R.id.edit_credential_password)
    EditPasswordTextItem password;
    @BindView(R.id.edit_credential_email)
    EditText email;
    @BindView(R.id.edit_credential_url)
    EditText url;
    @BindView(R.id.edit_credential_description)
    EditText description;

    AppCompatImageButton otpEditCollapseExtendedButton;
    LinearLayout otp_edit_extended;
    EditText otp_secret;
    EditText otp_digits;
    EditText otp_period;
    EditText otp_label;
    EditText otp_issuer;
    TextView credential_otp;
    ProgressBar otp_progress;

    @BindView(R.id.filesList)
    RecyclerView filesList;
    @BindView(R.id.customFieldsList)
    RecyclerView customFieldsList;
    @BindView(R.id.customFieldType)
    Spinner customFieldType;

    private Credential credential;
    private AtomicBoolean alreadySaving = new AtomicBoolean(false);
    private FileEditAdapter fed;
    private CustomFieldEditAdapter cfed;
    private RecyclerView filesListRecyclerView;
    private RecyclerView customFieldsListRecyclerView;
    private Handler handler = null;
    private Runnable otp_refresh = null;
    private String otp_qr_uri = "";
    private String otp_algorithm = "SHA1";
    private String otp_type = "totp";

    public CredentialEditFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment CredentialEditFragment.
     */
    public static CredentialEditFragment newInstance(String credentialGUID) {
        CredentialEditFragment fragment = new CredentialEditFragment();

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

        FloatingActionButton updateCredentialButton = (FloatingActionButton) view.findViewById(R.id.UpdateCredentialButton);
        updateCredentialButton.setOnClickListener(this);
        updateCredentialButton.setVisibility(View.VISIBLE);

        FloatingActionButton deleteCredentialButton = (FloatingActionButton) view.findViewById(R.id.DeleteCredentialButton);
        deleteCredentialButton.setOnClickListener(this.getDeleteButtonListener());
        deleteCredentialButton.setVisibility(View.VISIBLE);

        AppCompatImageButton scanOtpQRCodeButton = (AppCompatImageButton) view.findViewById(R.id.scanOtpQRCodeButton);
        scanOtpQRCodeButton.setOnClickListener(this.getScanOtpQRCodeButtonListener());

        otpEditCollapseExtendedButton = (AppCompatImageButton) view.findViewById(R.id.otpEditCollapseExtendedButton);
        otpEditCollapseExtendedButton.setOnClickListener(this.getOtpEditCollapseExtendedButtonListener());
        otpEditCollapseExtendedButton.setVisibility(View.VISIBLE);

        Button addFileButton = (Button) view.findViewById(R.id.AddFileButton);
        addFileButton.setOnClickListener(this.getAddFileButtonListener());
        addFileButton.setVisibility(View.VISIBLE);

        Button addCustomFieldButton = (Button) view.findViewById(R.id.AddCustomFieldButton);
        addCustomFieldButton.setOnClickListener(this.getAddCustomFieldButtonListener());
        addCustomFieldButton.setVisibility(View.VISIBLE);

        fed = new FileEditAdapter(credential);
        cfed = new CustomFieldEditAdapter(credential);

        otp_edit_extended = (LinearLayout) view.findViewById(R.id.otp_edit_extended);
        otp_secret = view.findViewById(R.id.edit_credential_otp_secret);
        otp_digits = view.findViewById(R.id.edit_credential_otp_digits);
        otp_period = view.findViewById(R.id.edit_credential_otp_period);
        otp_label = view.findViewById(R.id.otp_label);
        otp_issuer = view.findViewById(R.id.otp_issuer);
        credential_otp = view.findViewById(R.id.credential_otp);
        otp_progress = view.findViewById(R.id.credential_otp_progress);

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

        Vault.checkCloudConnectionAndShowHint(view);

        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.filesList);
        filesListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        filesListRecyclerView.setAdapter(fed);

        customFieldsListRecyclerView = (RecyclerView) view.findViewById(R.id.customFieldsList);
        customFieldsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        customFieldsListRecyclerView.setAdapter(cfed);

        label.setText(this.credential.getLabel());
        user.setText(this.credential.getUsername());
        password.setText(this.credential.getPassword());
        email.setText(this.credential.getEmail());
        url.setText(this.credential.getUrl());
        description.setText(this.credential.getDescription());

        try {
            JSONObject otpObj = new JSONObject(this.credential.getOtp());

            setOTPValuesFromJSON(otpObj);

            if (otpObj.has("secret") && otpObj.getString("secret").length() > 4) {
                String otpSecret = otpObj.getString("secret");
                otp_secret.setText(otpSecret);
            }

            handler = new Handler();
            otp_refresh = TOTPHelper.runAndUpdate(handler, otp_progress, credential_otp, otp_digits, otp_period, otp_secret);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
            try {
                credential = Credential.clone(v.findCredentialByGUID(getArguments().getString(CREDENTIAL)));
            } catch (JSONException e) {
                credential = v.findCredentialByGUID(getArguments().getString(CREDENTIAL));
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
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

    public void addSelectedFile(String encodedFile, String fileName, String mimeType, int fileSize, int requestCode) throws JSONException {
        Context context = getContext();
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
        final AsyncHttpResponseHandler responseHandler = new CredentialAddFileResponseHandler(progress, getView(), fileName, requestCode, fed, cfed);

        // Start encryption a little later so that the main thread does not get stuck in the file selection dialog and it can close.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                credential.uploadFile(context, encodedFile, fileName, mimeType, fileSize, responseHandler, progress);
            }
        }, 100);
    }

    private void setOTPValuesFromJSON(JSONObject otpObj) {
        try {
            if (otpObj.has("secret")) {
                if (otpObj.has("type")) {
                    otp_type = otpObj.getString("type");
                }
                if (otpObj.has("algorithm")) {
                    otp_algorithm = otpObj.getString("algorithm");
                }
                if (otpObj.has("qr_uri")) {
                    otp_qr_uri = otpObj.getString("qr_uri");
                }

                otp_secret.setText(otpObj.getString("secret"));

                int period = 30;
                if (otpObj.has("period")) {
                    period = otpObj.getInt("period");
                }
                otp_period.setText(String.valueOf(period));

                int digits = 6;
                if (otpObj.has("digits")) {
                    digits = otpObj.getInt("digits");
                }
                otp_digits.setText(String.valueOf(digits));

                if (otpObj.has("label")) {
                    otp_label.setText(otpObj.getString("label"));
                }
                if (otpObj.has("issuer")) {
                    otp_issuer.setText(otpObj.getString("issuer"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void processScannedQRCodeData(String qr_uri) {
        Log.d("CredentialEdit", "processScannedQRCodeData begins");

        try {
            setOTPValuesFromJSON(TOTPHelper.getCompleteOTPDataFromQrUrl(qr_uri));
            Log.d("CredentialEdit", "processScannedQRCodeData done");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("CredentialEdit", "processScannedQRCodeData failed");
        }
    }

    public View.OnClickListener getScanOtpQRCodeButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((PasswordListActivity) requireActivity()).scanQRCodeForOTP(PasswordListActivity.REQUEST_CODE_SCAN_QR_CODE_FOR_OTP_EDIT);
            }
        };
    }

    public View.OnClickListener getOtpEditCollapseExtendedButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (otp_edit_extended.getVisibility() == View.VISIBLE) {
                    otp_edit_extended.setVisibility(View.GONE);
                    otpEditCollapseExtendedButton.setRotation(-90);
                } else {
                    otp_edit_extended.setVisibility(View.VISIBLE);
                    otpEditCollapseExtendedButton.setRotation(0);
                }
            }
        };
    }

    public View.OnClickListener getAddFileButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((PasswordListActivity) requireActivity()).selectFileToAdd(FileUtils.activityRequestFileCode.credentialEditFile.ordinal());
            }
        };
    }

    public View.OnClickListener getAddCustomFieldButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (customFieldType.getSelectedItem().toString().equals("File")) {
                    ((PasswordListActivity) requireActivity()).selectFileToAdd(FileUtils.activityRequestFileCode.credentialEditCustomFieldFile.ordinal());
                } else {
                    CustomField cf = new CustomField();
                    cf.setLabel("newLabel" + (cfed.getItemCount() + 1));
                    cf.setSecret(customFieldType.getSelectedItem().toString().toLowerCase().equals("password"));
                    cf.setFieldType(customFieldType.getSelectedItem().toString().toLowerCase());
                    cf.setValue("");

                    cfed.addCustomField(cf);
                    cfed.notifyDataSetChanged();
                }
            }
        };
    }

    public View.OnClickListener getDeleteButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage(R.string.confirm_credential_deletion);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (alreadySaving.get()) {
                            return;
                        }

                        alreadySaving.set(true);

                        Context context = getContext();
                        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
                        final AsyncHttpResponseHandler responseHandler = new CredentialDeleteResponseHandler(alreadySaving, progress, view, (PasswordListActivity) getActivity(), getFragmentManager());

                        Date date = new Date();
                        credential.setDeleteTime(date.getTime());
                        credential.update(view.getContext(), responseHandler);

                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            }
        };
    }

    @Override
    public void onClick(View view) {
        if (alreadySaving.get()) {
            return;
        }

        if (label.getText().toString().equals("")) {
            label_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        }

        if (this.credential.getCompromised().equals("true") && !this.credential.getPassword().equals(password.getText().toString())) {
            this.credential.setCompromised(false);
        }

        // fix sometimes wrong saved compromised field
        if (!this.credential.getCompromised().equals("true")) {
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

        if (otp_secret.getText().toString().isEmpty()) {
            this.credential.setOtp(new JSONObject().toString());
        } else {
            JSONObject otpObj = TOTPHelper.getCompleteOTPDataAsJSONObject(otp_secret,
                    otp_digits,
                    otp_period,
                    otp_label,
                    otp_issuer,
                    otp_qr_uri,
                    otp_algorithm,
                    otp_type
            );
            this.credential.setOtp(otpObj.toString());
        }

        alreadySaving.set(true);

        Context context = getContext();
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
        final AsyncHttpResponseHandler responseHandler = new CredentialSaveResponseHandler(alreadySaving, true, progress, view, (PasswordListActivity) getActivity(), getFragmentManager());

        this.credential.update(context, responseHandler);
    }
}
