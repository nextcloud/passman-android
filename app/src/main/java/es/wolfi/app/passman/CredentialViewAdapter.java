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
package es.wolfi.app.passman;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.pixplicity.sharp.Sharp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.List;

import es.wolfi.passman.API.Credential;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Credential} and makes a call to the
 * specified {@link CredentialItemFragment.OnListFragmentInteractionListener}.
 */
public class CredentialViewAdapter extends RecyclerView.Adapter<CredentialViewAdapter.ViewHolder> {

    private final List<Credential> mValues;
    private final CredentialItemFragment.OnListFragmentInteractionListener mListener;
    private ViewGroup vg;

    public CredentialViewAdapter(List<Credential> items, CredentialItemFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        vg = parent;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_credential_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(holder.mItem.getLabel());

        if (holder.mItem != null && holder.mItem.getCompromised() != null && holder.mItem.getCompromised().equals("true")) {
            holder.contentLayout.setBackgroundColor(holder.mView.getResources().getColor(R.color.compromised));
        }

        if (holder.mItem != null) {
            String favicon = holder.mItem.getFavicon();
            if (favicon != null && !favicon.equals("") && !favicon.equals("null")) {
                try {
                    JSONObject icon = new JSONObject(favicon);
                    byte[] byteImageData = Base64.decode(icon.getString("content"), Base64.DEFAULT);
                    Bitmap bitmapImageData = BitmapFactory.decodeByteArray(byteImageData, 0, byteImageData.length);
                    if (bitmapImageData == null) {
                        Sharp.loadInputStream(new ByteArrayInputStream(byteImageData)).into(holder.contentImage);
                    } else {
                        holder.contentImage.setImageBitmap(bitmapImageData);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                holder.contentImage.setImageResource(R.drawable.ic_baseline_lock_24);
            }
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
