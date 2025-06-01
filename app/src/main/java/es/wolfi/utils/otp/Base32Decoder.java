package es.wolfi.utils.otp;

import java.util.HashMap;
import java.util.Map;

public class Base32Decoder {
    // Standard Base32 alphabet (RFC 4648)
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final Map<Character, Integer> DECODE_MAP = new HashMap<>();

    static {
        // Build the decode map
        for (int i = 0; i < BASE32_ALPHABET.length(); i++) {
            DECODE_MAP.put(BASE32_ALPHABET.charAt(i), i);
        }
    }

    /**
     * Decodes a Base32 encoded string to bytes
     *
     * @param encoded The Base32 encoded string
     * @return The decoded bytes
     * @throws IllegalArgumentException if the input is invalid
     */
    public static byte[] decodeBase32(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return new byte[0];
        }

        // Remove padding and convert to uppercase
        encoded = encoded.toUpperCase().replaceAll("=", "");

        // Validate input length
        if (encoded.isEmpty()) {
            return new byte[0];
        }

        // Calculate output length
        int outputLength = (encoded.length() * 5) / 8;
        byte[] output = new byte[outputLength];

        int bits = 0;
        int value = 0;
        int index = 0;

        for (char c : encoded.toCharArray()) {
            Integer charValue = DECODE_MAP.get(c);
            if (charValue == null) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }

            value = (value << 5) | charValue;
            bits += 5;

            if (bits >= 8) {
                output[index++] = (byte) ((value >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }

        return output;
    }

    /**
     * Decodes a Base32 encoded string to a UTF-8 string
     *
     * @param encoded The Base32 encoded string
     * @return The decoded string
     * @throws IllegalArgumentException if the input is invalid
     */
    public static String decodeBase32ToString(String encoded) {
        byte[] decoded = decodeBase32(encoded);
        return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
    }
}
