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

import java.security.AlgorithmParameters;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import org.json.JSONObject;

import javax.crypto.spec.SecretKeySpec;

public class SJCLCrypto {
    public static native String decryptString(String cryptogram, String key) throws Exception;

    private static final String ALGORITHM = "AES/CCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 64;
    private static final int IV_LENGTH_BYTE = 16;
    private static final int SALT_LENGTH_BYTE = 8;
    private static final int ITER = 1000;
    private static final int KEY_LENGTH_BIT = 256;

    static {
        System.loadLibrary("passman-lib");
    }

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

            Provider[] providers = Security.getProviders();
            for (int i = 0; i < providers.length; i++){
                Log.e("SJCL", "Name: " + providers[i].getName() + " Version: " + providers[i].getVersion());
            }

            Cipher aes = Cipher.getInstance(ALGORITHM);

            aes.init(Cipher.ENCRYPT_MODE,secretSpec);

            AlgorithmParameters algorithmParameters = aes.getParameters();
            ivBytes = algorithmParameters.getParameterSpec(IvParameterSpec.class).getIV();

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
}
