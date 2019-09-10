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


import android.util.Log;
import android.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.JSONObject;

import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SJCLCrypto {
    //public static native String decryptString(String cryptogram, String key) throws Exception;

    private static final String ALGORITHM = "AES/CCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 64;
    private static final int SALT_LENGTH_BYTE = 8;
    private static final int ITER = 1000;
    private static final int KEY_LENGTH_BIT = 256;

    /*static {
        System.loadLibrary("passman-lib");
    }*/

    public static JSONObject encryptString(String toencrypt, String stringkey)
    {
        try {

            SecureRandom random = new SecureRandom();

            byte[] ivBytes;
            byte bytes[] = new byte[SALT_LENGTH_BYTE];
            random.nextBytes(bytes);
            byte[] saltBytes = bytes;

            PBEKeySpec keySpec = new PBEKeySpec(
                    stringkey.toCharArray(),
                    saltBytes,
                    ITER,
                    KEY_LENGTH_BIT);

            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);

            SecretKeySpec secretSpec = new SecretKeySpec(secretKey.getEncoded(),"AES");

            Cipher aes = Cipher.getInstance(ALGORITHM);

            aes.init(Cipher.ENCRYPT_MODE,secretSpec);

            ivBytes = aes.getIV();

            byte[] encryptedTextBytes = aes.doFinal(toencrypt.getBytes("UTF-8"));

            String saltString = Base64.encodeToString(saltBytes,Base64.NO_WRAP);
            String ivString = Base64.encodeToString(ivBytes,Base64.NO_WRAP );
            String encryptedString = Base64.encodeToString(encryptedTextBytes,Base64.NO_WRAP);

            JSONObject jsonResult = new JSONObject();

            jsonResult.put("iv", ivString);
            jsonResult.put("v", 1);
            jsonResult.put("iter", ITER);
            jsonResult.put("ks", KEY_LENGTH_BIT);
            jsonResult.put("ts", TAG_LENGTH_BIT);
            jsonResult.put("mode", "ccm");
            jsonResult.put("adata", "");
            jsonResult.put("cipher", "aes");
            jsonResult.put("salt", saltString);
            jsonResult.put("ct", encryptedString);

            return jsonResult;

        }
        catch (Exception ex)
        {
            Log.d("SJCL", "Could not encrypt: " + ex.toString());
        }
        return null;
    }

    public static String decryptString(String todecrypt, String stringkey)
    {
        try {

            if (todecrypt == null || stringkey == null) {
                return null;
            }

            // Retrieve the parameters

            String jsonString = new String(Base64.decode(todecrypt, Base64.DEFAULT));

            JSONObject jsonResult = new JSONObject(jsonString);

            byte[] salt = Base64.decode(jsonResult.getString("salt"), Base64.DEFAULT);
            byte[] iv = Base64.decode(jsonResult.getString("iv"), Base64.DEFAULT);


            // TODO: Check the iv is at least 13 bytes
            // and figure out what to do if its not.
            // For now, just trim the nonce to 13 bytes
            byte[] strippedIV = Arrays.copyOf(iv, 13);


            byte[] ct = Base64.decode(jsonResult.getString("ct"), Base64.DEFAULT);

            int KEY_LENGTH_BIT_LOCAL = jsonResult.getInt("ks");
            int TAG_LENGTH_BIT_LOCAL = jsonResult.getInt("ts");

            PBEKeySpec keySpec = new PBEKeySpec(
                    stringkey.toCharArray(),
                    salt,
                    ITER,
                    KEY_LENGTH_BIT_LOCAL);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT_LOCAL, strippedIV);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);

            SecretKeySpec secretSpec = new SecretKeySpec(secretKey.getEncoded(),"AES");

            Cipher aes = Cipher.getInstance(ALGORITHM);

            aes.init(Cipher.DECRYPT_MODE, secretSpec, gcmParameterSpec);

            byte[] decryptedTextBytes = aes.doFinal(ct);

            String stringDecrypted = new String(decryptedTextBytes);

            try {
                JsonParser jsonParser = new JsonParser();
                JsonElement jsonElement = jsonParser.parse(stringDecrypted);
                return jsonElement.getAsString();
            }
            catch (UnsupportedOperationException ex)            {
                return stringDecrypted;
            }
        }
        catch (Exception ex)
        {
            Log.d("SJCL", "Could not decrypt: " + ex.toString());
        }
        return null;
    }
}
