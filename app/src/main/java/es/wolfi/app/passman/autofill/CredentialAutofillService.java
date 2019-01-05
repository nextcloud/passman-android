package es.wolfi.app.passman.autofill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.GeneralUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static android.service.autofill.SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE;
import static java.util.stream.Collectors.toList;

public final class CredentialAutofillService extends AutofillService {

    private static final String TAG = "CredentialAutofillSvc";

    private static final int MAX_DATASETS = 4;

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback) {
        Log.d(TAG, "onFillRequest()");

        List<FillContext> fillContexts = request.getFillContexts();
        ArrayList<AssistStructure> structures = new ArrayList<>();

        for (FillContext fc : fillContexts) {
            structures.add(fc.getStructure());
        }

        final AssistStructure latestAssistStructure =
                fillContexts.get(fillContexts.size() - 1).getStructure();

        // Find autofillable fields

        List<Pair<String, AutofillId>> fields = getAutofillableFields(latestAssistStructure);

        final String packageName = getApplicationContext().getPackageName();

        final String requesterPackageName = latestAssistStructure.getActivityComponent().getPackageName();

        Log.d(TAG, "autofillable fields for: " + requesterPackageName + ": " + fields);

        // We don't have any fields to work with
        if (fields.isEmpty()) {
            Log.d(TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess(null);
            return;
        }


        // Create the base response
        FillResponse.Builder response = new FillResponse.Builder();

        // Open Vault

        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());

        if (v == null) {
            GeneralUtils.debugAndToast(true, getApplicationContext(), getString(R.string.autofill_noactivevault));
            callback.onSuccess(null);
            return;
        }

        if (!v.is_unlocked()) {
            GeneralUtils.debugAndToast(true, getApplicationContext(), getString(R.string.autofill_vaultlocked));
            callback.onSuccess(null);
            return;
        }

        // If we get here, we have an unlocked vault
        Log.d(TAG, "Vault ready to go");

        CredentialAutofillService.WebDomainResult domain = getLikelyDomain(structures);

        // Grab Credentials from vault
        ArrayList<Credential> allCred = v.getCredentials();

        if (allCred.isEmpty()) {
            GeneralUtils.debugAndToast(true, getApplicationContext(), getString(R.string.autofill_vaultempty));
            callback.onSuccess(null);
            return;
        }

        // Find the credentials which match the requesting package name

        List<Credential> matchingCredentials = findMatchingCredentials(allCred, requesterPackageName, domain);

        Log.d(TAG, "Number of matching credentials for package: " +
                requesterPackageName +
                ":" +
                matchingCredentials.size());

        /*
         * TODO: validate package signature
         * (Maybe store apk signature or signing cert thumbprint in custom field)
         */

        /* Process credentials
         * Loop through the matching credentials
         * Create dataset
         * Add dataset to FillResponse
         */

        Set<AutofillId> tempFields = new HashSet<>();

        for (Credential thisCred : matchingCredentials) {

            String credLabel = returnBestString(thisCred.getLabel(),
                    thisCred.getUrl(),
                    thisCred.getUsername());

            Dataset.Builder dataset = new Dataset.Builder();

            for (Pair<String, AutofillId> field : fields) {

                String value = ""; // actual value we want to set the field to
                String displayValue = credLabel; // display value for the field

                String hint = field.first; // hint of field type we are filling
                AutofillId id = field.second; // id for the field

                switch (hint) {
                    case View.AUTOFILL_HINT_EMAIL_ADDRESS:
                        value = returnBestString(thisCred.getEmail(),
                                thisCred.getUsername(),
                                thisCred.getLabel());
                        break;
                    case View.AUTOFILL_HINT_USERNAME:
                        value = returnBestString(thisCred.getUsername(),
                                thisCred.getEmail(),
                                thisCred.getLabel());
                        break;
                    case View.AUTOFILL_HINT_PASSWORD:
                        value = thisCred.getPassword();
                        displayValue = getString(R.string.autofill_passwordfor) + credLabel;
                        break;
                }

                tempFields.add(id);

                Log.d(TAG, "Added to presentation: " + displayValue);
                // Draw the field in the dataset
                RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
                dataset.setValue(id, AutofillValue.forText(value), presentation);
            }
            Log.d(TAG, "Added to dataset");
            response.addDataset(dataset.build());
        }

        /* Let android know we want to save any credentials
         * Manually entered by the user
         * We will save usernames, passwords and email addresses
         */

        if (tempFields.size() > 0) {
            Log.d(TAG, "Requesting save info");

            AutofillId[] requiredIds = new AutofillId[tempFields.size()];
            tempFields.toArray(requiredIds);
            response.setSaveInfo(
                    new SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            requiredIds)
                            .setFlags(FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                            .build());
        }
        Log.d(TAG, "Building and calling success");
        callback.onSuccess(response.build());
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        Log.d(TAG, "onSaveRequest()");
        List<FillContext> fillContexts = request.getFillContexts();
        final AssistStructure latestStructure = fillContexts.get(fillContexts.size() - 1).getStructure();

        final String requesterPackageName = latestStructure.getActivityComponent().getPackageName();
        String requesterDomainName = null;
        String requesterApplicationLabel = null;

        // Find autofillable fields
        ArrayList<AssistStructure> structures = new ArrayList<>();

        for (FillContext fc : request.getFillContexts()) {
            structures.add(fc.getStructure());
        }

        CredentialAutofillService.WebDomainResult domain = getLikelyDomain(structures);

        if (domain.firstDomain != null) {
            requesterDomainName = domain.firstDomain;
        } else {
            requesterDomainName = "";
        }

        List<Pair<String, AutofillValue>> fields = getAutofillableFields(latestStructure, true);

        // We don't have any fields to work with
        if (fields.isEmpty()) {
            Log.d(TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess();
            return;
        }

        // Open Vault

        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());

        if (v == null) {
            GeneralUtils.debugAndToast(true, getApplicationContext(), getString(R.string.autofill_noactivevault));
            callback.onSuccess();
            return;
        }

        if (!v.is_unlocked()) {
            GeneralUtils.debugAndToast(true, getApplicationContext(), getString(R.string.autofill_vaultlocked));
            callback.onSuccess();
            return;
        }

        try {
            ApplicationInfo requesterAppInfo = getPackageManager().getApplicationInfo(requesterPackageName, 0);

            requesterApplicationLabel = getPackageManager().getApplicationLabel(requesterAppInfo).toString();
        } catch (Exception ex) {
            Log.d(TAG, "Couldn't read application label for: " + requesterPackageName);
        }

        if (TextUtils.isEmpty(requesterApplicationLabel)) {
            requesterApplicationLabel = requesterPackageName;
        }

        Log.d(TAG, "onSaveRequest(): Application: " + requesterApplicationLabel);

        String username = null;
        String email = null;
        String password = null;


        // TODO: switch this around, find the best
        // option for each of our hints
        // and send them.
        // See GetHints/inferHints below.

        for (Pair<String, AutofillValue> field : fields) {

            String hint = field.first; // hint of field type we are filling
            AutofillValue value = field.second; // id for the field

            if (!value.isText()) {
                continue;
            }

            switch (hint) {
                case View.AUTOFILL_HINT_EMAIL_ADDRESS:
                    email = value.getTextValue().toString();
                    break;
                case View.AUTOFILL_HINT_USERNAME:
                    username = value.getTextValue().toString();
                    break;
                case View.AUTOFILL_HINT_PASSWORD:
                    password = value.getTextValue().toString();
                    break;
            }
        }
        String customFieldString = "";
        try {
            JSONArray customFields = new JSONArray();
            JSONObject customField = new JSONObject();
            customField.put("label", "androidCredPackageName");
            customField.put("value", requesterPackageName);
            customField.put("secret", false);
            customField.put("field_type", "text");
            customFields.put(customField);
            customFieldString = customFields.toString();
        } catch (JSONException e) {
        }

        Log.d(TAG, "onSaveRequest(), building Credential");

        Credential newCred = new Credential();
        newCred
                .setVault(v)
                .setDescription(getString(R.string.autofill_createdbyautofillservice))
                .setEmail(email)
                .setLabel(requesterApplicationLabel)
                .setCustomFields(customFieldString)
                .setUsername(username)
                .setPassword(password)
                .setFiles((new JSONArray()).toString())
                .setTags((new JSONArray()).toString())
                .setOtp((new JSONObject()).toString())
                .setUrl(requesterDomainName);

        Log.d(TAG, "onSaveRequest(), saving Credential");

        Vault.addCredential(
                true,
                this,
                v,
                newCred,
                new FutureCallback<Credential>() {
                    @Override
                    public void onCompleted(Exception e, Credential result) {
                        if (e != null) {
                            Log.d(TAG, "onSaveRequest(), failed to save: " + e.toString());
                            GeneralUtils.debugAndToast(true, getApplicationContext(), "Failed to save: " + e.toString());
                        } else {
                            GeneralUtils.debugAndToast(true, getApplicationContext(), "Saved");
                        }
                    }
                });
        Log.d(TAG, "onSaveRequest() finished");
        GeneralUtils.debug("onSaveRequest finished");
        callback.onSuccess();
    }

    private static class WebDomainResult {
        String firstDomain = null;
        HashSet<String> allDomains;

        public WebDomainResult() {
            allDomains = new HashSet();
            GeneralUtils.debug("Web Domain Result constructed");
        }

        public void addDomain(String domain) {
            if (TextUtils.isEmpty(domain)) {
                return;
            }

            domain = domain.toLowerCase();

            GeneralUtils.debug("Found domain: " + domain);

            allDomains.add(domain);

            if (firstDomain == null) {
                firstDomain = domain;
            }
        }
    }


    @NonNull
    static CredentialAutofillService.WebDomainResult getLikelyDomain(ArrayList<AssistStructure> assistStructures) {
        GeneralUtils.debug("Finding domains.");
        CredentialAutofillService.WebDomainResult res = new CredentialAutofillService.WebDomainResult();
        GeneralUtils.debug("Finding domains - res allocated");
        for (AssistStructure assistStructure : assistStructures) {
            int nodes = assistStructure.getWindowNodeCount();
            for (int i = 0; i < nodes; i++) {
                AssistStructure.ViewNode viewNode = assistStructure.getWindowNodeAt(i).getRootViewNode();
                getNodeDomain(viewNode, res);
            }
        }
        GeneralUtils.debug("Returning, found :" + String.valueOf(res.allDomains.size()) + " domains.");
        return res;
    }

    static void getNodeDomain(AssistStructure.ViewNode viewNode, CredentialAutofillService.WebDomainResult res) {
        String webDomain = viewNode.getWebDomain();
        if (webDomain != null) {
            res.addDomain(webDomain);
        }
        for (int i = 0; i < viewNode.getChildCount(); i++) {
            getNodeDomain(viewNode.getChildAt(i), res);
        }
    }

    @NonNull
    static RemoteViews newDatasetPresentation(@NonNull String packageName,
                                              @NonNull CharSequence text) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.autofill_list_item);
        presentation.setTextViewText(R.id.autofilltext, text);
        return presentation;
    }


    @NonNull
    private List<Pair<String, AutofillId>> getAutofillableFields(@NonNull AssistStructure structure) {
        List<Pair<String, AutofillId>> fields = new ArrayList<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node);
        }
        return fields;
    }

    @NonNull
    private List<Pair<String, AutofillValue>> getAutofillableFields(@NonNull AssistStructure structure,
                                                                    boolean asValue) {
        List<Pair<String, AutofillValue>> fields = new ArrayList<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node, asValue);
        }
        return fields;
    }

    @NonNull
    private String returnBestString(@NonNull String... usernameOptions) {
        for (int i = 0; i < usernameOptions.length; i++) {
            String thisUsernameOption = usernameOptions[i];
            if (!TextUtils.isEmpty(thisUsernameOption) && !thisUsernameOption.equals("null")) {
                return thisUsernameOption;
            }
        }
        return "";
    }

    private List<Credential> findMatchingCredentials(
            @NonNull ArrayList<Credential> credentialArrayList,
            @NonNull String packageName,
            @NonNull CredentialAutofillService.WebDomainResult domain) {
        ArrayList<Credential> matchingCred = new ArrayList<>();

        for (Credential thisCred : credentialArrayList) {
            String credUri = null;
            try {
                String url = thisCred.getUrl();
                if (url != null) {
                    URI uri = new URI(url);
                    url = uri.getHost();

                    if (url != null) {
                        credUri = url.toLowerCase();
                    }
                }
            } catch (Exception ex) {
                GeneralUtils.debug("Couldn't decode Cred URL to host part:" + ex.toString());
            }

            if (credUri != null && domain.firstDomain != null) {
                if (credUri.equals(domain.firstDomain)) {
                    GeneralUtils.debug("Matching cred on domain: " + domain.firstDomain);
                    matchingCred.add(thisCred);
                }
            }

            try {
                JSONArray thisCredCustomFields = new JSONArray(thisCred.getCustomFields());
                for (int i = 0; i < thisCredCustomFields.length(); i++) {
                    JSONObject thisCredCustomField = thisCredCustomFields.getJSONObject(i);

                    String customFieldLabel =
                            thisCredCustomField.getString("label");

                    if (customFieldLabel.equalsIgnoreCase("androidCredPackageName")) {

                        String credPackageName = thisCredCustomField.getString("value");

                        Log.d(TAG, "Checking custom fields: " +
                                packageName +
                                " vs " +
                                credPackageName);

                        if (packageName.equalsIgnoreCase(credPackageName)) {
                            matchingCred.add(thisCred);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                Log.d(TAG, "Cannot decode custom fields: " + ex.toString());
            }

            if (matchingCred.size() >= MAX_DATASETS) {
                return matchingCred;
            }
        }
        return matchingCred;
    }

    private void addAutofillableFields(@NonNull List<Pair<String, AutofillId>> fields,
                                       @NonNull ViewNode node) {

        ArrayList<String> potentialHints = getHints(node);

        if (potentialHints != null) {
            for (String hint : potentialHints) {
                if (hint != null && isValidHint(hint)) {
                    AutofillId id = node.getAutofillId();
                    //if (!fields.containsKey(hint)) {
                    Log.v(TAG, "Setting hint '" + hint + "' on " + id);
                    Pair<String, AutofillId> thisField = new Pair<>(hint, id);
                    fields.add(thisField);
                    // } else {
                    //     Log.v(TAG, "Ignoring hint '" + hint + "' on " + id
                    //             + " because it was already set");
                    // }
                } else {
                    Log.v(TAG, "Ignoring hint '" + hint + "' on node " + node.getId()
                            + " because it is null or we cannot handle it.");
                }
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i));
        }
    }

    private void addAutofillableFields(@NonNull List<Pair<String, AutofillValue>> fields,
                                       @NonNull ViewNode node,
                                       boolean asValue) {

        ArrayList<String> potentialHints = getHints(node);

        if (potentialHints != null) {
            for (String hint : potentialHints) {
                if (hint != null && isValidHint(hint)) {
                    //AutofillId id = node.getAutofillId();
                    //if (!fields.containsKey(hint)) {
                    //Log.v(TAG, "Setting hint '" + hint + "' on " + id);
                    Pair<String, AutofillValue> thisField = new Pair<>(hint, node.getAutofillValue());
                    fields.add(thisField);
                    //fields.put(hint, node.getAutofillValue());
                    //} else {
                    //Log.v(TAG, "Ignoring hint '" + hint + "' on " + id
                    //         + " because it was already set");
                    //}
                } else {
                    Log.v(TAG, "Ignoring hint '" + hint + "' on node " + node.getId()
                            + " because it is null or we cannot handle it.");
                }
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i), asValue);
        }
    }

    /* true for hints we can handle */
    static boolean isValidHint(@NonNull String hint) {
        switch (hint) {
            case View.AUTOFILL_HINT_EMAIL_ADDRESS:
            case View.AUTOFILL_HINT_PASSWORD:
            case View.AUTOFILL_HINT_USERNAME:
                return true;
        }
        return false;
    }

    // Based on:
    // https://github.com/googlesamples/android-AutofillFramework/blob/master/
    // afservice/src/main/java/com/example/android/autofill/service/simple/DebugService.java

    @Nullable
    protected ArrayList<String> getHints(@NonNull ViewNode node) {

        ArrayList<String> potentialHints;

        // If real autofill hints are defined, use them
        // and skip the heuristics below

        String[] hints = node.getAutofillHints();

        if (hints != null) {
            potentialHints = new ArrayList<>(Arrays.asList(hints));
            return potentialHints;
        }

        // Otherwise
        // Then try some rudimentary heuristics based on other node properties

        potentialHints = new ArrayList<>();

        // TODO - node.getHtmlInfo(); - not really any point as password will be *****
        // in the compat browsers
        // TODO - figure out how to decide which required and which optional
        // Maybe give priority by order for each ViewNode.
        // Android hint first
        // then viewHint
        // then resourceId
        // by the text in the EditText

        // PROBLEM WAS THAT WE FOUND TOO MANY APPLICABLE VIEWNODES
        // AND WE CAN'T HAVE REQUIRED IDs for ALL OF THEM!
        // NEED TO FIGURE OUT WHICH ONES ARE THE REAL AUTH BOXES.

        // Maybe get Autofill Hint fields first, then other heuristically found
        // views, but only if they are in focus.


        /*String viewHint = node.getHint();
        String hint = inferHint(node, viewHint);
        if (hint != null) {
            Log.d(TAG, "Found hint using view hint(" + viewHint + "): " + hint);
            potentialHints.add(hint);
        } else if (!TextUtils.isEmpty(viewHint)) {
            Log.v(TAG, "No hint using view hint: " + viewHint);
        }

        String resourceId = node.getIdEntry();
        hint = inferHint(node, resourceId);
        if (hint != null) {
            Log.d(TAG, "Found hint using resourceId(" + resourceId + "): " + hint);
            potentialHints.add(hint);
        } else if (!TextUtils.isEmpty(resourceId)) {
            Log.v(TAG, "No hint using resourceId: " + resourceId);
        }

        CharSequence text = node.getText();
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
        return potentialHints;
    }

    /**
     * Uses heuristics to infer an autofill hint from a {@code string}.
     *
     * @return standard autofill hint, or {@code null} when it could not be inferred.
     */
    @Nullable
    protected String inferHint(ViewNode node, @Nullable String actualHint) {
        if (actualHint == null) return null;

        String hint = actualHint.toLowerCase();
        if (hint.contains("label") || hint.contains("container")) {
            Log.v(TAG, "Ignoring 'label/container' hint: " + hint);
            return null;
        }

        if (hint.contains("password")) return View.AUTOFILL_HINT_PASSWORD;
        if (hint.contains("username")
                || (hint.contains("login") && hint.contains("id")))
            return View.AUTOFILL_HINT_USERNAME;
        if (hint.contains("email")) return View.AUTOFILL_HINT_EMAIL_ADDRESS;
        if (hint.contains("name")) return View.AUTOFILL_HINT_NAME;
        if (hint.contains("phone")) return View.AUTOFILL_HINT_PHONE;

        // When everything else fails, return the full string - this is helpful to help app
        // developers visualize when autofill is triggered when it shouldn't (for example, in a
        // chat conversation window), so they can mark the root view of such activities with
        // android:importantForAutofill=noExcludeDescendants
        //if (node.isEnabled() && node.getAutofillType() != View.AUTOFILL_TYPE_NONE) {
        //Log.v(TAG, "Falling back to " + actualHint);
        //return actualHint;
        //}
        return null;
    }

}
