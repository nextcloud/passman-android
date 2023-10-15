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

package es.wolfi.app.passman.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import es.wolfi.app.ResponseHandlers.VaultSaveResponseHandler;
import es.wolfi.app.passman.EditPasswordTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.app.passman.databinding.FragmentVaultAddBinding;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.ProgressUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VaultAddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VaultAddFragment extends Fragment implements View.OnClickListener {
    private FragmentVaultAddBinding binding;

    TextView add_vault_name_header;
    EditText add_vault_name;

    TextView add_vault_password_header;
    EditPasswordTextItem add_vault_password;

    TextView add_vault_password_repeat_header;
    EditPasswordTextItem add_vault_password_repeat;

    Spinner add_vault_sharing_key_strength;

    private Vault vault;
    private AtomicBoolean alreadySaving = new AtomicBoolean(false);

    public VaultAddFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment VaultAddFragment.
     */
    public static VaultAddFragment newInstance() {
        return new VaultAddFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentVaultAddBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        FloatingActionButton saveVaultButton = view.findViewById(R.id.SaveVaultButton);
        saveVaultButton.setOnClickListener(this);
        saveVaultButton.setVisibility(View.VISIBLE);

        add_vault_name_header = binding.addVaultNameHeader;
        add_vault_name = binding.addVaultName;
        add_vault_password_header = binding.addVaultPasswordHeader;
        add_vault_password = binding.addVaultPassword;
        add_vault_password_repeat_header = binding.addVaultPasswordRepeatHeader;
        add_vault_password_repeat = binding.addVaultPasswordRepeat;
        add_vault_sharing_key_strength = binding.addVaultSharingKeyStrength;

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        add_vault_password.setPasswordGenerationButtonVisibility(false);
        add_vault_password_repeat.setPasswordGenerationButtonVisibility(false);

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, Vault.keyStrengths);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        add_vault_sharing_key_strength.setAdapter(adapter);
        add_vault_sharing_key_strength.setSelection(1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.vault = new Vault();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View view) {
        if (alreadySaving.get()) {
            return;
        }

        if (add_vault_name.getText().toString().equals("")) {
            add_vault_name_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        } else {
            add_vault_name_header.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        if (!add_vault_password.getText().toString().equals(add_vault_password_repeat.getText().toString())) {
            add_vault_password_header.setTextColor(getResources().getColor(R.color.danger));
            add_vault_password_repeat_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        } else {
            add_vault_password_header.setTextColor(getResources().getColor(R.color.colorAccent));
            add_vault_password_repeat_header.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        alreadySaving.set(true);

        this.vault.setName(add_vault_name.getText().toString());
        this.vault.setEncryptionKey(add_vault_password.getText().toString());
        int keyStrength = Integer.parseInt(add_vault_sharing_key_strength.getSelectedItem().toString());

        Context context = getContext();
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
        final AsyncHttpResponseHandler responseHandler = new VaultSaveResponseHandler(alreadySaving, false, this.vault, keyStrength, progress, view, (PasswordListActivity) getActivity(), getFragmentManager());

        this.vault.save(context, responseHandler);
    }
}
