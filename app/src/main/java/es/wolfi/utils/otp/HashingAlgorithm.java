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

public enum HashingAlgorithm {
    SHA1("HmacSHA1", "SHA1"),
    SHA256("HmacSHA256", "SHA256"),
    SHA512("HmacSHA512", "SHA512");

    public static final String[] hashingAlgorithmsFriendlyArray = new String[]{
            HashingAlgorithm.SHA1.getFriendlyName(),
            HashingAlgorithm.SHA256.getFriendlyName(),
            HashingAlgorithm.SHA512.getFriendlyName()
    };

    private final String hmacAlgorithm;
    private final String friendlyName;

    HashingAlgorithm(String hmacAlgorithm, String friendlyName) {
        this.hmacAlgorithm = hmacAlgorithm;
        this.friendlyName = friendlyName;
    }

    public String getHmacAlgorithm() {
        return hmacAlgorithm;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Parse input text into HashingAlgorithm. Returns HashingAlgorithm.SHA1 as fallback.
     */
    public static HashingAlgorithm fromStringOrSha1(String friendlyNameInput) {
        HashingAlgorithm algorithm = HashingAlgorithm.SHA1;
        if (friendlyNameInput.equalsIgnoreCase(HashingAlgorithm.SHA256.friendlyName)) {
            algorithm = HashingAlgorithm.SHA256;
        } else if (friendlyNameInput.equalsIgnoreCase(HashingAlgorithm.SHA512.friendlyName)) {
            algorithm = HashingAlgorithm.SHA512;
        }
        return algorithm;
    }
}
