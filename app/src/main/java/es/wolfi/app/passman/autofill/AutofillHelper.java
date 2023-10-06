/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2023, Timo Triebensky (timo@binsky.org)
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

import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.KeyStoreUtils;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillHelper {
    private static final String LOG_TAG = "AutofillHelper";
    private static final int MAX_DATASETS = 4;

    /**
     * Returns the autofill vault, if there was one selected in the app settings.
     * As fallback it returns the active fault if the app is active and any vault open.
     * Otherwise it returns null.
     *
     * @param ton previously initialized SingleTon
     * @return null or Vault
     */
    public static Vault getAutofillVault(SingleTon ton, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        KeyStoreUtils.initialize(settings);

        // active vault should be unlocked by default
        Vault activeVault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        String autofillVaultGuid = settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), null);

        if (activeVault == null || !activeVault.guid.equals(autofillVaultGuid)) {
            // assume autofill vault is not the active vault
            if (autofillVaultGuid != null && !autofillVaultGuid.equals("")) {
                try {
                    Vault autofillVault = Vault.fromJSON(new JSONObject(KeyStoreUtils.getString(SettingValues.AUTOFILL_VAULT.toString(), "")));
                    // try to unlock with optional saved access password, otherwise return locked vault
                    autofillVault.unlock(KeyStoreUtils.getString(autofillVaultGuid, ""));
                    return autofillVault;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return activeVault;
    }

    /**
     * @param response             FillResponse.Builder to which the matching fields will be appended
     * @param vault                opened/decrypted vault
     * @param packageName          Passman package name
     * @param requesterPackageName package name of the app that was requesting the autofill service
     * @param fields               fillable fields
     * @param structures
     * @return set of tempFields
     */
    public static Set<AutofillId> fillResponseForDecryptedVault(
            FillResponse.Builder response,
            Vault vault,
            String packageName,
            String requesterPackageName,
            AutofillFieldCollection fields,
            ArrayList<AssistStructure> structures
    ) {
        AutofillField bestUsername = AutofillHelper.getUsernameField(fields);
        AutofillField bestEmail = AutofillHelper.getEmailField(fields);
        AutofillField bestPassword = AutofillHelper.getPasswordField(fields);

        WebDomainResult domain = getLikelyDomain(structures);

        // Grab Credentials from vault
        ArrayList<Credential> allCred = vault.getCredentials();

        // Find the credentials which match the requesting package name

        List<Credential> matchingCredentials = findMatchingCredentials(allCred, requesterPackageName, domain);

        Log.d(LOG_TAG, "Number of matching credentials for package: " +
                requesterPackageName +
                ":" +
                matchingCredentials.size());

        /*
         * TODO: validate package signature
         * (Maybe store apk signature or signing cert thumbprint in custom field)
         */

        Set<AutofillId> tempFields = new HashSet<>();

        for (Credential thisCred : matchingCredentials) {

            String credLabel = returnBestString(thisCred.getLabel(), thisCred.getUrl());

            Dataset.Builder dataset = new Dataset.Builder();

            if (bestUsername != null) {
                String value = returnBestString(thisCred.getUsername(), thisCred.getEmail());

                buildAndAddPresentation(dataset,
                        packageName,
                        bestUsername,
                        value,
                        "Username for: " + credLabel);
                Log.d(LOG_TAG, "add: Username for: " + credLabel);
                tempFields.add(bestUsername.getAutofillid());
            }

            if (bestEmail != null) {
                String value = returnBestString(thisCred.getEmail(), thisCred.getUsername());

                buildAndAddPresentation(dataset,
                        packageName,
                        bestEmail,
                        value,
                        "Email for: " + credLabel);
                Log.d(LOG_TAG, "add: Email for: " + credLabel);
                tempFields.add(bestEmail.getAutofillid());
            }

            if (bestPassword != null) {
                String value = thisCred.getPassword();

                buildAndAddPresentation(dataset,
                        packageName,
                        bestPassword,
                        value,
                        "Password for: " + credLabel);
                Log.d(LOG_TAG, "add: Password for: " + credLabel);
                tempFields.add(bestPassword.getAutofillid());
            }

            if (bestUsername != null || bestEmail != null || bestPassword != null) {
                response.addDataset(dataset.build());
            }
        }

        return tempFields;
    }

    @NonNull
    private static String returnBestString(@NonNull String... usernameOptions) {
        for (String thisUsernameOption : usernameOptions) {
            if (!TextUtils.isEmpty(thisUsernameOption) && !thisUsernameOption.equals("null")) {
                return thisUsernameOption;
            }
        }
        return "";
    }

    public static class WebDomainResult {
        String firstDomain = null;
        HashSet<String> allDomains;

        public WebDomainResult() {
            allDomains = new HashSet<>();
            Log.d(LOG_TAG, "Web Domain Result constructed");
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

    public static String getDomainName(String url) {
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
    static WebDomainResult getLikelyDomain(ArrayList<AssistStructure> assistStructures) {
        WebDomainResult res = new WebDomainResult();

        for (AssistStructure assistStructure : assistStructures) {
            int nodes = assistStructure.getWindowNodeCount();
            for (int i = 0; i < nodes; i++) {
                AssistStructure.ViewNode viewNode = assistStructure.getWindowNodeAt(i).getRootViewNode();
                getNodeDomain(viewNode, res);
            }
        }
        Log.d(LOG_TAG, "Returning, found :" + String.valueOf(res.allDomains.size()) + " domains.");
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

    private static List<Credential> findMatchingCredentials(
            @NonNull ArrayList<Credential> credentialArrayList,
            @NonNull String packageName,
            @NonNull WebDomainResult domain) {
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
                Log.d(LOG_TAG, "Couldn't decode Cred URL to host part:" + ex.toString());
            }

            if (credUri != null && domain.firstDomain != null) {
                if (credUri.equals(domain.firstDomain) || thisCred.getUrl().equals(domain.firstDomain)) {
                    Log.d(LOG_TAG, "Matching cred on domain: " + domain.firstDomain);
                    matchingDomainCred.add(thisCred);
                }
            } else if (thisCred.getUrl() != null && domain.firstDomain != null) {
                if (thisCred.getUrl().equals(domain.firstDomain)) {
                    Log.d(LOG_TAG, "Matching cred on url: " + domain.firstDomain);
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
                Log.d(LOG_TAG, "Cannot decode custom fields: " + ex.toString());
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
    static RemoteViews newDatasetPresentation(@NonNull String packageName,
                                              @NonNull CharSequence text) {
        RemoteViews presentation = new RemoteViews(packageName, R.layout.autofill_list_item);
        presentation.setTextViewText(R.id.autofilltext, text);
        return presentation;
    }

    public static void buildAndAddPresentation(@NonNull Dataset.Builder dataset,
                                               @NonNull String packageName,
                                               @NonNull AutofillField field,
                                               @NonNull String value,
                                               @NonNull String displayValue) {
        RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
        dataset.setValue(field.getAutofillid(), AutofillValue.forText(value), presentation);
    }

    @NonNull
    public static AutofillFieldCollection getAutofillableFields(@NonNull AssistStructure structure,
                                                                boolean asValue) {
        AutofillFieldCollection fields = new AutofillFieldCollection();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            AssistStructure.ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node, asValue);
        }
        return fields;
    }

    private static void addAutofillableFields(@NonNull AutofillFieldCollection fields,
                                              @NonNull AssistStructure.ViewNode node,
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
            Log.d(LOG_TAG, "Couldn't add node to fields: " + ex.toString());
        }

        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i), asValue);
        }
    }

    public static AutofillField getUsernameField(AutofillFieldCollection fields) {
        return fields.getRequiredId(View.AUTOFILL_HINT_USERNAME);
    }

    public static AutofillField getEmailField(AutofillFieldCollection fields) {
        AutofillField emailField = fields.getRequiredId(View.AUTOFILL_HINT_EMAIL_ADDRESS);
        if (emailField == null) {
            emailField = fields.getRequiredId("email");
        }
        return emailField;
    }

    public static AutofillField getPasswordField(AutofillFieldCollection fields) {
        AutofillField passwordField = fields.getRequiredId(View.AUTOFILL_HINT_PASSWORD);
        if (passwordField == null) {
            passwordField = fields.getRequiredId("current-password");
        }
        return passwordField;
    }
}
