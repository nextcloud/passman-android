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

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.text.TextUtils;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.wolfi.app.ResponseHandlers.AutofillCredentialSaveResponseHandler;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.activities.AutofillInteractionActivity;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

@RequiresApi(api = Build.VERSION_CODES.O)
public final class CredentialAutofillService extends AutofillService {

    private static final String LOG_TAG = "CredentialAutofillSvc";

    public static HashSet<String> blacklistedPackageNames = new HashSet<String>() {
        {
            add("android");
            add("com.android.settings");
            add("es.wolfi.app.passman");        // github and fdroid releases
            add("es.wolfi.app.passman.alpha");  // alpha and gplay releases
            add("es.wolfi.app.passman.debug");  // testing
        }
    };

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback) {
        Log.d(LOG_TAG, "onFillRequest()");

        List<FillContext> fillContexts = request.getFillContexts();
        ArrayList<AssistStructure> structures = new ArrayList<>();

        for (FillContext fc : fillContexts) {
            structures.add(fc.getStructure());
        }

        final AssistStructure latestAssistStructure = structures.get(structures.size() - 1);

        // Find autofillable fields
        AutofillFieldCollection fields = AutofillHelper.getAutofillableFields(latestAssistStructure, false);

        final String packageName = getApplicationContext().getPackageName();

        final String requesterPackageName = latestAssistStructure.getActivityComponent().getPackageName();

        Log.d(LOG_TAG, "autofillable fields for: " + requesterPackageName + ":");
        for (AutofillField f : fields) {
            Log.d(LOG_TAG, "field: " + f.getHints().toString());
        }

        // We don't have any fields to work with
        // Passman should not authenticate itself (see blacklistedPackageNames)
        if (fields.isEmpty() || blacklistedPackageNames.contains(requesterPackageName)) {
            Log.d(LOG_TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess(null);
            return;
        }

        // Create the base response
        FillResponse.Builder response = new FillResponse.Builder();

        // Open Vault
        SingleTon ton = SingleTon.getTon();
        final Vault v = AutofillHelper.getAutofillVault(ton, getBaseContext());

        if (v == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_noactivevault), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, getString(R.string.autofill_noactivevault));
            callback.onSuccess(null);
            return;
        }

        if (!v.is_unlocked()) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_vaultlocked), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, getString(R.string.autofill_vaultlocked));

            RemoteViews vaultUnlockPresentation = new RemoteViews(getPackageName(), R.layout.autofill_list_item_with_icon);
            vaultUnlockPresentation.setTextViewText(R.id.autofilltext, "Unlock autofill vault");

            Intent authIntent = new Intent(this, AutofillInteractionActivity.class);
            authIntent.setAction(AutofillInteractionActivity.CustomAutofillIntentActions.VAULT_UNLOCK.name());
            authIntent.putExtra("packageName", packageName);
            authIntent.putExtra("requesterPackageName", requesterPackageName);
            authIntent.putParcelableArrayListExtra("structures", structures);

            setupCustomAutofillIntentItem(this, response, fields, vaultUnlockPresentation, authIntent);

            callback.onSuccess(response.build());
            return;
        }

        // If we get here, we have an already unlocked vault
        Log.d(LOG_TAG, "Vault ready to go");

        // Grab Credentials from vault
        ArrayList<Credential> allCred = v.getCredentials();

        if (allCred.isEmpty()) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_vaultempty), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, getString(R.string.autofill_vaultempty));
            callback.onSuccess(null);
            return;
        }

        Set<AutofillId> tempFields = AutofillHelper.fillResponseForDecryptedVault(
                response,
                v,
                packageName,
                requesterPackageName,
                fields,
                structures
        );

        if (tempFields.size() > 0) {
            AutofillHelper.fillResponseWithSaveInfo(response, tempFields);

            Log.d(LOG_TAG, "Building and calling success");
            callback.onSuccess(response.build());
        } else {
            Log.d(LOG_TAG, "No matching credentials were found to fill out");
            Toast.makeText(getApplicationContext(), getString(R.string.no_matching_credentials_found), Toast.LENGTH_SHORT).show();

            // check enable manual search as fallback
            if (true) {
                // show icon item to open activity with vault list fragment

                RemoteViews vaultUnlockPresentation = new RemoteViews(getPackageName(), R.layout.autofill_list_item_with_icon);
                vaultUnlockPresentation.setTextViewText(R.id.autofilltext, "Manual search");

                Intent authIntent = new Intent(this, AutofillInteractionActivity.class);
                authIntent.setAction(AutofillInteractionActivity.CustomAutofillIntentActions.MANUAL_SEARCH.name());
                authIntent.putExtra("packageName", packageName);
                authIntent.putExtra("requesterPackageName", requesterPackageName);
                authIntent.putParcelableArrayListExtra("structures", structures);

                setupCustomAutofillIntentItem(this, response, fields, vaultUnlockPresentation, authIntent);
                callback.onSuccess(response.build());
            } else {
                callback.onSuccess(null);
            }
        }
    }

    public static void setupCustomAutofillIntentItem(
            Context context,
            FillResponse.Builder response,
            AutofillFieldCollection fields,
            RemoteViews presentation,
            Intent itemActionIntent
    ) {
        IntentSender intentSender = PendingIntent.getActivity(
                context,
                AutofillInteractionActivity.REQUEST_CODE_AUTOFILL_PLACEHOLDER,
                itemActionIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ).getIntentSender();

        AutofillField bestUsername = AutofillHelper.getUsernameField(fields);
        AutofillField bestEmail = AutofillHelper.getEmailField(fields);
        AutofillField bestPassword = AutofillHelper.getPasswordField(fields);

        ArrayList<AutofillId> ids = new ArrayList<>();
        if (bestUsername != null) {
            ids.add(bestUsername.getAutofillid());
        }
        if (bestEmail != null) {
            ids.add(bestEmail.getAutofillid());
        }
        if (bestPassword != null) {
            ids.add(bestPassword.getAutofillid());
        }

        response.setAuthentication(ids.toArray(new AutofillId[0]), intentSender, presentation);
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        Log.d(LOG_TAG, "onSaveRequest()");
        List<FillContext> fillContexts = request.getFillContexts();
        final AssistStructure latestStructure = fillContexts.get(fillContexts.size() - 1).getStructure();

        final String requesterPackageName = latestStructure.getActivityComponent().getPackageName();
        String requesterDomainName = null;
        String requesterApplicationLabel = null;
        final Context context = getBaseContext();

        // Find autofillable fields
        ArrayList<AssistStructure> structures = new ArrayList<>();

        for (FillContext fc : request.getFillContexts()) {
            structures.add(fc.getStructure());
        }

        AutofillHelper.WebDomainResult domain = AutofillHelper.getLikelyDomain(structures);

        if (domain.firstDomain != null) {
            requesterDomainName = domain.firstDomain;
        } else {
            requesterDomainName = "";
        }

        AutofillFieldCollection fields = AutofillHelper.getAutofillableFields(latestStructure, true);

        // We don't have any fields to work with
        if (fields.isEmpty()) {
            Log.d(LOG_TAG, "No autofillable fields for: " + requesterPackageName);
            callback.onSuccess();
            return;
        }

        SingleTon ton = SingleTon.getTon();
        final Vault v = AutofillHelper.getAutofillVault(ton, context);

        if (v == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_noactivevault), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, getString(R.string.autofill_noactivevault));
            callback.onSuccess();
            return;
        }

        if (!v.is_unlocked()) {
            Toast.makeText(getApplicationContext(), getString(R.string.autofill_vaultlocked), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, getString(R.string.autofill_vaultlocked));
            callback.onSuccess();
            return;
        }

        try {
            ApplicationInfo requesterAppInfo = getPackageManager().getApplicationInfo(requesterPackageName, 0);
            requesterApplicationLabel = getPackageManager().getApplicationLabel(requesterAppInfo).toString();
        } catch (Exception ex) {
            Log.d(LOG_TAG, "Couldn't read application label for: " + requesterPackageName);
        }

        if (TextUtils.isEmpty(requesterApplicationLabel)) {
            requesterApplicationLabel = requesterPackageName;
        }

        Log.d(LOG_TAG, "onSaveRequest(): Application: " + requesterApplicationLabel);

        if (!requesterDomainName.equals("")) {
            String parsedDomain = AutofillHelper.getDomainName(requesterDomainName);
            if (parsedDomain.equals("")) {
                parsedDomain = requesterDomainName;
            }
            requesterApplicationLabel += " - " + parsedDomain;
        }

        AutofillField bestUsername = AutofillHelper.getUsernameField(fields);
        AutofillField bestEmail = AutofillHelper.getEmailField(fields);
        AutofillField bestPassword = AutofillHelper.getPasswordField(fields);

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
            Log.e(LOG_TAG, "onSaveRequest(), error creating customField");
        }

        Log.d(LOG_TAG, "onSaveRequest(), building Credential");

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

        Log.d(LOG_TAG, "onSaveRequest(), saving Credential");

        final AsyncHttpResponseHandler responseHandler = new AutofillCredentialSaveResponseHandler(
                AutofillHelper.getAutofillVault(ton, context), context, getApplicationContext(), ton, LOG_TAG
        );
        newCred.save(getApplicationContext(), responseHandler);

        Log.d(LOG_TAG, "onSaveRequest() finished");
        callback.onSuccess();
    }
}
