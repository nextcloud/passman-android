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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.koushikdutta.async.future.FutureCallback;

import java.net.MalformedURLException;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

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
}
