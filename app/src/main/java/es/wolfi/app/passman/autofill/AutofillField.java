package es.wolfi.app.passman.autofill;

import android.app.assist.AssistStructure;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class AutofillField implements Comparable {

    private static String TAG = "AutofillField";

    public static final int AUTOFILL_DISCOVERY_TYPE_NONE = 0;
    public static final int AUTOFILL_DISCOVERY_TYPE_ANDROIDHINT = 1;
    public static final int AUTOFILL_DISCOVERY_TYPE_HTMLINFO = 2;
    public static final int AUTOFILL_DISCOVERY_TYPE_VIEWHINT = 4;
    public static final int AUTOFILL_DISCOVERY_TYPE_RESOURCEID = 8;
    public static final int AUTOFILL_DISCOVERY_TYPE_TEXT = 16;

    private HashSet<String> hints;
    private AutofillId autofillid;
    private AutofillValue autofillValue;
    private boolean focused;
    private int discoveryTypes;


    // constructors

    public AutofillField(AutofillId id, AssistStructure.ViewNode viewNode) throws Exception
    {
        if (id == null || viewNode == null)
            throw new NullPointerException("id and viewNode must be non null");

        autofillid = id;
        focused = viewNode.isFocused();
        hints = new HashSet<>();
        getHints(viewNode);

        if (discoveryTypes == AUTOFILL_DISCOVERY_TYPE_NONE || hints.isEmpty())
            throw new Exception("Not autofillable.");
    }

    public AutofillField(AutofillValue value, AssistStructure.ViewNode viewNode) throws Exception
    {
        if (value == null || viewNode == null)
            throw new NullPointerException("value and viewNode must be non null");

        autofillValue = value;
        focused = viewNode.isFocused();
        hints = new HashSet<>();
        getHints(viewNode);

        if ((discoveryTypes == AUTOFILL_DISCOVERY_TYPE_NONE) || hints.isEmpty())
            throw new Exception("Not autofillable.");
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean hasHint(String hint) {
        if (hints != null) {
            for (String fieldHint : hints) {
                if (fieldHint.equals(hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasDiscoveryType(int discoveryType) {
        if ((discoveryTypes & discoveryType) == discoveryType) {
            return true;
        }
        return false;
    }

    public boolean hasValidHint() {
        return hasHint(View.AUTOFILL_HINT_EMAIL_ADDRESS) ||
                hasHint(View.AUTOFILL_HINT_USERNAME) ||
                hasHint(View.AUTOFILL_HINT_PASSWORD);
    }

    public HashSet<String> getHints() {
        return hints;
    }

    public AutofillId getAutofillid() {
        return autofillid;
    }

    public AutofillValue getAutofillValue() {
        return autofillValue;
    }

    public int getDiscoveryTypes() {
        return discoveryTypes;
    }

    private int calculateSortValue() {
        // prioritise focused fields
        if (!isFocused()) {
            return discoveryTypes + 1;
        }
        return discoveryTypes;
    }

    public static String toStringValue(AutofillField field) {
        if (field != null &&
                field.getAutofillValue().isText() &&
                field.getAutofillValue().getTextValue() != null) {
            return field.getAutofillValue().getTextValue().toString();
        }
        return null;
    }

    @Override
    public int compareTo(Object o) {
        AutofillField otherAF = (AutofillField)o;

        int comparison = (this.calculateSortValue())-(otherAF.calculateSortValue());
        // lowest value is best, highest worst
        return comparison;
    }

    // Following based on:
    // https://github.com/googlesamples/android-AutofillFramework/blob/master/
    // afservice/src/main/java/com/example/android/autofill/service/simple/DebugService.java

    private void getHints(@NonNull AssistStructure.ViewNode node) {

        // If real autofill hints are defined, use them
        // and skip the heuristics below

        String[] androidhints = node.getAutofillHints();

        if (androidhints != null) {
            hints.addAll(Arrays.asList(androidhints));
            discoveryTypes |= AUTOFILL_DISCOVERY_TYPE_ANDROIDHINT;
            return;
        }


        String viewHint = node.getHint();
        String hint = inferHint(node, viewHint);
        if (hint != null) {
            //Log.d(TAG, "Found hint using view hint(" + viewHint + "): " + hint);
            hints.add(hint);
            discoveryTypes |= AUTOFILL_DISCOVERY_TYPE_VIEWHINT;
        }

        String resourceId = node.getIdEntry();
        hint = inferHint(node, resourceId);
        if (hint != null) {
           // Log.d(TAG, "Found hint using resourceId(" + resourceId + "): " + hint);
            hints.add(hint);
            discoveryTypes |= AUTOFILL_DISCOVERY_TYPE_RESOURCEID;
        }

        ViewStructure.HtmlInfo htmlInfo = node.getHtmlInfo();
        if (htmlInfo != null && htmlInfo.getTag().toLowerCase().equals("input")) {
            List<Pair<String, String>> htmlAttributes = htmlInfo.getAttributes();
            if (htmlAttributes != null)
            {
                for (Pair<String, String> keyValuePair: htmlAttributes) {
                    if (keyValuePair.first != null && keyValuePair.second != null) {
                        String key = keyValuePair.first.toLowerCase();
                        String value = keyValuePair.second.toLowerCase();
                        //Log.d(TAG, "Found input with attribute: " + key + ": " + value);
                        if (key.equals("type") && value.equals("password")) {
                            //Log.d(TAG, "Found password hint using htmlInfo");
                            hints.add(View.AUTOFILL_HINT_PASSWORD);
                            discoveryTypes |= AUTOFILL_DISCOVERY_TYPE_HTMLINFO;
                        }
                        else if (key.equals("id") || key.equals("name")) {
                            hint = inferHint(node, value);
                            if (hint != null) {
                                //Log.d(TAG, "Found hint using htmlInfo: " + hint);
                                hints.add(hint);
                                discoveryTypes |= AUTOFILL_DISCOVERY_TYPE_HTMLINFO;
                            }
                        }
                    }
                    else {
                        Log.d(TAG, "Found input with no attributes");
                    }
                }
            }
        }


        // I REALLY DON'T LIKE THIS IDEA, BUT WILL COME BACK TO IT

        /*CharSequence text = node.getText();
        CharSequence className = node.getClassName();
        if (text != null && className != null && className.toString().contains("EditText")) {
            hint = inferHint(node, text.toString());
            if (hint != null) {
                Log.d(TAG, "Found hint using text: " + hint);
                potentialHints.add(hint);
            }
        } else if (!TextUtils.isEmpty(text)) {
            Log.v(TAG, "No hint using class " + className);
        }
        */
    }

    /**
     * Uses heuristics to infer an autofill hint from a {@code string}.
     *
     * @return standard autofill hint, or {@code null} when it could not be inferred.
     */
    @Nullable
    private String inferHint(AssistStructure.ViewNode node, @Nullable String actualHint) {
        if (actualHint == null) return null;

        String hint = actualHint.toLowerCase();
        if (hint.contains("label") || hint.contains("container")) {
            //Log.v(TAG, "Ignoring 'label/container' hint: " + hint);
            return null;
        }

        if (hint.contains("password")) return View.AUTOFILL_HINT_PASSWORD;
        if (hint.contains("username")
                || (hint.contains("login") && hint.contains("id"))
                || (hint.contains("login") && hint.contains("user")))
            return View.AUTOFILL_HINT_USERNAME;
        if (hint.contains("email")) return View.AUTOFILL_HINT_EMAIL_ADDRESS;


        return null;
    }
}
