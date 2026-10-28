/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2026, Timo Triebensky (timo@binsky.org)
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

package es.wolfi.utils;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import es.wolfi.app.passman.SettingValues;

/**
 * App start authentication with the device PIN, pattern or password only, biometrics are not offered.
 */
public class DeviceCredentialAuthentication {

    /**
     * Before Android 11 the system may offer a biometric instead of the device credential, there is no way to exclude biometrics.
     *
     * @return whether the system supports an authentication with the device credential only
     */
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    /**
     * @param settings the shared preferences of the app
     * @return whether the app start authentication should use the device credential only
     */
    public static boolean isEnabled(SharedPreferences settings) {
        return isSupported() && settings.getBoolean(SettingValues.APP_START_DEVICE_CREDENTIAL_ONLY.toString(), false);
    }

    /**
     * Only call it if {@link #isSupported()}, the prompt can't be built on older Android versions.
     *
     * @param title    the title of the prompt
     * @param subtitle the subtitle of the prompt
     * @return the prompt info that only allows the device credential
     */
    public static BiometricPrompt.PromptInfo createPromptInfo(String title, String subtitle) {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }
}
