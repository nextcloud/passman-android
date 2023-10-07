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
package es.wolfi.app.passman.activities;

import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;

import android.app.assist.AssistStructure;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.FillResponse;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Set;

import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.autofill.AutofillFieldCollection;
import es.wolfi.app.passman.autofill.AutofillHelper;
import es.wolfi.app.passman.databinding.ActivityAutofillInteractionBinding;
import es.wolfi.app.passman.fragments.CredentialItemFragment;
import es.wolfi.app.passman.fragments.VaultLockScreenFragment;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillInteractionActivity extends AppCompatActivity implements
        VaultLockScreenFragment.VaultUnlockInteractionListener,
        CredentialItemFragment.OnListFragmentInteractionListener {
    public final static String LOG_TAG = "AutofillInteractionAct.";

    public static final int REQUEST_CODE_AUTOFILL_PLACEHOLDER = 1;

    public enum CustomAutofillIntentActions {
        VAULT_UNLOCK("VAULT_UNLOCK"),
        MANUAL_SEARCH("MANUAL_SEARCH");

        private final String name;

        CustomAutofillIntentActions(final String name) {
            this.name = name;
        }
    }

    private ArrayList<AssistStructure> structures;
    private String packageName;
    private String requesterPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityAutofillInteractionBinding binding = ActivityAutofillInteractionBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Log.d(LOG_TAG, "in onCreate");
        new OfflineStorage(getBaseContext());

        Intent intent = getIntent();
        if (intent != null) {
            structures = intent.getParcelableArrayListExtra("structures");
            packageName = intent.getStringExtra("packageName");
            requesterPackageName = intent.getStringExtra("requesterPackageName");

            SingleTon ton = SingleTon.getTon();
            Vault vault = AutofillHelper.getAutofillVault(ton, getBaseContext());

            if (intent.getAction().equals(CustomAutofillIntentActions.VAULT_UNLOCK.name)) {
                Log.d(LOG_TAG, "open VaultLockScreenFragment");
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                        .replace(R.id.frameContainer, VaultLockScreenFragment.newInstance(vault), "vault_lockscreen")
                        .commitNow();
            } else if (intent.getAction().equals(CustomAutofillIntentActions.MANUAL_SEARCH.name)) {
                // assume that vault is already unlocked when we are here

                Log.d(LOG_TAG, "open CredentialItemFragment");
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                        .replace(R.id.frameContainer, new CredentialItemFragment(vault, true), "vault")
                        .commitNow();
            }
        }
    }

    @Override
    public void onVaultUnlock(Vault vault) {
        Log.d(LOG_TAG, "onVaultUnlock " + vault.name);

        FillResponse.Builder fillResponse = new FillResponse.Builder();
        Intent replyIntent = new Intent();

        // Find autofillable fields
        AutofillFieldCollection fields = AutofillHelper.getAutofillableFields(structures.get(structures.size() - 1), false);

        Set<AutofillId> tempFields = AutofillHelper.fillResponseForDecryptedVault(
                fillResponse,
                vault,
                packageName,
                requesterPackageName,
                fields,
                structures
        );

        if (tempFields.size() > 0) {
            AutofillHelper.fillResponseWithSaveInfo(fillResponse, tempFields);

            // Send the data back to the service
            Log.d(LOG_TAG, "Building and calling success");
            replyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse.build());
            setResult(RESULT_OK, replyIntent);
            finish();
        } else {
            Log.d(LOG_TAG, "No matching credentials were found to fill out");
            Toast.makeText(getApplicationContext(), getString(R.string.no_matching_credentials_found), Toast.LENGTH_SHORT).show();

            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                    .replace(R.id.frameContainer, new CredentialItemFragment(vault, true), "vault")
                    .commitNow();
        }
    }

    @Override
    public void onListFragmentInteraction(Credential item) {
        Log.d(LOG_TAG, "onListFragmentInteraction " + item.getLabel());

        FillResponse.Builder fillResponse = new FillResponse.Builder();
        Intent replyIntent = new Intent();

        // Find autofillable fields
        AutofillFieldCollection fields = AutofillHelper.getAutofillableFields(structures.get(structures.size() - 1), false);

        Set<AutofillId> tempFields = AutofillHelper.fillResponseForExplicitCredential(
                fillResponse,
                item,
                packageName,
                requesterPackageName,
                fields
        );

        if (tempFields.size() > 0) {
            AutofillHelper.fillResponseWithSaveInfo(fillResponse, tempFields);
        } else {
            Log.d(LOG_TAG, "Failed to find matching fields");
        }

        // Send the data back to the service
        Log.d(LOG_TAG, "Building and calling success");
        replyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse.build());
        setResult(RESULT_OK, replyIntent);
        finish();
    }

    @Override
    //unused//
    public void setLastCredentialListPosition(int pos) {
    }

    @Override
    //unused//
    public int getLastCredentialListPosition() {
        return 0;
    }
}
