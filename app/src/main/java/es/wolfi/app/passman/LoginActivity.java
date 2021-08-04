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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.wolfi.passman.API.Core;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            AccountImporter.pickNewAccount(this);
            Log.w("SSO@LoginActivity", "try AccountImporter was successful");
        } catch (NextcloudFilesAppNotInstalledException e1) {
            //UiExceptionManager.showDialogForException(this, e1);
            Log.w("SSO@LoginActivity", "Nextcloud app is not installed. Cannot choose account. Use legacy login method.");
            //e1.printStackTrace();
            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);

            settings = PreferenceManager.getDefaultSharedPreferences(this);
            ton = SingleTon.getTon();

            try {
                String host = settings.getString(SettingValues.HOST.toString(), null);
                URL uri = new URL(host);

                String hostonly = uri.getHost();
                input_host.setText(hostonly);

                String protocolonly = uri.getProtocol();
                input_protocol.setPrompt(protocolonly.toUpperCase());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), getString(R.string.wrongNCUrl), Toast.LENGTH_LONG).show();
            }

            input_user.setText(settings.getString(SettingValues.USER.toString(), null));
            input_pass.setText(settings.getString(SettingValues.PASSWORD.toString(), null));

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        } catch (AndroidGetAccountsPermissionNotGranted e2) {
            AccountImporter.requestAndroidAccountPermissionsAndPickAccount(this);
        }
    }

    @OnClick(R.id.next)
    public void onNextClick() {
        Log.e("Login", "begin");
        final String protocol = input_protocol.getSelectedItem().toString().toLowerCase();
        final String host = protocol + "://" + input_host.getText().toString();
        final String user = input_user.getText().toString();
        final String pass = input_pass.getText().toString();

        final Activity c = this;

        ton.addString(SettingValues.HOST.toString(), host);
        ton.addString(SettingValues.USER.toString(), user);
        ton.addString(SettingValues.PASSWORD.toString(), pass);

        Core.checkLogin(this, true, new FutureCallback<Boolean>() {
            @Override
            public void onCompleted(Exception e, Boolean result) {
                if (result) {
                    settings.edit()
                            .putString(SettingValues.HOST.toString(), host)
                            .putString(SettingValues.USER.toString(), user)
                            .putString(SettingValues.PASSWORD.toString(), pass)
                            .apply();

                    ton.getCallback(CallbackNames.LOGIN.toString()).onTaskFinished();
                    c.finish();
                } else {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());
                }

            }
        });
    }

    /**
     * Displays this activity
     *
     * @param c
     * @param cb
     */
    public static void launch(Context c, ICallback cb) {
        SingleTon.getTon().addCallback(CallbackNames.LOGIN.toString(), cb);
        Intent i = new Intent(c, LoginActivity.class);
        c.startActivity(i);
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
                    SingleSignOnAccount ssoAccount = null;
                    try {
                        ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(l_context);
                    } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
                        UiExceptionManager.showDialogForException(l_context, e);
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

                                        ton.getCallback(CallbackNames.LOGIN.toString()).onTaskFinished();
                                        //c.finish();
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
            e.printStackTrace();
        }
    }
}
