package es.wolfi.app.passman.autofill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.content.pm.ApplicationInfo;
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
import android.support.v4.util.ArrayMap;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static android.service.autofill.SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE;

public final class CredentialAutofillService extends AutofillService {

    private static final String TAG = "CredentialAutofillSvc";

    private static final int MAX_DATASETS = 4;

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback) {
        Log.d(TAG, "onFillRequest()");

        // Find autofillable fields
        AssistStructure structure = getLatestAssistStructure(request);
        Map<String, AutofillId> fields = getAutofillableFields(structure);

        final String packageName = getApplicationContext().getPackageName();

        final String requesterPackageName = structure.getActivityComponent().getPackageName();

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
            GeneralUtils.debugAndToast(true,getApplicationContext(),getString(R.string.autofill_noactivevault));
            callback.onSuccess(null);
            return;
        }

        if (!v.is_unlocked()) {
            GeneralUtils.debugAndToast(true,getApplicationContext(),getString(R.string.autofill_vaultlocked));
            callback.onSuccess(null);
            return;
        }

        // If we get here, we have an unlocked vault
        Log.d(TAG, "Vault ready to go");

        // Grab Credentials from vault
        ArrayList<Credential> allCred = v.getCredentials();

        if (allCred.isEmpty()) {
            GeneralUtils.debugAndToast(true,getApplicationContext(),getString(R.string.autofill_vaultempty));
            callback.onSuccess(null);
            return;
        }

        // Find the credentials which match the requesting package name

        List<Credential> matchingCredentials = findMatchingCredentials(allCred,requesterPackageName);

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

        for (Credential thisCred: matchingCredentials) {

            String credLabel = returnBestString(thisCred.getLabel(),
                    thisCred.getUrl(),
                    thisCred.getUsername());

            Dataset.Builder dataset = new Dataset.Builder();

            for (Entry<String, AutofillId> field : fields.entrySet()) {

                String value = ""; // actual value we want to set the field to
                String displayValue = credLabel; // display value for the field

                String hint = field.getKey(); // hint of field type we are filling
                AutofillId id = field.getValue(); // id for the field

                switch(hint) {
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

                // Draw the field in the dataset
                RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
                dataset.setValue(id, AutofillValue.forText(value), presentation);
            }
            response.addDataset(dataset.build());
        }

        /* Let android know we want to save any credentials
         * Manually entered by the user
         * We will save usernames, passwords and email addresses
        */

        Collection<AutofillId> ids = fields.values();
        AutofillId[] requiredIds = new AutofillId[ids.size()];
        ids.toArray(requiredIds);
        response.setSaveInfo(
                new SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                        requiredIds)
                        .setFlags(FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                        .build());

        callback.onSuccess(response.build());
    }

    @Override
    public void onSaveRequest(SaveRequest request,  SaveCallback callback) {
        //List<FillContext> fillContexts = request.getFillContexts();
        Log.d(TAG, "onSaveRequest()");
        final AssistStructure latestStructure = getLatestAssistStructure(request);
        final String requesterPackageName = latestStructure.getActivityComponent().getPackageName();
        String requesterApplicationLabel = null;

        Map<String, AutofillValue> fields = getAutofillableFields(latestStructure, true);

        // We don't have any fields to work with
        if (fields.isEmpty()) {
            Log.d(TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess();
            return;
        }

        // Open Vault

        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());

        if (v == null) {
            GeneralUtils.debugAndToast(true,getApplicationContext(),getString(R.string.autofill_noactivevault));
            callback.onSuccess();
            return;
        }

        if (!v.is_unlocked()) {
            GeneralUtils.debugAndToast(true,getApplicationContext(),getString(R.string.autofill_vaultlocked));
            callback.onSuccess();
            return;
        }

        try {
            ApplicationInfo requesterAppInfo = getPackageManager().getApplicationInfo(requesterPackageName, 0);

            requesterApplicationLabel = getPackageManager().getApplicationLabel(requesterAppInfo).toString();
        }
        catch (Exception ex) {
            Log.d(TAG, "Couldn't read application label for: " + requesterPackageName);
        }

        if (TextUtils.isEmpty(requesterApplicationLabel)) {
            requesterApplicationLabel = requesterPackageName;
        }

        Log.d(TAG, "onSaveRequest(): Application: " + requesterApplicationLabel);


        String username = null;
        String email = null;
        String password = null;

        for (Entry<String, AutofillValue> field : fields.entrySet()) {

            String hint = field.getKey(); // hint of field type we are filling
            AutofillValue value = field.getValue(); // id for the field

            if (!value.isText()) {
                continue;
            }

            switch(hint) {
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
            customField.put("secret",false);
            customField.put("field_type", "text");
            customFields.put(customField);
            customFieldString = customFields.toString();
        }
        catch (JSONException e)
        {}

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
                .setUrl("");

        Log.d(TAG, "onSaveRequest(), saving Credential");

        Vault.addCredential(this, v, newCred, new FutureCallback<Credential>() {
            @Override
            public void onCompleted(Exception e, Credential result) {
                if (e != null) {
                    Log.d(TAG, "onSaveRequest(), failed to save: " + e.toString());
                }
                else
                {
                    Log.d(TAG, "onSaveRequest(), saved");
                }
            }
        });
        Log.d(TAG, "onSaveRequest() finished");
        GeneralUtils.debug("onSaveRequest finished");
        callback.onSuccess();
    }

    @NonNull
    private Map<String, AutofillId> getAutofillableFields(@NonNull AssistStructure structure) {
        Map<String, AutofillId> fields = new ArrayMap<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node);
        }
        return fields;
    }

    @NonNull
    private Map<String, AutofillValue> getAutofillableFields(@NonNull AssistStructure structure,
                                                             boolean asValue) {
        Map<String, AutofillValue> fields = new ArrayMap<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node, asValue);
        }
        return fields;
    }

    @NonNull
    private String returnBestString(@NonNull String ... usernameOptions)
    {
        for (int i=0; i < usernameOptions.length; i++) {
            String thisUsernameOption = usernameOptions[i];
            if (!TextUtils.isEmpty(thisUsernameOption) && !thisUsernameOption.equals("null")) {
                return thisUsernameOption;
            }
        }
        return "";
    }

    private List<Credential> findMatchingCredentials(
            @NonNull ArrayList<Credential> credentialArrayList,
            @NonNull String packageName)
    {
        ArrayList<Credential> matchingCred = new ArrayList<>();

        for(Credential thisCred: credentialArrayList) {
            try {
                JSONArray thisCredCustomFields = new JSONArray(thisCred.getCustomFields());
                for(int i = 0; i < thisCredCustomFields.length(); i++) {
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
                        }
                    }
                }
            }
            catch(Exception ex) {
                Log.d(TAG, "Cannot decode custom fields: " + ex.toString());
            }
            finally {
                if (matchingCred.size() >= MAX_DATASETS) {
                    return matchingCred;
                }
            }
        }
        return matchingCred;
    }

    private void addAutofillableFields(@NonNull Map<String, AutofillId> fields,
                                       @NonNull ViewNode node) {
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String hint: hints) {
                if (hint != null && isValidHint(hint)) {
                    AutofillId id = node.getAutofillId();
                    if (!fields.containsKey(hint)) {
                        Log.v(TAG, "Setting hint '" + hint + "' on " + id);
                        fields.put(hint, id);
                    } else {
                        Log.v(TAG, "Ignoring hint '" + hint + "' on " + id
                                + " because it was already set");
                    }
                }
                else {
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

    private void addAutofillableFields(@NonNull Map<String, AutofillValue> fields,
                                       @NonNull ViewNode node,
                                       boolean asValue) {
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String hint: hints) {
                if (hint != null && isValidHint(hint)) {
                    AutofillId id = node.getAutofillId();
                    if (!fields.containsKey(hint)) {
                        Log.v(TAG, "Setting hint '" + hint + "' on " + id);
                        fields.put(hint, node.getAutofillValue());
                    } else {
                        Log.v(TAG, "Ignoring hint '" + hint + "' on " + id
                                + " because it was already set");
                    }
                }
                else {
                    Log.v(TAG, "Ignoring hint '" + hint + "' on node " + node.getId()
                            + " because it is null or we cannot handle it.");
                }
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i),asValue);
        }
    }


    static boolean isValidHint(@NonNull String hint) {
        switch (hint) {
            case View.AUTOFILL_HINT_EMAIL_ADDRESS:
            case View.AUTOFILL_HINT_PASSWORD:
            case View.AUTOFILL_HINT_USERNAME:
                return true;
        }
        return false;
    }

    @NonNull
    static AssistStructure getLatestAssistStructure(@NonNull FillRequest request) {
        List<FillContext> fillContexts = request.getFillContexts();
        return fillContexts.get(fillContexts.size() - 1).getStructure();
    }

    @NonNull
    static AssistStructure getLatestAssistStructure(@NonNull SaveRequest request) {
        List<FillContext> fillContexts = request.getFillContexts();
        return fillContexts.get(fillContexts.size() - 1).getStructure();
    }

    @NonNull
    static RemoteViews newDatasetPresentation(@NonNull String packageName,
                                              @NonNull CharSequence text) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.autofill_list_item);
        presentation.setTextViewText(R.id.autofilltext, text);
        return presentation;
    }

}
