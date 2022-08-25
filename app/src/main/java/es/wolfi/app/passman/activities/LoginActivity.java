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
package es.wolfi.app.passman.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.koushikdutta.async.future.FutureCallback;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.Constants;
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
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Core;
import es.wolfi.utils.KeyStoreUtils;

public class LoginActivity extends AppCompatActivity {
    public final static String LOG_TAG = "LoginActivity";

    @BindView(R.id.protocol)
    Spinner input_protocol;
    @BindView(R.id.host)
    EditText input_host;
    @BindView(R.id.user)
    EditText input_user;
    @BindView(R.id.pass)
    EditText input_pass;
    @BindView(R.id.next)
    Button bt_next;

    SharedPreferences settings;
    SingleTon ton;
    boolean isLegacyOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("LoginActivity", "in onCreate");

        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        new OfflineStorage(getBaseContext());

        if (!isNextcloudFilesAppInstalled(this)) {
            isLegacyOnly = true;
            loadLegacyLogin();
        }
    }

    @OnClick(R.id.load_legacy_login_button)
    public void loadLegacyLogin() {
        hideLoginOptions();

        ImageView login_options_logo = findViewById(R.id.login_options_logo);
        login_options_logo.setVisibility(View.INVISIBLE);

        LinearLayout content_legacy_login = findViewById(R.id.content_legacy_login);
        content_legacy_login.setVisibility(View.VISIBLE);

        EditText hostForm = findViewById(R.id.host);
        hostForm.requestFocus();

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

    @OnClick(R.id.load_sso_login_button)
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
        LinearLayout content_legacy_login = findViewById(R.id.content_legacy_login);
        content_legacy_login.setVisibility(View.INVISIBLE);

        LinearLayout login_options = findViewById(R.id.login_options);
        login_options.setVisibility(View.VISIBLE);
        ImageView login_options_logo = findViewById(R.id.login_options_logo);
        login_options_logo.setVisibility(View.VISIBLE);
    }

    private void hideLoginOptions() {
        LinearLayout login_options = findViewById(R.id.login_options);
        login_options.setVisibility(View.INVISIBLE);
    }

    private static boolean isNextcloudFilesAppInstalled(Context context) {
        List<String> APPS = Arrays.asList(Constants.PACKAGE_NAME_PROD, Constants.PACKAGE_NAME_DEV);

        boolean returnValue = false;
        PackageManager pm = context.getPackageManager();
        for (String app : APPS) {
            try {
                PackageInfo pi = pm.getPackageInfo(app, PackageManager.GET_ACTIVITIES);
                // check if Nextcloud Files App version with the required PATCH request fix is installed
                if ((pi.versionCode >= 30180052 && pi.packageName.equals("com.nextcloud.client")) ||
                        pi.versionCode >= 20211027 && pi.packageName.equals("com.nextcloud.android.beta")) {
                    returnValue = true;
                    break;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return returnValue;
    }

    @OnClick(R.id.next)
    public void onNextClick() {
        Log.e("Login", "begin");
        final String protocol = input_protocol.getSelectedItem().toString().toLowerCase();
        final String host = protocol + "://" + input_host.getText().toString().trim();
        final String user = input_user.getText().toString().trim();
        final String pass = input_pass.getText().toString();

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
                    SingleAccountHelper.setCurrentAccount(l_context, account.name);

                    // Get the "default" account
                    SingleSignOnAccount ssoAccount;
                    try {
                        ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(l_context);
                    } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
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
        LinearLayout login_options = findViewById(R.id.login_options);
        if (login_options.getVisibility() == View.INVISIBLE && !isLegacyOnly) {
            showLoginOptions();
        } else {
            PasswordListActivity.running = false;
            super.onBackPressed();
        }
    }
}
