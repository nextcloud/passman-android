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

package es.wolfi.utils;

public class JSONUtils {
    /**
     * Checks if an string looks like a json object
     * @param text to check
     * @return true if it looks like a json object, false otherwise
     */
    public static final boolean isJSONObject(String data){
        if (data.length() == 0) return false;
        return data.charAt(0) == '{' && data.charAt(data.length() -1) == '}';
    }

    /**
     * Check if an string seems like a json array
     * @param the text to check
     * @return true if it looks like an array, false otherwise
     */
    public static final boolean isJSONArray(String data){
        if (data.length() == 0) return false;
        return data.charAt(0) == '[' && data.charAt(data.length() -1) == ']';
    }
}
