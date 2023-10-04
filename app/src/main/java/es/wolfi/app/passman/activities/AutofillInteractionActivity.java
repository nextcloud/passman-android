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

import static android.service.autofill.SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE;
import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;

import android.app.assist.AssistStructure;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;

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
import es.wolfi.app.passman.fragments.VaultLockScreenFragment;
import es.wolfi.passman.API.Vault;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillInteractionActivity extends AppCompatActivity implements VaultLockScreenFragment.VaultUnlockInteractionListener {
    public final static String LOG_TAG = "AutofillInteractionAct.";

    public static final int REQUEST_CODE_AUTOFILL_VAULT_UNLOCK = 1;
    public final static String GENERATE_AUTOFILL_VAULT_UNLOCK_INTENT_ACTION = "custom.actions.intent.AUTOFILL_VAULT_UNLOCK";

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
        if (intent != null && intent.getAction().equals(GENERATE_AUTOFILL_VAULT_UNLOCK_INTENT_ACTION)) {
            Log.d(LOG_TAG, "in intent equal condition");

            structures = intent.getParcelableArrayListExtra("structures");
            packageName = intent.getStringExtra("packageName");
            requesterPackageName = intent.getStringExtra("requesterPackageName");

            SingleTon ton = SingleTon.getTon();
            Vault vault = AutofillHelper.getAutofillVault(ton, getBaseContext());

            Log.d(LOG_TAG, "open VaultLockScreenFragment");
            VaultLockScreenFragment lf = VaultLockScreenFragment.newInstance(vault);

            //setContentView(lf.getView());
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                    .replace(R.id.frameContainer, lf, "vault_lockscreen")
                    .commitNow();
            //setContentView(lf.getView());
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

        /*
            Let android know we want to save any credentials manually entered by the user.
            We will save usernames, passwords and email addresses.
         */

        if (tempFields.size() > 0) {
            Log.d(LOG_TAG, "Requesting save info");

            AutofillId[] requiredIds = new AutofillId[tempFields.size()];
            tempFields.toArray(requiredIds);
            fillResponse.setSaveInfo(
                    new SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            requiredIds)
                            .setFlags(FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                            .build());

            Log.d(LOG_TAG, "Building and calling success");

            // Send the data back to the service
            replyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse.build());
            setResult(RESULT_OK, replyIntent);
        } else {
            Log.d(LOG_TAG, "Failed to find anything to do, bailing");
        }

        setResult(RESULT_OK, replyIntent);
        finish();
    }
}
