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
package es.wolfi.app.passman;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;

import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.FilterListAsyncTask;
import es.wolfi.utils.GeneralUtils;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListCredentialFragmentInteractionListener}
 * interface.
 */
public class CredentialItemFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";

    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListCredentialFragmentInteractionListener mListener;
    private AsyncTask filterTask = null;
    private FloatingActionButton fab = null;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        GeneralUtils.debug("Creating View");

        View view = inflater.inflate(R.layout.fragment_credential_item_list, container, false);

        View fabView = view.findViewById(R.id.addcredfab);

        if (fabView instanceof FloatingActionButton) {
            GeneralUtils.debug("Setting Listener View");
            fab = (FloatingActionButton)fabView;
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onActionCreateClick();
                }

            });
        }

        // Set the adapter
        View credentialView = view.findViewById(R.id.list);

        if (credentialView instanceof RecyclerView) {
            Context context = credentialView.getContext();
            final RecyclerView recyclerView = (RecyclerView) credentialView;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
            if (v != null) {
                final EditText searchInput = (EditText) view.findViewById(R.id.search_input);
                searchInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String searchText = searchInput.getText().toString().toLowerCase();
                        if (filterTask != null) {
                            filterTask.cancel(true);
                        }
                        filterTask = new FilterListAsyncTask(searchText, recyclerView, mListener);
                        ArrayList<Credential> input[] = new ArrayList[]{v.getCredentials()};
                        filterTask.execute((Object[]) input);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                recyclerView.setAdapter(new CredentialViewAdapter(v.getCredentials(), mListener));
            }
        }
        if (mListener != null) {
            mListener.onListFragmentCreatedView();
        }
        GeneralUtils.debug("Returning View");
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListCredentialFragmentInteractionListener) {
            mListener = (OnListCredentialFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListCredentialFragmentInteractionListener");
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
    public interface OnListCredentialFragmentInteractionListener {
        // TODO: Update argument type and name
        void onCredentialClick(Credential item);
        boolean onCredentialLongClick(Credential item);
        void onActionCreateClick();
        void onListFragmentCreatedView();
    }
}
