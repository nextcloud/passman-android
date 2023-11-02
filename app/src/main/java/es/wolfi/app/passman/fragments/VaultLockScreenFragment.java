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

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.databinding.FragmentVaultLockScreenBinding;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.KeyStoreUtils;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VaultUnlockInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VaultLockScreenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VaultLockScreenFragment extends Fragment {
    private Vault vault;
    private VaultUnlockInteractionListener mListener;
    private FragmentVaultLockScreenBinding binding;

    TextInputLayout input_layout_password;
    TextView vault_name;
    EditText vault_password;
    FloatingActionButton btn_unlock;
    CheckBox chk_save;

    public VaultLockScreenFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param vault The vault
     * @return A new instance of fragment VaultLockScreenFragment.
     */
    public static VaultLockScreenFragment newInstance(Vault vault) {
        VaultLockScreenFragment fragment = new VaultLockScreenFragment();
        fragment.vault = vault;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentVaultLockScreenBinding.inflate(inflater, container, false);

        input_layout_password = binding.inputLayoutPassword;
        vault_name = binding.fragmentVaultName;
        vault_password = binding.fragmentVaultPassword;
        btn_unlock = binding.fragmentVaultUnlock;
        chk_save = binding.vaultLockScreenChkSavePw;

        return binding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof VaultUnlockInteractionListener) {
            mListener = (VaultUnlockInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement VaultUnlockInteractionListener");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (vault != null) {
            Log.e("VaultLockScreenFragment", "Vault guid: ".concat(vault.guid));
            vault_name.setText(vault.name);
            input_layout_password.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

            binding.fragmentVaultUnlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBtnUnlockClick();
                }
            });
        } else {
            Toast.makeText(getContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    void onBtnUnlockClick() {
        if (vault.unlock(vault_password.getText().toString())) {
            if (chk_save.isChecked()) {
                KeyStoreUtils.putStringAndCommit(vault.guid, vault_password.getText().toString());
            }
            mListener.onVaultUnlock(vault);
            return;
        }
        Toast.makeText(getContext(), R.string.wrong_vault_pw, Toast.LENGTH_LONG).show();
        input_layout_password.setHintTextColor(ColorStateList.valueOf(getResources().getColor(R.color.danger)));
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface VaultUnlockInteractionListener {
        void onVaultUnlock(Vault vault);
    }
}
