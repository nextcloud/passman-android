/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

package es.wolfi.app.passman.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.app.ResponseHandlers.VaultDeleteResponseHandler;
import es.wolfi.app.passman.EditPasswordTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.ProgressUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VaultDeleteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VaultDeleteFragment extends Fragment implements View.OnClickListener {
    public static String VAULT = "vault";

    @BindView(R.id.vault_name)
    TextView vault_name;
    @BindView(R.id.delete_vault_password_header)
    TextView delete_vault_password_header;
    @BindView(R.id.delete_vault_password)
    EditPasswordTextItem delete_vault_password;

    private Vault vault;
    private AtomicBoolean alreadySaving = new AtomicBoolean(false);

    public VaultDeleteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment VaultEditFragment.
     */
    public static VaultDeleteFragment newInstance(String vaultGUID) {
        VaultDeleteFragment fragment = new VaultDeleteFragment();

        Bundle b = new Bundle();
        b.putString(VAULT, vaultGUID);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vault_delete, container, false);

        FloatingActionButton deleteVaultButton = view.findViewById(R.id.DeleteVaultButton);
        deleteVaultButton.setOnClickListener(this);
        deleteVaultButton.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        vault_name.setText(vault.getName());
        delete_vault_password.setPasswordGenerationButtonVisibility(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            try {
                // copy vault to not lock it if it ws unlocked before, and a wrong password is entered
                vault = Vault.fromJSON(new JSONObject(Vault.asJson(Vault.getVaultByGuid(getArguments().getString(VAULT)))));
            } catch (JSONException e) {
                e.printStackTrace();
                vault = Vault.getVaultByGuid(getArguments().getString(VAULT));
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (!vault.unlock(delete_vault_password.getText().toString())) {
            delete_vault_password_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        } else {
            delete_vault_password_header.setTextColor(getResources().getColor(R.color.colorAccent));
        }
        Context context = view.getContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(view.getContext().getString(R.string.confirm_vault_deletion) + " (" + vault.getName() + ")");
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (alreadySaving.get()) {
                    return;
                }
                alreadySaving.set(true);
                final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
                final AsyncHttpResponseHandler responseHandler = new VaultDeleteResponseHandler(alreadySaving, vault, progress, view, (PasswordListActivity) getActivity(), getFragmentManager());
                vault.delete(context, responseHandler);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
