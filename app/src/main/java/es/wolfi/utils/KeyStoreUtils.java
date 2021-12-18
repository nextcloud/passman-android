/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.SettingValues;

/**
 * Takes care of data encryption in SharedPreferences.
 * This is an optional feature, but should be used for all user data.
 * <p>
 * Use this class directly only if you don't want to make use of the OfflineStorage class!
 */
public class KeyStoreUtils {

    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String KEY_ALIAS = "PassmanAndroidDefaultKey";
    private static final int IV_LENGTH = 12;
    private static KeyStore keyStore = null;
    private static SharedPreferences settings = null;

    /**
     * Call initialize() at the top of each activity you want to use encrypted data stored in Androids SharedPreferences.
     * Example usage: KeyStoreUtils.initialize(SharedPreferences settings);
     *
     * @param sharedPreferences SharedPreferences
     */
    public static void initialize(SharedPreferences sharedPreferences) {
        Log.d("KeyStoreUtils", "initialize");
        settings = sharedPreferences;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (keyStore == null) {
                    Log.d("KeyStoreUtils", "load KeyStore");
                    keyStore = KeyStore.getInstance(AndroidKeyStore);
                    keyStore.load(null);

                    // KEY_STORE_MIGRATION_STATE == 0 check prevents creating a KeyStore after the first app start and making already stored data unusable
                    if (!keyStore.containsAlias(KEY_ALIAS) && settings.getInt(SettingValues.KEY_STORE_MIGRATION_STATE.toString(), 0) == 0) {
                        Log.d("KeyStoreUtils", "generate new key");
                        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
                        keyGenerator.init(
                                new KeyGenParameterSpec.Builder(KEY_ALIAS,
                                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                        .setRandomizedEncryptionRequired(false)
                                        .build());
                        keyGenerator.generateKey();
                    }
                    migrateSharedPreferences();
                }
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called with any KeyStoreUtils.initialize().
     * Used to automatically encrypt unencrypted stored data from older Passman versions.
     */
    private static void migrateSharedPreferences() {
        int originalMigrationState = settings.getInt(SettingValues.KEY_STORE_MIGRATION_STATE.toString(), 0);
        int currentMigrationState = originalMigrationState;

        if (currentMigrationState < 1) {
            // first app start and first KeyStoreUtils usage migration
            Log.d("KeyStoreUtils", "run initial local storage encryption migration");

            KeyStoreUtils.putStringAndCommit(SettingValues.HOST.toString(), settings.getString(SettingValues.HOST.toString(), null));
            KeyStoreUtils.putStringAndCommit(SettingValues.USER.toString(), settings.getString(SettingValues.USER.toString(), null));
            KeyStoreUtils.putStringAndCommit(SettingValues.PASSWORD.toString(), settings.getString(SettingValues.PASSWORD.toString(), null));
            KeyStoreUtils.putStringAndCommit(SettingValues.AUTOFILL_VAULT.toString(), settings.getString(SettingValues.AUTOFILL_VAULT.toString(), ""));
            KeyStoreUtils.putStringAndCommit(SettingValues.OFFLINE_STORAGE.toString(), settings.getString(SettingValues.OFFLINE_STORAGE.toString(), OfflineStorage.EMPTY_STORAGE_STRING));

            currentMigrationState++;
        }

        if (originalMigrationState != currentMigrationState) {
            settings.edit().putInt(SettingValues.KEY_STORE_MIGRATION_STATE.toString(), currentMigrationState).commit();
        }
    }

    private static byte[] generateIv() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    private static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    private static java.security.Key getSecretKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return keyStore.getKey(KEY_ALIAS, null);
    }

    /**
     * Encrypt data using the Android KeyStore feature (to store it in SharedPreferences).
     *
     * @param input String
     * @return String - encrypted input
     */
    public static String encrypt(String input) {
        try {
            if (input != null && keyStore != null && keyStore.containsAlias(KEY_ALIAS)) {
                Cipher c = Cipher.getInstance(AES_MODE);
                byte[] iv = generateIv();
                String ivHex = byteArrayToHexString(iv);
                c.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv));
                byte[] encodedBytes = c.doFinal(input.getBytes(StandardCharsets.UTF_8));

                return ivHex + Base64.encodeToString(encodedBytes, Base64.DEFAULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return input;
    }

    /**
     * Decrypt data using the Android KeyStore feature (to load encrypted data from SharedPreferences).
     *
     * @param encrypted String
     * @param fallback  String - use this as fallback if the decryption fails
     * @return String - decrypted data
     */
    public static String decrypt(String encrypted, String fallback) {
        try {
            if (encrypted != null && keyStore != null && keyStore.containsAlias(KEY_ALIAS) && encrypted.length() >= IV_LENGTH * 2) {
                String ivHex = encrypted.substring(0, IV_LENGTH * 2);
                byte[] decoded = Base64.decode(encrypted.substring(IV_LENGTH * 2), Base64.DEFAULT);
                Cipher c = Cipher.getInstance(AES_MODE);
                c.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, hexStringToByteArray(ivHex)));
                byte[] decrypted = c.doFinal(decoded);

                return new String(decrypted);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fallback;
    }

    /**
     * Decrypt data from SharedPreferences and return it as String.
     * Replace settings.getString() with KeyStoreUtils.getString().
     *
     * @param key      String
     * @param fallback String
     * @return String
     */
    public static String getString(String key, String fallback) {
        return decrypt(settings.getString(key, null), fallback);
    }

    /**
     * Encrypt data and store it in SharedPreferences.
     * Replace settings.edit().putString() with KeyStoreUtils.putString().
     * Without the explicit commit() call, the backend will take care of storing the data asynchronously (recommended)
     *
     * @param key   String
     * @param value String
     */
    public static void putString(String key, String value) {
        settings.edit().putString(key, encrypt(value)).apply();
    }

    /**
     * Encrypt data and store it in SharedPreferences.
     * Replace settings.edit().putString().commit() with KeyStoreUtils.putStringAndCommit().
     * With the explicit commit() call, data will be stored synchronously (not recommended for the most cases)
     *
     * @param key   String
     * @param value String
     * @return boolean - Returns true if the value was successfully written to persistent storage.
     */
    public static boolean putStringAndCommit(String key, String value) {
        return settings.edit().putString(key, encrypt(value)).commit();
    }
}
