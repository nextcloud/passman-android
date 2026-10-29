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

package es.wolfi.app.passman.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import es.wolfi.app.passman.SettingValues;
import es.wolfi.utils.DeviceCredentialAuthentication;

@RunWith(AndroidJUnit4.class)
public class DeviceCredentialAuthenticationTest {
    private SharedPreferences settings;

    @Before
    public void createSettings() {
        // separate preferences, the settings of the app stay untouched
        settings = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("device_credential_authentication_test", Context.MODE_PRIVATE);
        settings.edit().clear().commit();
    }

    @After
    public void clearSettings() {
        settings.edit().clear().commit();
    }

    @Test
    public void supportedFromAndroid11() {
        assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R, DeviceCredentialAuthentication.isSupported());
    }

    @Test
    public void disabledByDefault() {
        assertFalse(DeviceCredentialAuthentication.isEnabled(settings));
    }

    @Test
    public void enabledBySettingIfSupported() {
        settings.edit().putBoolean(SettingValues.APP_START_DEVICE_CREDENTIAL_ONLY.toString(), true).commit();
        assertEquals(DeviceCredentialAuthentication.isSupported(), DeviceCredentialAuthentication.isEnabled(settings));
    }

    @Test
    public void promptAllowsDeviceCredentialOnly() {
        assumeTrue(DeviceCredentialAuthentication.isSupported());

        BiometricPrompt.PromptInfo promptInfo = DeviceCredentialAuthentication.createPromptInfo("title", "subtitle");
        // neither BIOMETRIC_STRONG nor BIOMETRIC_WEAK
        assertEquals(BiometricManager.Authenticators.DEVICE_CREDENTIAL, promptInfo.getAllowedAuthenticators());
    }

    @Test
    public void promptUnsupportedBeforeAndroid11() {
        assumeFalse(DeviceCredentialAuthentication.isSupported());

        // androidx.biometric refuses it, a biometric could be offered instead of the device credential
        assertThrows(IllegalArgumentException.class, () -> DeviceCredentialAuthentication.createPromptInfo("title", "subtitle"));
    }
}
