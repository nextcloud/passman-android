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
package es.wolfi.app.passman.adapters;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.wolfi.app.ResponseHandlers.VaultSaveResponseHandler;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.fragments.CredentialItemFragment;
import es.wolfi.passman.API.Credential;
import es.wolfi.utils.IconUtils;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Credential} and makes a call to the
 * specified {@link CredentialItemFragment.OnListFragmentInteractionListener}.
 */
public class CredentialViewAdapter extends RecyclerView.Adapter<CredentialViewAdapter.ViewHolder> {

    private final List<Credential> mValues;
    private final CredentialItemFragment.OnListFragmentInteractionListener mListener;
    private final SharedPreferences settings;

    public CredentialViewAdapter(List<Credential> items, CredentialItemFragment.OnListFragmentInteractionListener listener, SharedPreferences settings) {
        mValues = items;
        mListener = listener;
        this.settings = settings;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_credential_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(holder.mItem.getLabel());

        // the automatically created test credential must always be there to ensure vault consistency
        if (holder.mItem.getLabel().startsWith(VaultSaveResponseHandler.labelPrefixForFirstVaultConsistencyCredential)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        }

        if (holder.mItem != null && holder.mItem.getCompromised() != null && holder.mItem.getCompromised().equals("true")) {
            holder.contentLayout.setBackgroundColor(holder.mView.getResources().getColor(R.color.compromised));
        } else {
            holder.contentLayout.setBackgroundColor(0);
        }

        if (holder.mItem != null && settings.getBoolean(SettingValues.ENABLE_CREDENTIAL_LIST_ICONS.toString(), true)) {
            // overwrite real credential icon for every shared credential
            if (holder.mItem.getCredentialACL() != null) {
                // shared with me
                holder.contentImage.setImageResource(R.drawable.ic_baseline_share_24);
            } else if (holder.mItem.isASharedCredential()) {
                // shared with other (use as alternative to fa-share-alt-square)
                holder.contentImage.setImageResource(R.drawable.ic_baseline_share_24);
                holder.contentImage.setBackgroundColor(holder.mView.getResources().getColor(R.color.cardview_dark_background));
                holder.contentImage.setColorFilter(holder.mView.getResources().getColor(R.color.white));
            } else {
                IconUtils.loadIconToImageView(holder.mItem.getFavicon(), holder.contentImage);
            }
        } else {
            holder.contentLayout.removeView(holder.contentImage);
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.setLastCredentialListPosition(holder.getBindingAdapterPosition());

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
        public final TextView mContentView;
        public final LinearLayout contentLayout;
        public final ImageView contentImage;
        public Credential mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.content);
            contentLayout = (LinearLayout) view.findViewById(R.id.contentLayout);
            contentImage = (ImageView) view.findViewById(R.id.contentImage);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
