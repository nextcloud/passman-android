/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package es.wolfi.app.passman;

import android.util.Base64;
import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SJCLCrypto {
    public static native String decryptStringCpp(String cryptogram, String key) throws Exception;
    public static native String encryptStringCpp(char[] plaintext_bytearray, String key) throws Exception;

    public static String decryptString(String input, String password){
        String output = "";
        try {
            output = decrypt_ccm(new String(android.util.Base64.decode(input, Base64.DEFAULT), StandardCharsets.UTF_8), password);
        } catch (Exception e) {
            e.printStackTrace();
           try {
               Log.e("decrypt exception", "try to use the old c++ based decryption method");
               output = decryptStringCpp(input, password);
           } catch (Exception ecpp){
               ecpp.printStackTrace();
           }
        }

        return output;
    }

    public static String encryptString(String input, String password){
        String output = "";
        try {
            String encrypted = encrypt_ccm(input, password);
            output = android.util.Base64.encodeToString(encrypted.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
    }

    public static String decrypt_ccm(String input, String password) throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException, JSONException {

        JSONObject food = new JSONObject(input);

        if (food.length() <= 0){
            throw new JSONException("Error parsing the SJCL JSON");
        }

        int AES_KEY_SIZE = food.getInt("ks");
        int AES_ITERATION_COUNT = food.getInt("iter");
        int CCM_TAG_LENGTH = food.getInt("ts");
        String IV_64 = food.getString("iv");
        String SALT_64 = food.getString("salt");
        String CT_64 = food.getString("ct");

        byte[] salt = android.util.Base64.decode(SALT_64, Base64.DEFAULT);
        byte[] iv = android.util.Base64.decode(IV_64, Base64.DEFAULT);
        byte[] ct = android.util.Base64.decode(CT_64, Base64.DEFAULT);

        Security.addProvider(new BouncyCastleProvider());

        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");

        // Create SecretKeySpec
        SecretKey derived_key = getAESKeyFromPassword(password.toCharArray(), salt, AES_ITERATION_COUNT, AES_KEY_SIZE);
        SecretKeySpec secretKeySpec = new SecretKeySpec(derived_key.getEncoded(), "AES");

        if (iv.length > 13){
            int iv_len = 13;
            if (ct.length >= 1<<16) iv_len--;
            if (ct.length >= 1<<24) iv_len--;

            byte[] tmpiv = new byte[iv_len];
            for (int i = 0; i < iv_len; i++){
                tmpiv[i] = iv[i];
            }
            iv = tmpiv;
        }

        // Create GCMParameterSpec
        GCMParameterSpec parameterSpec = new GCMParameterSpec(CCM_TAG_LENGTH, iv);

        // Initialize Cipher for DECRYPT_MODE
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

        // Perform Decryption
        byte[] plainText = cipher.doFinal(ct);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    public static String encrypt_ccm(String input, String password)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException, JSONException {

        int AES_KEY_SIZE = 256;
        int AES_ITERATION_COUNT = 1000;
        int CCM_IV_LENGTH = 13;
        int CCM_TAG_LENGTH = 64;
        int CCM_SALT_LENGTH = 12;
        int plaintext_len = input.length();

        if (plaintext_len >= 1<<16) CCM_IV_LENGTH--;
        if (plaintext_len >= 1<<24) CCM_IV_LENGTH--;


        byte[] plaintext = input.getBytes();
        SecureRandom random = new SecureRandom();

        // Generate random IV
        byte[] IV = new byte[CCM_IV_LENGTH];
        random.nextBytes(IV);

        // Generate random salt
        byte[] salt = new byte[CCM_SALT_LENGTH];
        random.nextBytes(salt);


        Security.addProvider(new BouncyCastleProvider());

        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");

        // Create SecretKeySpec
        SecretKey derived_key = getAESKeyFromPassword(password.toCharArray(), salt, AES_ITERATION_COUNT, AES_KEY_SIZE);
        SecretKeySpec secretKeySpec = new SecretKeySpec(derived_key.getEncoded(), "AES");

        // Create GCMParameterSpec
        GCMParameterSpec parameterSpec = new GCMParameterSpec(CCM_TAG_LENGTH, IV);

        // Initialize Cipher for ENCRYPT_MODE
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);
        //cipher.updateAAD(new byte[]{0x01});

        // Perform Encryption
        byte[] cipherText = cipher.doFinal(plaintext);


        JSONObject food = new JSONObject();

        food.put("ct", android.util.Base64.encodeToString(cipherText, Base64.NO_WRAP | Base64.NO_PADDING));
        food.put("salt", android.util.Base64.encodeToString(salt, Base64.NO_WRAP | Base64.NO_PADDING));
        food.put("iv", android.util.Base64.encodeToString(IV, Base64.NO_WRAP | Base64.NO_PADDING));

        food.put("v", 1);
        food.put("iter", AES_ITERATION_COUNT);
        food.put("ks", AES_KEY_SIZE);
        food.put("ts", CCM_TAG_LENGTH);

        food.put("mode", "ccm");
        food.put("adata", "");
        food.put("cipher", "aes");

        return food.toString().replaceAll("\\\\/", "/");
    }

    // AES key derived from a password
    public static SecretKey getAESKeyFromPassword(char[] password, byte[] salt, int iterationCount, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, iterationCount, keyLength);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }

    static {
        System.loadLibrary("passman-lib");
    }
}
