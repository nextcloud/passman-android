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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.util.List;

import es.wolfi.app.passman.CopyTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.fragments.CredentialDisplayFragment;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;
import es.wolfi.utils.FileUtils;

/**
 * {@link RecyclerView.Adapter} that can display a {@link File} and makes a call to the
 * specified {@link CredentialDisplayFragment.OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class CustomFieldViewAdapter extends RecyclerView.Adapter<CustomFieldViewAdapter.ViewHolder> {

    private final Credential credential;
    private final List<CustomField> mValues;
    private final CredentialDisplayFragment.OnListFragmentInteractionListener customFieldListListener;

    public CustomFieldViewAdapter(Credential credential, CredentialDisplayFragment.OnListFragmentInteractionListener listener) {
        this.credential = credential;
        mValues = credential.getCustomFieldsList();
        customFieldListListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_credential_custom_field_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        CustomField customField = mValues.get(position);
        holder.mLabelView.setText(customField.getLabel());

        if (customField.getFieldType().equals("password")) {
            holder.mValueView.setModePassword();
        }

        if (customField.getFieldType().equals("file")) {
            holder.mValueView.setVisibility(View.INVISIBLE);
            holder.mFileValueView.setVisibility(View.VISIBLE);

            try {
                File file = new File(customField.getJvalue(), credential);
                String filenameToPrint = String.format("%s (%s)", file.getFilename(), FileUtils.humanReadableByteCount((Double.valueOf(file.getSize())).longValue(), true));
                holder.mFileValueView.setText(filenameToPrint);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != customFieldListListener && holder.mItem.getFieldType().equals("file")) {
                            // Notify the active callbacks interface (the activity, if the
                            // fragment is attached to one) that an item has been selected.
                            try {
                                File file = new File(holder.mItem.getJvalue(), credential);
                                customFieldListListener.onListFragmentInteraction(file);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            holder.mValueView.setText(customField.getValue());
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mLabelView;
        public final CopyTextItem mValueView;
        public final TextView mFileValueView;
        public CustomField mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mLabelView = (TextView) view.findViewById(R.id.customFieldLabel);
            mValueView = (CopyTextItem) view.findViewById(R.id.customFieldValue);
            mFileValueView = (TextView) view.findViewById(R.id.customFieldFileValue);
        }
    }
}
