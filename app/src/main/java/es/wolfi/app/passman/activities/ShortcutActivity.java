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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import es.wolfi.app.passman.R;
import es.wolfi.utils.PasswordGenerator;

public class ShortcutActivity extends AppCompatActivity {
    public final static String LOG_TAG = "ShortcutActivity";
    public final static String GENERATE_PASSWORD_ID = "es.wolfi.app.passman.generate_password";
    public final static String GENERATE_PASSWORD_INTENT_ACTION = "custom.actions.intent.GENERATE_PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getAction().equals(GENERATE_PASSWORD_INTENT_ACTION)) {
            generatePassword();
        }

        finish();
    }

    protected void generatePassword() {
        String password = new PasswordGenerator(getApplicationContext()).generateRandomPassword();

        if (password != null && password.length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("generated_password", password);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getApplicationContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
    }
}
