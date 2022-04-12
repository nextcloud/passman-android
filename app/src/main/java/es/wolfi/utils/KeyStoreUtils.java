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
import es.wolfi.app.passman.SettingsCache;

/**
 * Takes care of data encryption in SharedPreferences.
 * This is an optional feature, but should be used for all user data.
 * <p>
 * Use this class directly only if you don't want to make use of the OfflineStorage class!
 */
public class KeyStoreUtils {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String KEY_ALIAS = "PassmanAndroidDefaultKey";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static KeyStore keyStore = null;
    private static SharedPreferences settings = null;

    /**
     * Call initialize() at the top of each activity you want to use encrypted data stored in Androids SharedPreferences.
     * Example usage: KeyStoreUtils.initialize(SharedPreferences settings);
     * <p>
     * If the Android KeyStore does not contain the required KEY_ALIAS (usually only at the first app start) an encryption key
     * for AES/GCM will be generated and stored in the KeyStore (which also protects it from any direct access).
     * This AES/GCM key is the encryption key for a random generated password which is used to encrypt the user data with the known SJCL.cpp lib.
     * This is much faster than using any java crypto implementation to encrypt/decrypt user data like data from the OfflineStorage class.
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

                        keyGenerator.generateKey();

                        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
                        byte[] encryptionKeyBytes = new byte[4096];
                        random.nextBytes(encryptionKeyBytes);

                        String encryptionKeyString = new String(Hex.encodeHex(DigestUtils.sha512(encryptionKeyBytes)));
                        String encryptedEncryptionKeyString = encryptKey(encryptionKeyString);
                        settings.edit().putString(SettingValues.KEY_STORE_ENCRYPTION_KEY.toString(), encryptedEncryptionKeyString).commit();
                    }
                    migrateSharedPreferences();
                }
            } else {
                Log.d("KeyStoreUtils", "not supported");

                // since offline cache is enabled by default this code disables it for devices with Android < API 23
                boolean enableOfflineCache = settings.getBoolean(SettingValues.ENABLE_OFFLINE_CACHE.toString(), false);
                if (!enableOfflineCache) {
                    settings.edit().putBoolean(SettingValues.ENABLE_OFFLINE_CACHE.toString(), false).commit();
                    SettingsCache.clear();
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
            // already saved vault password will not be migrated and have to be reentered
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

    /**
     * Generates the initialisation vector for the encryption of the user data's encryption key.
     *
     * @return byte[]
     * @throws NoSuchAlgorithmException
     */
    private static byte[] generateIv() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * Returns a Key instance to encrypt/decrypt the data encryption key.
     *
     * @return java.security.Key instance from Android KeyStore
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    private static java.security.Key getSecretKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return keyStore.getKey(KEY_ALIAS, null);
    }

    /**
     * Encrypts the user data's encryption key.
     * Should be called only once from initialize().
     *
     * @param input String plain encryption key
     * @return String|null encrypted encryption key or null if the encryption failed or the used KeyStore was not initialized
     */
    private static String encryptKey(String input) {
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

    /**
     * Decrypts the user data's encryption key.
     *
     * @param encrypted String encrypted encryption key
     * @return String|null plain encryption key or null if the decryption failed or the used KeyStore was not initialized
     */
    private static String decryptKey(String encrypted) {
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

    /**
     * Encrypt data using the SJCLCrypto library (to store it in SharedPreferences).
     *
     * @param input String
     * @return String encrypted data or original input data if encryption is not enabled or failed
     */
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

        // seems like KeyStore is not enabled / supported (KeyStore requires at least Android API 23)
        return input;
    }

    /**
     * Decrypt data using the SJCLCrypto library (to load encrypted data from SharedPreferences).
     *
     * @param encrypted String
     * @return String decrypted data or original input data if decryption is not enabled or failed
     */
    public static String decrypt(String encrypted) {
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

        // seems like KeyStore is not enabled / supported (KeyStore requires at least Android API 23)
        return encrypted;
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
        return decrypt(settings.getString(key, fallback));
    }

    /**
     * Encrypt data and store it in SharedPreferences.
     * Replace settings.edit().putString() with KeyStoreUtils.putString().
     * Without the explicit commit() call, the backend will take care of storing the data asynchronously (recommended).
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
     * With the explicit commit() call, data will be stored synchronously (not recommended for the most cases).
     *
     * @param key   String
     * @param value String
     * @return boolean returns true if the value was successfully written to persistent storage
     */
    public static boolean putStringAndCommit(String key, String value) {
        return settings.edit().putString(key, encrypt(value)).commit();
    }
}
