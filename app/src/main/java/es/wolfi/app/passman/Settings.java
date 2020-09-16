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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.JSONUtils;


public class Settings extends Fragment {

    @BindView(R.id.settings_nextcloud_url)
    EditText settings_nextcloud_url;
    @BindView(R.id.settings_nextcloud_user)
    EditText settings_nextcloud_user;
    @BindView(R.id.settings_nextcloud_password)
    EditText settings_nextcloud_password;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @BindView(R.id.settings_app_start_password_switch)
    Switch settings_app_start_password_switch;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @BindView(R.id.settings_encryption_implementation_switch)
    Switch settings_encryption_implementation_switch;

    @BindView(R.id.encryption_implementation_label)
    TextView encryption_implementation_label;

    @BindView(R.id.encryption_implementation_description)
    TextView encryption_implementation_description;

    SharedPreferences settings;

    public Settings() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment CredentialDisplay.
     */
    // TODO: Rename and change types and number of parameters
    public static Settings newInstance() {
        Settings fragment = new Settings();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button settingsSaveButton = (Button) view.findViewById(R.id.settings_save_button);
        settingsSaveButton.setOnClickListener(this.getSaveButtonListener());
        settingsSaveButton.setVisibility(View.VISIBLE);

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

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Hide encryption implementation switch
            settings_encryption_implementation_switch.setClickable(false);
            settings_encryption_implementation_switch.setActivated(false);

            encryption_implementation_label.setTextColor(getResources().getColor(R.color.disabled));
            encryption_implementation_description.setTextColor(getResources().getColor(R.color.disabled));
            settings_encryption_implementation_switch.setTextColor(getResources().getColor(R.color.disabled));

            Toast.makeText(getContext(), R.string.outdated_version_options_hint, Toast.LENGTH_LONG).show();
        }

        settings_nextcloud_url.setText(settings.getString(SettingValues.HOST.toString(), null));
        settings_nextcloud_user.setText(settings.getString(SettingValues.USER.toString(), null));
        settings_nextcloud_password.setText(settings.getString(SettingValues.PASSWORD.toString(), null));
        settings_encryption_implementation_switch.setChecked(settings.getBoolean(SettingValues.JAVA_CRYPTO_IMPLEMENTATION.toString(),
                (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && SJCLCrypto.isJavaEncryptionSupported())));
        settings_app_start_password_switch.setChecked(settings.getBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), false));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public View.OnClickListener getSaveButtonListener() {
        return new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View view) {
                SingleTon ton = SingleTon.getTon();

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.edit().putBoolean(SettingValues.JAVA_CRYPTO_IMPLEMENTATION.toString(), settings_encryption_implementation_switch.isChecked()).commit();
                }

                settings.edit().putBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), settings_app_start_password_switch.isChecked()).commit();

                if (!settings.getString(SettingValues.HOST.toString(), null).equals(settings_nextcloud_url.getText().toString()) ||
                        !settings.getString(SettingValues.USER.toString(), null).equals(settings_nextcloud_user.getText().toString()) ||
                        !settings.getString(SettingValues.PASSWORD.toString(), null).equals(settings_nextcloud_password.getText().toString())) {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());

                    ton.addString(SettingValues.HOST.toString(), settings_nextcloud_url.getText().toString());
                    ton.addString(SettingValues.USER.toString(), settings_nextcloud_user.getText().toString());
                    ton.addString(SettingValues.PASSWORD.toString(), settings_nextcloud_password.getText().toString());

                    settings.edit().putString(SettingValues.HOST.toString(), settings_nextcloud_url.getText().toString()).commit();
                    settings.edit().putString(SettingValues.USER.toString(), settings_nextcloud_user.getText().toString()).commit();
                    settings.edit().putString(SettingValues.PASSWORD.toString(), settings_nextcloud_password.getText().toString()).commit();

                    Objects.requireNonNull(((PasswordList) getActivity())).applyNewSettings(true);
                } else {
                    Objects.requireNonNull(((PasswordList) getActivity())).applyNewSettings(false);
                }
            }
        };
    }
}
