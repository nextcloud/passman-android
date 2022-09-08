/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2022, Timo Triebensky (timo@binsky.org)
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.koushikdutta.async.future.FutureCallback;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import butterknife.ButterKnife;
import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SettingsCache;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.KeyStoreUtils;
import es.wolfi.utils.PasswordGenerator;


public class SettingsFragment extends Fragment {

    RelativeLayout manual_server_connection_settings;
    RelativeLayout sso_settings;

    TextView sso_user_server;

    EditText settings_nextcloud_url;
    EditText settings_nextcloud_user;
    EditText settings_nextcloud_password;

    MaterialCheckBox settings_app_start_password_switch;

    MaterialCheckBox settings_password_generator_shortcut_switch;
    MaterialCheckBox settings_password_generator_use_uppercase_switch;
    MaterialCheckBox settings_password_generator_use_lowercase_switch;
    MaterialCheckBox settings_password_generator_use_digits_switch;
    MaterialCheckBox settings_password_generator_use_special_chars_switch;
    MaterialCheckBox settings_password_generator_avoid_ambiguous_chars_switch;
    MaterialCheckBox settings_password_generator_require_every_char_type_switch;
    EditText settings_password_generator_length_value;

    MaterialCheckBox enable_credential_list_icons_switch;
    MaterialCheckBox enable_offline_cache_switch;

    TextView default_autofill_vault_title;
    Spinner default_autofill_vault;
    EditText clear_clipboard_delay_value;

    EditText request_connect_timeout_value;
    EditText request_response_timeout_value;
    Button clear_offline_cache_button;

    SharedPreferences settings;
    SingleSignOnAccount ssoAccount;
    PasswordGenerator passwordGenerator;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment SettingsFragment.
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        manual_server_connection_settings = view.findViewById(R.id.manual_server_connection_settings);
        sso_settings = view.findViewById(R.id.sso_settings);

        FloatingActionButton settingsSaveButton = view.findViewById(R.id.settings_save_button);
        settingsSaveButton.setOnClickListener(this.getSaveButtonListener());

        Button sso_user_server_logout_button = view.findViewById(R.id.sso_user_server_logout_button);
        sso_user_server_logout_button.setOnClickListener(this.getSSOLogoutButtonListener());

        sso_user_server = view.findViewById(R.id.sso_user_server);

        settings_nextcloud_url = view.findViewById(R.id.settings_nextcloud_url);
        settings_nextcloud_user = view.findViewById(R.id.settings_nextcloud_user);
        settings_nextcloud_password = view.findViewById(R.id.settings_nextcloud_password);

        settings_app_start_password_switch = view.findViewById(R.id.settings_app_start_password_switch);

        settings_password_generator_shortcut_switch = view.findViewById(R.id.settings_password_generator_shortcut_switch);
        settings_password_generator_use_uppercase_switch = view.findViewById(R.id.settings_password_generator_use_uppercase_switch);
        settings_password_generator_use_lowercase_switch = view.findViewById(R.id.settings_password_generator_use_lowercase_switch);
        settings_password_generator_use_digits_switch = view.findViewById(R.id.settings_password_generator_use_digits_switch);
        settings_password_generator_use_special_chars_switch = view.findViewById(R.id.settings_password_generator_use_special_chars_switch);
        settings_password_generator_avoid_ambiguous_chars_switch = view.findViewById(R.id.settings_password_generator_avoid_ambiguous_chars_switch);
        settings_password_generator_require_every_char_type_switch = view.findViewById(R.id.settings_password_generator_require_every_char_type_switch);
        settings_password_generator_length_value = view.findViewById(R.id.settings_password_generator_length_value);

        enable_credential_list_icons_switch = view.findViewById(R.id.enable_credential_list_icons_switch);
        enable_offline_cache_switch = view.findViewById(R.id.enable_offline_cache_switch);

        default_autofill_vault_title = view.findViewById(R.id.default_autofill_vault_title);
        default_autofill_vault = view.findViewById(R.id.default_autofill_vault);
        clear_clipboard_delay_value = view.findViewById(R.id.clear_clipboard_delay_value);

        request_connect_timeout_value = view.findViewById(R.id.request_connect_timeout_value);
        request_response_timeout_value = view.findViewById(R.id.request_response_timeout_value);
        clear_offline_cache_button = view.findViewById(R.id.clear_offline_cache_button);
        clear_offline_cache_button.setOnClickListener(this.getClearOfflineCacheButtonListener());

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

