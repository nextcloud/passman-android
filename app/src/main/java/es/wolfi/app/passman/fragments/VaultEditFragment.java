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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.app.ResponseHandlers.VaultSaveResponseHandler;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.ProgressUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VaultEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VaultEditFragment extends Fragment implements View.OnClickListener {
    public static String VAULT = "vault";

    @BindView(R.id.edit_vault_name_header)
    TextView edit_vault_name_header;
    @BindView(R.id.edit_vault_name)
    EditText edit_vault_name;

    private Vault vault;
    private AtomicBoolean alreadySaving = new AtomicBoolean(false);

    public VaultEditFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment VaultEditFragment.
     */
    public static VaultEditFragment newInstance(String vaultGUID) {
        VaultEditFragment fragment = new VaultEditFragment();

        Bundle b = new Bundle();
        b.putString(VAULT, vaultGUID);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vault_edit, container, false);

        FloatingActionButton saveVaultButton = view.findViewById(R.id.SaveVaultButton);
        saveVaultButton.setOnClickListener(this);
        saveVaultButton.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.vault = (Vault) SingleTon.getTon().getExtra(getArguments().getString(VAULT));
        }
    }

    @Override
    public void onClick(View view) {
        if (alreadySaving.get()) {
            return;
        }

        if (edit_vault_name.getText().toString().equals("")) {
            edit_vault_name_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        } else {
            edit_vault_name_header.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        alreadySaving.set(true);

        this.vault.setName(edit_vault_name.getText().toString());

        Context context = getContext();
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
        final AsyncHttpResponseHandler responseHandler = new VaultSaveResponseHandler(alreadySaving, true, this.vault, 0, progress, view, (PasswordListActivity) getActivity(), getFragmentManager());

        this.vault.edit(context, responseHandler);
    }
}
