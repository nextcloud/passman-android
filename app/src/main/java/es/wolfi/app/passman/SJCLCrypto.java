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

import android.util.Base64;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class SJCLCrypto {
    public static native String decryptStringCpp(String cryptogram, String key) throws Exception;

    public static native String encryptStringCpp(String plaintext, String key) throws Exception;

    public static String decryptString(String input, String password) throws Exception {
        String output = new String(android.util.Base64.decode(decryptStringCpp(input, password), Base64.DEFAULT), StandardCharsets.UTF_8);

        if (output.length() > 0) {
            try {
                Gson g = new Gson();
                return g.fromJson(output, String.class);
            } catch (Exception egson) {
                return output;
            }
        }

        return output;
    }

    public static String encryptString(String input, String password, boolean asJsonString) throws Exception {
        if (asJsonString) {
            Gson g = new Gson();
            input = g.toJson(input);
        }

        return encryptStringCpp(input, password);
    }

    static {
        System.loadLibrary("passman-lib");
    }
}
