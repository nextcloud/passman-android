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

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.List;

import es.wolfi.app.passman.VaultFragment.OnListFragmentInteractionListener;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.ColorUtils;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Vault} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class VaultViewAdapter extends RecyclerView.Adapter<VaultViewAdapter.ViewHolder> {
    private static final String TAG = VaultViewAdapter.class.getSimpleName();

    private final List<Vault> mValues;
    private final OnListFragmentInteractionListener mListener;

    public VaultViewAdapter(List<Vault> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_vault, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.name.setText(mValues.get(position).name);

        DateFormat f = DateFormat.getDateInstance();
        holder.created.setText(f.format(holder.mItem.getCreatedTime()));
        holder.last_access.setText(f.format(holder.mItem.getLastAccessTime()));

        try {
            holder.name.setTextColor(ColorUtils.calculateColor(mValues.get(position).name));
        } catch (Exception e) {
            Log.w(TAG, "Error calculating vault item color.");
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView name;
        public final TextView created;
        public final TextView last_access;
        public Vault mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            name = (TextView) view.findViewById(R.id.vault_name);
            created = (TextView) view.findViewById(R.id.vault_created);
            last_access = (TextView) view.findViewById(R.id.vault_last_access);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.name + "'";
        }
    }
}
