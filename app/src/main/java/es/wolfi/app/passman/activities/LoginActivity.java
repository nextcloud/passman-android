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
package es.wolfi.app.passman.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.koushikdutta.async.future.FutureCallback;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;

import java.net.MalformedURLException;
import java.net.URL;

import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.databinding.ActivityLoginBinding;
import es.wolfi.app.passman.databinding.ContentLegacyLoginBinding;
import es.wolfi.passman.API.Core;
import es.wolfi.utils.KeyStoreUtils;
import es.wolfi.utils.SSOUtils;

public class LoginActivity extends AppCompatActivity {
    public final static String LOG_TAG = "LoginActivity";

    Spinner input_protocol;
    EditText input_host;
    EditText input_user;
    EditText input_pass;
    Button bt_next;

    LinearLayout contentLegacyLogin;
    ImageView loginOptionsLogo;
    LinearLayout loginOptions;

    SharedPreferences settings;
    SingleTon ton;
    boolean isLegacyOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("LoginActivity", "in onCreate");

        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        contentLegacyLogin = binding.contentLegacyLoginInclude.contentLegacyLoginLayout;

        input_protocol = binding.contentLegacyLoginInclude.protocol;
        input_host = binding.contentLegacyLoginInclude.host;
        input_user = binding.contentLegacyLoginInclude.user;
        input_pass = binding.contentLegacyLoginInclude.pass;
        bt_next = binding.contentLegacyLoginInclude.next;

        loginOptionsLogo = binding.loginOptionsLogo;
        loginOptions = binding.loginOptions;

        new OfflineStorage(getBaseContext());

        if (!SSOUtils.isNextcloudFilesAppInstalled(this)) {
            isLegacyOnly = true;
            loadLegacyLogin();
        }

