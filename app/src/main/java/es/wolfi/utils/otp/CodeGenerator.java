/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2024, Timo Triebensky (timo@binsky.org)
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

package es.wolfi.utils.otp;

import org.apache.commons.codec.binary.Base32;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CodeGenerator {
    private final HashingAlgorithm algorithm;
    private final int digits;
    private final int period;

    public CodeGenerator() {
        this(HashingAlgorithm.SHA1, 6, 30);
    }

    public CodeGenerator(HashingAlgorithm algorithm, int digits, int period) {
        if (algorithm == null) {
            throw new InvalidParameterException("HashingAlgorithm must not be null.");
        }
        if (digits < 1) {
            throw new InvalidParameterException("Number of digits must be higher than 0.");
        }
        if (period < 1) {
            throw new InvalidParameterException("Time step (period) must be higher than 0.");
        }

        this.algorithm = algorithm;
        this.digits = digits;
        this.period = period;
    }

    public String generate(String key) throws CodeGenerationException {
        try {
            // Get the current number of seconds since the epoch and
            // calculate the number of time periods passed.
            // see https://datatracker.ietf.org/doc/html/rfc6238#section-4.2
            long counter = Math.floorDiv(System.currentTimeMillis() / 1000, period);

            byte[] hash = generateHash(key, counter);
            return getDigitsFromHash(hash);
        } catch (Exception e) {
            throw new CodeGenerationException("Failed to generate code. See nested exception.", e);
        }
    }

    /**
     * Generate a HMAC-SHA hash of the given key and counter number.
     */
    private byte[] generateHash(String key, long counter) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        byte[] decodedKey;
        try {
            Base32 codec = new Base32();
            decodedKey = codec.decode(key);
        } catch (NoSuchMethodError e) {
            decodedKey = Base32Decoder.decodeBase32(key);
        }

        SecretKeySpec signKey = new SecretKeySpec(decodedKey, algorithm.getHmacAlgorithm());
        Mac mac = Mac.getInstance(algorithm.getHmacAlgorithm());
        mac.init(signKey);

        return mac.doFinal(data);
    }

    /**
     * Get the n-digit code for a given hash.
     */
    private String getDigitsFromHash(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xF;

        long truncatedHash = 0;

        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= (long) Math.pow(10, digits);

        // Left pad with 0s for a n-digit code
        return String.format("%0" + digits + "d", truncatedHash);
    }
}
