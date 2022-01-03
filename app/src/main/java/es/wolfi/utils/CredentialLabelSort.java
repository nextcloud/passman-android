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

import java.util.Comparator;

import es.wolfi.passman.API.Credential;

/**
 * Created by wolfi on 12/11/17.
 */

public class CredentialLabelSort implements Comparator<Credential> {

    /**
     * credential sort methods:
     * description                      code
     * <p>
     * default server sort              0
     * alphabetically ascending         1
     * alphabetically descending        2
     */
    public enum SortMethod {
        STANDARD, ALPHABETICALLY_ASCENDING, ALPHABETICALLY_DESCENDING
    }

    private final int method;

    public CredentialLabelSort(int method) {
        this.method = method;
    }

    @Override
    public int compare(Credential left, Credential right) {
        if (method == SortMethod.ALPHABETICALLY_ASCENDING.ordinal()) {
            return left.getLabel().compareTo(right.getLabel());
        }
        if (method == SortMethod.ALPHABETICALLY_DESCENDING.ordinal()) {
            return right.getLabel().compareTo(left.getLabel());
        }
        return left.getId() - right.getId();
    }
}
