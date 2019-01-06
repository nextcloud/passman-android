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
