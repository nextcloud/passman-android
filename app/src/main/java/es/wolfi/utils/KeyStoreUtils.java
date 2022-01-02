/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
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
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.SJCLCrypto;
import es.wolfi.app.passman.SettingValues;

public class KeyStoreUtils {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String KEY_ALIAS = "PassmanAndroidDefaultKey";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static KeyStore keyStore = null;
    private static SharedPreferences settings = null;

    public static void initialize(SharedPreferences sharedPreferences) {
        Log.d("KeyStoreUtils", "initialize");
        settings = sharedPreferences;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (keyStore == null) {
                    Log.d("KeyStoreUtils", "load KeyStore");
                    keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
                    keyStore.load(null);

                    // KEY_STORE_MIGRATION_STATE == 0 check prevents creating a KeyStore after the first app start and making already stored data unusable
                    if (!keyStore.containsAlias(KEY_ALIAS) && settings.getInt(SettingValues.KEY_STORE_MIGRATION_STATE.toString(), 0) == 0) {
                        Log.d("KeyStoreUtils", "generate new encryption key");
                        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                        keyGenerator.init(
                                new KeyGenParameterSpec.Builder(KEY_ALIAS,
                                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                        .setRandomizedEncryptionRequired(false)
                                        .build());
                        SecretKey key = keyGenerator.generateKey();
                        keyStore.setKeyEntry(KEY_ALIAS, key, null, null);

                        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
                        byte[] encryptionKeyBytes = new byte[4096];
                        random.nextBytes(encryptionKeyBytes);

                        String encryptionKeyString = new String(Hex.encodeHex(DigestUtils.sha512(encryptionKeyBytes)));
                        String encryptedEncryptionKeyString = encryptKey(encryptionKeyString);
                        settings.edit().putString(SettingValues.KEY_STORE_ENCRYPTION_KEY.toString(), encryptedEncryptionKeyString).commit();
                    }
                    migrateSharedPreferences();
                }
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateException e) {
            e.printStackTrace();
        }
    }

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

    private static java.security.Key getSecretKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return keyStore.getKey(KEY_ALIAS, null);
    }

    public static String encryptKey(String input) {
        try {
            if (input != null && keyStore != null && keyStore.containsAlias(KEY_ALIAS)) {
                Cipher c = Cipher.getInstance(AES_MODE);
                byte[] iv = generateIv();
                c.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
                byte[] encryptedBytes = c.doFinal(input.getBytes(StandardCharsets.UTF_8));

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(iv);
                outputStream.write(encryptedBytes);
                return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String decryptKey(String encrypted) {
        try {
            if (encrypted != null && keyStore != null && keyStore.containsAlias(KEY_ALIAS) && encrypted.length() >= IV_LENGTH) {
                byte[] decoded = Base64.decode(encrypted, Base64.DEFAULT);
                byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
                Cipher c = Cipher.getInstance(AES_MODE);
                c.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
                byte[] decrypted = c.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);

                return new String(decrypted);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String encrypt(String input) {
        if (input != null && keyStore != null) {
            String encryptedEncryptionKey = settings.getString(SettingValues.KEY_STORE_ENCRYPTION_KEY.toString(), null);
            String encryptionKey = decryptKey(encryptedEncryptionKey);

            if (encryptionKey != null) {
                try {
                    return SJCLCrypto.encryptString(input, encryptionKey, true);
                } catch (Exception e) {
                    Log.e("KeyStoreUtils encrypt", e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public static String decrypt(String encrypted, String fallback) {
        if (encrypted != null && keyStore != null) {
            String encryptedEncryptionKey = settings.getString(SettingValues.KEY_STORE_ENCRYPTION_KEY.toString(), null);
            String encryptionKey = decryptKey(encryptedEncryptionKey);

            if (encryptionKey != null) {
                try {
                    return SJCLCrypto.decryptString(encrypted, encryptionKey);
                } catch (Exception e) {
                    Log.e("KeyStoreUtils decrypt", e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return fallback;
    }

    public static String getString(String key, String fallback) {
        return decrypt(settings.getString(key, null), fallback);
    }

    public static void putString(String key, String value) {
        settings.edit().putString(key, encrypt(value)).apply();
    }

    public static boolean putStringAndCommit(String key, String value) {
        return settings.edit().putString(key, encrypt(value)).commit();
    }
}
