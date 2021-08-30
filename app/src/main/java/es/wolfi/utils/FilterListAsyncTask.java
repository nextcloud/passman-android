/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package es.wolfi.utils;

import android.os.AsyncTask;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import es.wolfi.app.passman.fragments.CredentialItemFragment;
import es.wolfi.app.passman.adapters.CredentialViewAdapter;
import es.wolfi.app.passman.fragments.VaultFragment;
import es.wolfi.app.passman.adapters.VaultViewAdapter;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

public class FilterListAsyncTask <T extends Filterable> extends AsyncTask<ArrayList<T>, Integer, ArrayList<T>>{

    private String filter;
    RecyclerView recyclerView;
    CredentialItemFragment.OnListFragmentInteractionListener credentialMListener = null;
    VaultFragment.OnListFragmentInteractionListener vaultMListener = null;
    Boolean isVaultFragment;

    public FilterListAsyncTask(String filter, RecyclerView recyclerView, CredentialItemFragment.OnListFragmentInteractionListener mListener){
        this.filter = filter;
        this.recyclerView = recyclerView;
        this.credentialMListener = mListener;
        this.isVaultFragment = false;
    }

    public FilterListAsyncTask(String filter, RecyclerView recyclerView, VaultFragment.OnListFragmentInteractionListener mListener){
        this.filter = filter;
        this.recyclerView = recyclerView;
        this.vaultMListener = mListener;
        this.isVaultFragment = true;
    }

    @Override
    protected ArrayList<T> doInBackground(ArrayList<T>... list) {
        return new ListUtils().filterList(filter, list[0]);
    }

    @Override
    protected void onPostExecute(ArrayList<T> filteredList){
        if(isVaultFragment){
            recyclerView.setAdapter(new VaultViewAdapter((ArrayList<Vault>)filteredList, vaultMListener));
        }
        else {
            recyclerView.setAdapter(new CredentialViewAdapter((ArrayList<Credential>) filteredList, credentialMListener));
        }
    }
}
