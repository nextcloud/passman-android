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

package es.wolfi.app.passman.autofill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutofillFieldCollection extends ArrayList<AutofillField> {
    public AutofillField getRequiredId(String hint) {
        // Order: Android Hint, HTMLInfo, View Hint,  Resource id
        // Focused First

        Collections.sort(this);

        for (AutofillField field : this) {
            if (field.hasHint(hint)) {
                return field;
            }
        }
        return null;
    }

    public List<AutofillField> getOptionalIds(String hint) {
        // Order: Android Hint, View Hint, HTMLInfo, Resource id
        // Focused First
        // Excludes requiredIds
        List<AutofillField> allMatching = new ArrayList<>();

        Collections.sort(this);

        for (AutofillField field : this) {
            if (field.hasHint(hint)) {
                allMatching.add(field);
            }
        }
        allMatching.remove(getRequiredId(hint));

        return allMatching;
    }
}
