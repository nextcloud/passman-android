package es.wolfi.app.passman.autofill;

import static android.service.autofill.SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.CancellationSignal;
import android.preference.PreferenceManager;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.wolfi.app.ResponseHandlers.AutofillCredentialSaveResponseHandler;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.KeyStoreUtils;

@RequiresApi(api = Build.VERSION_CODES.O)
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
        AutofillFieldCollection fields = getAutofillableFields(latestAssistStructure, false);

        final String packageName = getApplicationContext().getPackageName();

        final String requesterPackageName = latestAssistStructure.getActivityComponent().getPackageName();

        Log.d(TAG, "autofillable fields for: " + requesterPackageName + ": " + fields);
        // We don't have any fields to work with
        // Passman should not authenticate itself
        if (fields.isEmpty() || requesterPackageName.startsWith("es.wolfi.app.passman")) {
            Log.d(TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess(null);
            return;
        }


        // Create the base response
        FillResponse.Builder response = new FillResponse.Builder();

        // Open Vault
        SingleTon ton = SingleTon.getTon();
        final Vault v = getAutofillVault(ton);

        if (v == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_noactivevault), Toast.LENGTH_SHORT).show();
            Log.d(TAG, getString(R.string.autofill_noactivevault));
            callback.onSuccess(null);
            return;
        }

        if (!v.is_unlocked()) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_vaultlocked), Toast.LENGTH_SHORT).show();
            Log.d(TAG, getString(R.string.autofill_vaultlocked));
            callback.onSuccess(null);
            return;
        }

        // If we get here, we have an unlocked vault
        Log.d(TAG, "Vault ready to go");

        CredentialAutofillService.WebDomainResult domain = getLikelyDomain(structures);

        // Grab Credentials from vault
        ArrayList<Credential> allCred = v.getCredentials();

        if (allCred.isEmpty()) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_vaultempty), Toast.LENGTH_SHORT).show();
            Log.d(TAG, getString(R.string.autofill_vaultempty));
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

        Set<AutofillId> tempFields = new HashSet<>();

        for (Credential thisCred : matchingCredentials) {

            String credLabel = returnBestString(thisCred.getLabel(),
                    thisCred.getUrl(),
                    thisCred.getUsername());

            Dataset.Builder dataset = new Dataset.Builder();

            // simplify into a function
            AutofillField bestUsername = fields.getRequiredId(View.AUTOFILL_HINT_USERNAME);
            AutofillField bestEmail = fields.getRequiredId(View.AUTOFILL_HINT_EMAIL_ADDRESS);
            AutofillField bestPassword = fields.getRequiredId(View.AUTOFILL_HINT_PASSWORD);

            if (bestUsername != null) {
                String value = returnBestString(thisCred.getUsername(),
                        thisCred.getEmail(),
                        thisCred.getLabel());

                buildAndAddPresentation(dataset,
                        packageName,
                        bestUsername,
                        value,
                        credLabel);

                tempFields.add(bestUsername.getAutofillid());
            }

            if (bestEmail != null) {
                String value = returnBestString(thisCred.getEmail(),
                        thisCred.getUsername(),
                        thisCred.getLabel());

                buildAndAddPresentation(dataset,
                        packageName,
                        bestEmail,
                        value,
                        credLabel);
                tempFields.add(bestEmail.getAutofillid());
            }

            if (bestPassword != null) {
                String value = thisCred.getPassword();

                buildAndAddPresentation(dataset,
                        packageName,
                        bestPassword,
                        value,
                        "Password for: " + credLabel);
                tempFields.add(bestPassword.getAutofillid());
            }

            if (bestUsername != null || bestEmail != null || bestPassword != null) {
                response.addDataset(dataset.build());
            }
        }

        /* Let android know we want to save any credentials manually entered by the user
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

            Log.d(TAG, "Building and calling success");
            callback.onSuccess(response.build());
            return;
        }
        Log.d(TAG, "Failed to find anything to do, bailing");
        callback.onSuccess(null);
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

        AutofillFieldCollection fields = getAutofillableFields(latestStructure, true);

        // We don't have any fields to work with
        if (fields.isEmpty()) {
            Log.d(TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess();
            return;
        }

        SingleTon ton = SingleTon.getTon();
        final Vault v = getAutofillVault(ton);

        if (v == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_noactivevault), Toast.LENGTH_SHORT).show();
            Log.d(TAG, getString(R.string.autofill_noactivevault));
            callback.onSuccess();
            return;
        }

        if (!v.is_unlocked()) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_vaultlocked), Toast.LENGTH_SHORT).show();
            Log.d(TAG, getString(R.string.autofill_vaultlocked));
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

        if (!requesterDomainName.equals("")) {
            String parsedDomain = getDomainName(requesterDomainName);
            if (parsedDomain.equals("")) {
                parsedDomain = requesterDomainName;
            }
            requesterApplicationLabel += " - " + parsedDomain;
        }

        AutofillField bestUsername = fields.getRequiredId(View.AUTOFILL_HINT_USERNAME);
        AutofillField bestEmail = fields.getRequiredId(View.AUTOFILL_HINT_EMAIL_ADDRESS);
        AutofillField bestPassword = fields.getRequiredId(View.AUTOFILL_HINT_PASSWORD);

        String username = AutofillField.toStringValue(bestUsername);
        String email = AutofillField.toStringValue(bestEmail);
        String password = AutofillField.toStringValue(bestPassword);

        if (email == null || email.equals("true") || email.equals("false")) {
            email = "";
        }
        if (username == null || username.equals("true") || username.equals("false")) {
            username = "";
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
            Log.e(TAG, "onSaveRequest(), error creating customField");
        }

        Log.d(TAG, "onSaveRequest(), building Credential");

        Credential newCred = new Credential();
        newCred.setVault(v);
        newCred.setDescription(getString(R.string.autofill_createdbyautofillservice));
        newCred.setEmail(email);
        newCred.setLabel(requesterApplicationLabel);
        newCred.setCustomFields(customFieldString);
        newCred.setUsername(username);
        newCred.setPassword(password);
        newCred.setFiles((new JSONArray()).toString());
        newCred.setTags((new JSONArray()).toString());
        newCred.setOtp((new JSONObject()).toString());
        newCred.setUrl(requesterDomainName);
        newCred.setCompromised(false);

        Log.d(TAG, "onSaveRequest(), saving Credential");

        final AsyncHttpResponseHandler responseHandler = new AutofillCredentialSaveResponseHandler(getAutofillVault(ton), getBaseContext(), getApplicationContext(), ton, TAG);
        newCred.save(getApplicationContext(), responseHandler);

        Log.d(TAG, "onSaveRequest() finished");
        callback.onSuccess();
    }

    private static class WebDomainResult {
        String firstDomain = null;
        HashSet<String> allDomains;

        public WebDomainResult() {
            allDomains = new HashSet();
            Log.d(TAG, "Web Domain Result constructed");
        }

        public void addDomain(String domain) {
            if (TextUtils.isEmpty(domain)) {
                return;
            }

            domain = domain.toLowerCase();
            allDomains.add(domain);

            if (firstDomain == null) {
                firstDomain = domain;
            }
        }
    }

    private String getDomainName(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String domain = "";
        if (uri != null) {
            if (uri.getHost() != null) {
                domain = uri.getHost();
            }
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
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
        ArrayList<Credential> matchingDomainCred = new ArrayList<>();
        ArrayList<Credential> matchingPackageCred = new ArrayList<>();

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
                Log.d(TAG, "Couldn't decode Cred URL to host part:" + ex.toString());
            }

            if (credUri != null && domain.firstDomain != null) {
                if (credUri.equals(domain.firstDomain) || thisCred.getUrl().equals(domain.firstDomain)) {
                    Log.d(TAG, "Matching cred on domain: " + domain.firstDomain);
                    matchingDomainCred.add(thisCred);
                }
            } else if (thisCred.getUrl() != null && domain.firstDomain != null) {
                if (thisCred.getUrl().equals(domain.firstDomain)) {
                    Log.d(TAG, "Matching cred on url: " + domain.firstDomain);
                    matchingDomainCred.add(thisCred);
                }
            }

            try {
                String thisCredCustomFieldsString = thisCred.getCustomFields();
                if (thisCredCustomFieldsString != null) {
                    JSONArray thisCredCustomFields = new JSONArray(thisCredCustomFieldsString);
                    for (int i = 0; i < thisCredCustomFields.length(); i++) {
                        JSONObject thisCredCustomField = thisCredCustomFields.getJSONObject(i);

                        String customFieldLabel = thisCredCustomField.getString("label");

                        if (customFieldLabel.equalsIgnoreCase("androidCredPackageName")) {
                            String credPackageName = thisCredCustomField.getString("value");
                            if (packageName.equalsIgnoreCase(credPackageName)) {
                                matchingPackageCred.add(thisCred);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Log.d(TAG, "Cannot decode custom fields: " + ex.toString());
            }

            if (matchingDomainCred.size() >= MAX_DATASETS) {
                return matchingDomainCred;
            }
            if (matchingPackageCred.size() >= MAX_DATASETS && matchingDomainCred.size() == 0) {
                return matchingPackageCred;
            }
        }
        if (matchingDomainCred.size() > 0) {
            return matchingDomainCred;
        }
        return matchingPackageCred;
    }

    @NonNull
    static WebDomainResult getLikelyDomain(ArrayList<AssistStructure> assistStructures) {
        WebDomainResult res = new WebDomainResult();

        for (AssistStructure assistStructure : assistStructures) {
            int nodes = assistStructure.getWindowNodeCount();
            for (int i = 0; i < nodes; i++) {
                AssistStructure.ViewNode viewNode = assistStructure.getWindowNodeAt(i).getRootViewNode();
                getNodeDomain(viewNode, res);
            }
        }
        Log.d(TAG, "Returning, found :" + String.valueOf(res.allDomains.size()) + " domains.");
        return res;
    }

    static void getNodeDomain(AssistStructure.ViewNode viewNode, WebDomainResult res) {
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
        RemoteViews presentation = new RemoteViews(packageName, R.layout.autofill_list_item);
        presentation.setTextViewText(R.id.autofilltext, text);
        return presentation;
    }

    public void buildAndAddPresentation(@NonNull Dataset.Builder dataset,
                                        @NonNull String packageName,
                                        @NonNull AutofillField field,
                                        @NonNull String value,
                                        @NonNull String displayValue) {
        RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
        dataset.setValue(field.getAutofillid(), AutofillValue.forText(value), presentation);
    }

    @NonNull
    private AutofillFieldCollection getAutofillableFields(@NonNull AssistStructure structure,
                                                          boolean asValue) {
        AutofillFieldCollection fields = new AutofillFieldCollection();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node, asValue);
        }
        return fields;
    }

    private void addAutofillableFields(@NonNull AutofillFieldCollection fields,
                                       @NonNull ViewNode node,
                                       boolean asValue) {
        AutofillField thisField;
        try {
            if (!asValue) {
                thisField = new AutofillField(node.getAutofillId(), node);
            } else {
                thisField = new AutofillField(node.getAutofillValue(), node);
            }
            fields.add(thisField);
        } catch (Exception ex) {
            Log.d(TAG, "Couldn't add node to fields: " + ex.toString());
        }

        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i), asValue);
        }
    }

    private Vault getAutofillVault(SingleTon ton) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        KeyStoreUtils.initialize(settings);
        Vault activeVault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        String autofillVaultGuid = settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), null);

        if (activeVault != null && !activeVault.guid.equals(autofillVaultGuid) && !autofillVaultGuid.equals("")) {
            try {
                Vault requestedVault = Vault.fromJSON(new JSONObject(KeyStoreUtils.getString(SettingValues.AUTOFILL_VAULT.toString(), "")));
                requestedVault.unlock(KeyStoreUtils.getString(autofillVaultGuid, ""));
                return requestedVault;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return activeVault;
    }
}