        try {
            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(getContext());
            manual_server_connection_settings.removeAllViews();
            sso_settings.setVisibility(View.VISIBLE);

            String hostname = "";
            try {
                URL uri = new URL(ssoAccount.url);
                hostname = uri.getHost();
            } catch (MalformedURLException e) {
                Log.d("SettingsFragment", "Error parsing host from sso account");
            }
            sso_user_server.setText(String.format("%s@%s", ssoAccount.userId, hostname));
        } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
            manual_server_connection_settings.setVisibility(View.VISIBLE);
            sso_settings.removeAllViews();
        }

        settings_nextcloud_url.setText(KeyStoreUtils.getString(SettingValues.HOST.toString(), null));
        settings_nextcloud_user.setText(KeyStoreUtils.getString(SettingValues.USER.toString(), null));
        settings_nextcloud_password.setText(KeyStoreUtils.getString(SettingValues.PASSWORD.toString(), null));

        settings_app_start_password_switch.setChecked(settings.getBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), false));

        passwordGenerator = new PasswordGenerator(getContext());

        settings_password_generator_shortcut_switch.setChecked(settings.getBoolean(SettingValues.ENABLE_PASSWORD_GENERATOR_SHORTCUT.toString(), true));
        settings_password_generator_use_uppercase_switch.setChecked(passwordGenerator.isUseUppercase());
        settings_password_generator_use_lowercase_switch.setChecked(passwordGenerator.isUseLowercase());
        settings_password_generator_use_digits_switch.setChecked(passwordGenerator.isUseDigits());
        settings_password_generator_use_special_chars_switch.setChecked(passwordGenerator.isUseSpecialChars());
        settings_password_generator_avoid_ambiguous_chars_switch.setChecked(passwordGenerator.isAvoidAmbiguousCharacters());
        settings_password_generator_require_every_char_type_switch.setChecked(passwordGenerator.isRequireEveryCharType());
        settings_password_generator_length_value.setText(String.valueOf(passwordGenerator.getLength()));

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            ((ViewManager) settings_password_generator_shortcut_switch.getParent()).removeView(settings_password_generator_shortcut_switch);
        }

        enable_credential_list_icons_switch.setChecked(settings.getBoolean(SettingValues.ENABLE_CREDENTIAL_LIST_ICONS.toString(), true));
        enable_offline_cache_switch.setChecked(settings.getBoolean(SettingValues.ENABLE_OFFLINE_CACHE.toString(), true));

        Set<Map.Entry<String, Vault>> vaults = getVaultsEntrySet();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && vaults != null) {
            String last_selected_guid = "";
            if (settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), null) != null) {
                last_selected_guid = settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), null);
            }

            String[] vault_names = new String[vaults.size() + 1];
            vault_names[0] = getContext().getString(R.string.automatically);
            int i = 1;
            int selection_id = 0;
            for (Map.Entry<String, Vault> vault_entry : vaults) {
                if (last_selected_guid.equals(vault_entry.getValue().guid)) {
                    selection_id = i;
                }
                vault_names[i] = vault_entry.getValue().name;
                i++;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, vault_names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            default_autofill_vault.setAdapter(adapter);
            default_autofill_vault.setSelection(selection_id);
        } else {
            ((ViewManager) default_autofill_vault.getParent()).removeView(default_autofill_vault);
            ((ViewManager) default_autofill_vault_title.getParent()).removeView(default_autofill_vault_title);
        }

        clear_clipboard_delay_value.setText(String.valueOf(settings.getInt(SettingValues.CLEAR_CLIPBOARD_DELAY.toString(), 0)));

        request_connect_timeout_value.setText(String.valueOf(settings.getInt(SettingValues.REQUEST_CONNECT_TIMEOUT.toString(), 15)));
        request_response_timeout_value.setText(String.valueOf(settings.getInt(SettingValues.REQUEST_RESPONSE_TIMEOUT.toString(), 120)));
        clear_offline_cache_button.setText(String.format("%s (%s)", getString(R.string.clear_offline_cache), OfflineStorage.getInstance().getSize()));
    }

    private Set<Map.Entry<String, Vault>> getVaultsEntrySet() {
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) SingleTon.getTon().getExtra(SettingValues.VAULTS.toString());
        return vaults != null ? vaults.entrySet() : null;
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

    public View.OnClickListener getSSOLogoutButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage(R.string.confirm_account_logout);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AccountImporter.clearAllAuthTokens(getContext());
                        SingleAccountHelper.setCurrentAccount(getContext(), null);

                        settings.edit().remove(SettingValues.HOST.toString()).commit();
                        settings.edit().remove(SettingValues.USER.toString()).commit();
                        settings.edit().remove(SettingValues.PASSWORD.toString()).commit();

                        dialogInterface.dismiss();
                        PasswordListActivity.triggerRebirth(Objects.requireNonNull(((PasswordListActivity) getActivity())));
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            }
        };
    }

    public View.OnClickListener getSaveButtonListener() {
        return new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View view) {
                SingleTon ton = SingleTon.getTon();

                settings.edit().putBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), settings_app_start_password_switch.isChecked()).commit();

                settings.edit().putBoolean(SettingValues.ENABLE_PASSWORD_GENERATOR_SHORTCUT.toString(), settings_password_generator_shortcut_switch.isChecked()).commit();

                int length = Integer.parseInt(settings_password_generator_length_value.getText().toString());
                passwordGenerator.setLength(length > 0 ? length : 12);
                passwordGenerator.setUseUppercase(settings_password_generator_use_uppercase_switch.isChecked());
                passwordGenerator.setUseLowercase(settings_password_generator_use_lowercase_switch.isChecked());
                passwordGenerator.setUseDigits(settings_password_generator_use_digits_switch.isChecked());
                passwordGenerator.setUseSpecialChars(settings_password_generator_use_special_chars_switch.isChecked());
                passwordGenerator.setAvoidAmbiguousCharacters(settings_password_generator_avoid_ambiguous_chars_switch.isChecked());
                passwordGenerator.setRequireEveryCharType(settings_password_generator_require_every_char_type_switch.isChecked());
                passwordGenerator.applyChanges();

                settings.edit().putBoolean(SettingValues.ENABLE_CREDENTIAL_LIST_ICONS.toString(), enable_credential_list_icons_switch.isChecked()).commit();
                settings.edit().putBoolean(SettingValues.ENABLE_OFFLINE_CACHE.toString(), enable_offline_cache_switch.isChecked()).commit();

                settings.edit().putInt(SettingValues.CLEAR_CLIPBOARD_DELAY.toString(), Integer.parseInt(clear_clipboard_delay_value.getText().toString())).commit();
                Objects.requireNonNull(((PasswordListActivity) getActivity())).attachClipboardListener();

                settings.edit().putInt(SettingValues.REQUEST_CONNECT_TIMEOUT.toString(), Integer.parseInt(request_connect_timeout_value.getText().toString())).commit();
                settings.edit().putInt(SettingValues.REQUEST_RESPONSE_TIMEOUT.toString(), Integer.parseInt(request_response_timeout_value.getText().toString())).commit();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (default_autofill_vault.getSelectedItem() == null || default_autofill_vault.getSelectedItem().toString().equals(getContext().getString(R.string.automatically))) {
                        ton.removeExtra(SettingValues.AUTOFILL_VAULT_GUID.toString());
                        settings.edit().putString(SettingValues.AUTOFILL_VAULT_GUID.toString(), "").commit();
                    } else {
                        Set<Map.Entry<String, Vault>> vaults = getVaultsEntrySet();
                        if (vaults != null) {
                            for (Map.Entry<String, Vault> vault_entry : vaults) {
                                if (vault_entry.getValue().name.equals(default_autofill_vault.getSelectedItem().toString())) {
                                    ton.addExtra(SettingValues.AUTOFILL_VAULT_GUID.toString(), vault_entry.getValue().guid);
                                    settings.edit().putString(SettingValues.AUTOFILL_VAULT_GUID.toString(), vault_entry.getValue().guid).commit();

                                    Vault.getVault(getContext(), vault_entry.getValue().guid, new FutureCallback<Vault>() {
                                        @Override
                                        public void onCompleted(Exception e, Vault result) {
                                            if (e != null) {
                                                return;
                                            }
                                            Vault.updateAutofillVault(result, settings);
                                        }
                                    });

                                    break;
                                }
                            }
                        }
                    }
                }

                SettingsCache.clear();
                if (ssoAccount == null && (!KeyStoreUtils.getString(SettingValues.HOST.toString(), null).equals(settings_nextcloud_url.getText().toString()) ||
                        !KeyStoreUtils.getString(SettingValues.USER.toString(), null).equals(settings_nextcloud_user.getText().toString()) ||
                        !KeyStoreUtils.getString(SettingValues.PASSWORD.toString(), null).equals(settings_nextcloud_password.getText().toString()))) {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());

                    ton.addString(SettingValues.HOST.toString(), settings_nextcloud_url.getText().toString());
                    ton.addString(SettingValues.USER.toString(), settings_nextcloud_user.getText().toString());
                    ton.addString(SettingValues.PASSWORD.toString(), settings_nextcloud_password.getText().toString());

                    KeyStoreUtils.putStringAndCommit(SettingValues.HOST.toString(), settings_nextcloud_url.getText().toString());
                    KeyStoreUtils.putStringAndCommit(SettingValues.USER.toString(), settings_nextcloud_user.getText().toString());
                    KeyStoreUtils.putStringAndCommit(SettingValues.PASSWORD.toString(), settings_nextcloud_password.getText().toString());

                    Objects.requireNonNull(((PasswordListActivity) getActivity())).applyNewSettings(true);
                } else {
                    Objects.requireNonNull(((PasswordListActivity) getActivity())).applyNewSettings(false);
                }
            }
        };
    }

    public View.OnClickListener getClearOfflineCacheButtonListener() {
        return new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View view) {
                OfflineStorage.getInstance().clear();
                clear_offline_cache_button.setText(String.format("%s (%s)", getString(R.string.clear_offline_cache), OfflineStorage.getInstance().getSize()));
            }
        };
    }
}
