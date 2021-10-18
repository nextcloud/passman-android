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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.adapters.CredentialViewAdapter;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.CredentialLabelSort;
import es.wolfi.utils.FilterListAsyncTask;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class CredentialItemFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private int sortMethod = CredentialLabelSort.SortMethod.STANDARD.ordinal();
    private OnListFragmentInteractionListener mListener;
    private AsyncTask filterTask = null;
    private RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CredentialItemFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static CredentialItemFragment newInstance(int columnCount) {
        CredentialItemFragment fragment = new CredentialItemFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    public void loadCredentialList(View view) {
        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        final EditText searchInput = (EditText) view.findViewById(R.id.search_input);
        final AppCompatImageButton toggleSortButton = (AppCompatImageButton) view.findViewById(R.id.toggle_sort_button);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                applyFilters(v, searchInput);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        toggleSortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortMethod = (++sortMethod % 3);
                updateToggleSortButtonImage(toggleSortButton);

                v.sort(sortMethod);
                applyFilters(v, searchInput);
            }
        });
        v.sort(sortMethod);
        recyclerView.setAdapter(new CredentialViewAdapter(v.getCredentials(), mListener, PreferenceManager.getDefaultSharedPreferences(getContext())));
        scrollToLastPosition();
        updateToggleSortButtonImage(toggleSortButton);
    }

    public void applyFilters(Vault vault, EditText searchInput) {
        String searchText = searchInput.getText().toString().toLowerCase();
        if (filterTask != null) {
            filterTask.cancel(true);
        }
        filterTask = new FilterListAsyncTask(searchText, recyclerView, mListener);
        ArrayList<Credential> input[] = new ArrayList[]{vault.getCredentials()};
        filterTask.execute((Object[]) input);
    }

    public void updateToggleSortButtonImage(AppCompatImageButton toggleSortButton) {
        if (sortMethod == CredentialLabelSort.SortMethod.STANDARD.ordinal()) {
            // set default image
            toggleSortButton.setImageResource(R.drawable.ic_baseline_list_24);
        } else if (sortMethod == CredentialLabelSort.SortMethod.ALPHABETICALLY_ASCENDING.ordinal()) {
            // set az ascending image
            toggleSortButton.setImageResource(R.drawable.ic_baseline_sort_by_alpha_24);
        } else if (sortMethod == CredentialLabelSort.SortMethod.ALPHABETICALLY_DESCENDING.ordinal()) {
            // set az descending image
            toggleSortButton.setImageResource(R.drawable.ic_baseline_sort_by_alpha_24);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credential_item_list, container, false);

        // Set the adapter
        View credentialView = view.findViewById(R.id.list);
        if (credentialView instanceof RecyclerView) {
            Context context = credentialView.getContext();
            recyclerView = (RecyclerView) credentialView;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            loadCredentialList(view);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    public void scrollToLastPosition() {
        if (recyclerView != null) {
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPositionWithOffset(mListener.getLastCredentialListPosition(), 0);
                    }
                }
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Credential item);

        void setLastCredentialListPosition(int pos);

        int getLastCredentialListPosition();
    }
}