        binding.loadLegacyLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadLegacyLogin();
            }
        });
        binding.loadSsoLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadSSOLogin();
            }
        });
        binding.contentLegacyLoginInclude.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextClick();
            }
        });
    }

    public void loadLegacyLogin() {
        hideLoginOptions();

        loginOptionsLogo.setVisibility(View.INVISIBLE);
        contentLegacyLogin.setVisibility(View.VISIBLE);
        input_host.requestFocus();

        bt_next.setOnFocusChangeListener((View view, boolean buttonEnabled) -> {
            if (buttonEnabled) {
                Log.i(LOG_TAG, "Next button focus enabled");
                onNextClick();
            }
        });

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        KeyStoreUtils.initialize(settings);
        ton = SingleTon.getTon();

        try {
            String host = KeyStoreUtils.getString(SettingValues.HOST.toString(), null);
            if (host != null) {
                URL uri = new URL(host);

                String hostonly = uri.getHost();
                input_host.setText(hostonly);

                String protocolonly = uri.getProtocol();
                input_protocol.setPrompt(protocolonly.toUpperCase());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.wrongNCUrl), Toast.LENGTH_LONG).show();
        }

        input_user.setText(KeyStoreUtils.getString(SettingValues.USER.toString(), null));
        input_pass.setText(KeyStoreUtils.getString(SettingValues.PASSWORD.toString(), null));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void loadSSOLogin() {
        hideLoginOptions();

        try {
            AccountImporter.pickNewAccount(this);
            Log.w("SSO@LoginActivity", "try AccountImporter was successful");
        } catch (NextcloudFilesAppNotInstalledException e1) {
            UiExceptionManager.showDialogForException(this, e1);
            Log.w("SSO@LoginActivity", "Nextcloud app is not installed. Cannot choose account. Use legacy login method.");

            loadLegacyLogin();
        } catch (AndroidGetAccountsPermissionNotGranted e2) {
            AccountImporter.requestAndroidAccountPermissionsAndPickAccount(this);
        }
    }

    private void showLoginOptions() {
        contentLegacyLogin.setVisibility(View.INVISIBLE);

        loginOptions.setVisibility(View.VISIBLE);
        loginOptionsLogo.setVisibility(View.VISIBLE);
    }

    private void hideLoginOptions() {
        loginOptions.setVisibility(View.INVISIBLE);
    }

    public void onNextClick() {
        Log.e("Login", "begin");
        final String protocol = input_protocol.getSelectedItem().toString().toLowerCase();
        final String hostInput = input_host.getText().toString().trim();
        final String host = protocol + "://" + hostInput;
        final String user = input_user.getText().toString().trim();
        final String pass = input_pass.getText().toString();

        if (hostInput.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Log.e("Login", "abort");
            Toast.makeText(getApplicationContext(), getString(R.string.wrongNCSettings), Toast.LENGTH_SHORT).show();
            return;
        }

        ton.addString(SettingValues.HOST.toString(), host);
        ton.addString(SettingValues.USER.toString(), user);
        ton.addString(SettingValues.PASSWORD.toString(), pass);

        Core.checkLogin(this, true, new FutureCallback<Boolean>() {
            @Override
            public void onCompleted(Exception e, Boolean loginSuccessful) {
                if (loginSuccessful) {
                    KeyStoreUtils.putString(SettingValues.HOST.toString(), host);
                    KeyStoreUtils.putString(SettingValues.USER.toString(), user);
                    KeyStoreUtils.putString(SettingValues.PASSWORD.toString(), pass);

                    setResult(RESULT_OK);
                    LoginActivity.this.finish();
                } else {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Context c = this;
        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data, this, new AccountImporter.IAccountAccessGranted() {

                @Override
                public void accountAccessGranted(SingleSignOnAccount account) {
                    Context l_context = getApplicationContext();

                    // As this library supports multiple accounts we created some helper methods if you only want to use one.
                    // The following line stores the selected account as the "default" account which can be queried by using
                    // the SingleAccountHelper.getCurrentSingleSignOnAccount(context) method
                    SingleAccountHelper.commitCurrentAccount(l_context, account.name);

                    // Get the "default" account
                    SingleSignOnAccount ssoAccount;
                    try {
                        ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(l_context);
                    } catch (NextcloudFilesAppAccountNotFoundException |
                             NoCurrentAccountSelectedException e) {
                        UiExceptionManager.showDialogForException(l_context, e);
                        return;
                    }

                    SingleTon ton = SingleTon.getTon();
                    ton.addString(SettingValues.HOST.toString(), ssoAccount.url);
                    ton.addString(SettingValues.USER.toString(), ssoAccount.userId);
                    ton.addString(SettingValues.PASSWORD.toString(), ssoAccount.token);

                    SingleSignOnAccount finalSsoAccount = ssoAccount;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Core.checkLogin(c, true, new FutureCallback<Boolean>() {
                                @Override
                                public void onCompleted(Exception e, Boolean result) {
                                    if (result) {
                                        settings.edit()
                                                .putString(SettingValues.HOST.toString(), finalSsoAccount.url)
                                                .putString(SettingValues.USER.toString(), finalSsoAccount.userId)
                                                .putString(SettingValues.PASSWORD.toString(), finalSsoAccount.token)
                                                .apply();

                                        setResult(RESULT_OK);
                                        LoginActivity.this.finish();
                                    } else {
                                        ton.removeString(SettingValues.HOST.toString());
                                        ton.removeString(SettingValues.USER.toString());
                                        ton.removeString(SettingValues.PASSWORD.toString());
                                    }
                                }
                            });
                        }
                    });
                }
            });
        } catch (AccountImportCancelledException e) {
            showLoginOptions();
        }
    }

    @Override
    public void onBackPressed() {
        if (loginOptions.getVisibility() == View.INVISIBLE && !isLegacyOnly) {
            showLoginOptions();
        } else {
            PasswordListActivity.running = false;
            super.onBackPressed();
        }
    }
}
