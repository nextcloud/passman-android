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
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.koushikdutta.async.future.FutureCallback;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Vault;

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
        ton = SingleTon.getTon();

        try {
            String host = settings.getString(SettingValues.HOST.toString(), null);
            URL uri = new URL(host);

            String hostonly = uri.getHost();
            input_host.setText(hostonly);

            String protocolonly = uri.getProtocol();
            input_protocol.setPrompt(protocolonly.toUpperCase());
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        }

        input_user.setText(settings.getString(SettingValues.USER.toString(), null));
        input_pass.setText(settings.getString(SettingValues.PASSWORD.toString(), null));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
     * @param c
     * @param cb
     */
    public static void launch(Context c, ICallback cb) {
        SingleTon.getTon().addCallback(CallbackNames.LOGIN.toString(), cb);
        Intent i = new Intent(c, LoginActivity.class);
        c.startActivity(i);
    }
}
