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

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import es.wolfi.app.ResponseHandlers.CustomFieldFileDeleteResponseHandler;
import es.wolfi.app.passman.EditPasswordTextItem;
import es.wolfi.app.passman.R;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;
import es.wolfi.utils.FileUtils;
import es.wolfi.utils.ProgressUtils;

/**
 * {@link RecyclerView.Adapter} that can display a {@link File}.
 * TODO: Replace the implementation with code for your data type.
 */
public class CustomFieldEditAdapter extends RecyclerView.Adapter<CustomFieldEditAdapter.ViewHolder> {

    private final Credential credential;
    private final List<CustomField> mValues;

    public CustomFieldEditAdapter(Credential cred) {
        credential = cred;
        mValues = credential.getCustomFieldsList();
    }

    public String getCustomFieldsString() {
        JSONArray customFields = new JSONArray();
        for (CustomField cf : mValues) {
            try {
                customFields.put(cf.getAsJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return customFields.toString().replaceAll("\\\\/", "/");
    }

    public boolean addCustomField(CustomField customField) {
        return mValues.add(customField);
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
        holder.mLabelEdit.setText(customField.getLabel());

        if (customField.getFieldType().equals("file")) {
            holder.mValueEdit.setEnabled(false);
            try {
                File file = new File(customField.getJvalue());
                String filenameToPrint = String.format("%s (%s)", file.getFilename(), FileUtils.humanReadableByteCount((Double.valueOf(file.getSize())).longValue(), true));
                holder.mValueEdit.setText(filenameToPrint);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            if (holder.mItem.getFieldType().equals("password")) {
                Log.d("customfield", "got password field");
                holder.mValueEdit.setVisibility(View.GONE);
                holder.mValuePasswordEdit.setVisibility(View.VISIBLE);
                holder.mValuePasswordEdit.setText(customField.getValue());
                holder.mValueEdit.setEnabled(false);
            } else {
                Log.d("customfield", "got default field");
                holder.mValueEdit.setText(customField.getValue());
                holder.mValuePasswordEdit.setEnabled(false);
                holder.mValuePasswordEdit.setVisibility(View.GONE);
            }
        }

        holder.mLabelEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            private Timer timer = new Timer();
            private final long DELAY = 100; // milliseconds

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                // you will probably need to use runOnUiThread(Runnable action) for some specific actions (e.g. manipulating views)
                                int itemIndex = mValues.indexOf(holder.mItem);
                                holder.mItem.setLabel(s.toString());
                                mValues.set(itemIndex, holder.mItem);
                            }
                        },
                        DELAY
                );
            }
        });

        holder.mValueEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            private Timer timer = new Timer();
            private final long DELAY = 100; // milliseconds

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                // you will probably need to use runOnUiThread(Runnable action) for some specific actions (e.g. manipulating views)
                                int itemIndex = mValues.indexOf(holder.mItem);
                                holder.mItem.setValue(s.toString());
                                mValues.set(itemIndex, holder.mItem);
                            }
                        },
                        DELAY
                );
            }
        });

        holder.mValuePasswordEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            private Timer timer = new Timer();
            private final long DELAY = 100; // milliseconds

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                // you will probably need to use runOnUiThread(Runnable action) for some specific actions (e.g. manipulating views)
                                int itemIndex = mValues.indexOf(holder.mItem);
                                holder.mItem.setValue(s.toString());
                                mValues.set(itemIndex, holder.mItem);
                            }
                        },
                        DELAY
                );
            }
        });

        CustomFieldEditAdapter self = this;
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = holder.mView.getContext();

                if (holder.mItem.getFieldType().equals("file")) {
                    final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
                    final AsyncHttpResponseHandler responseHandler = new CustomFieldFileDeleteResponseHandler(progress, holder, mValues, self);

                    try {
                        File file = new File(holder.mItem.getJvalue());
                        credential.sendFileDeleteRequest(context, file.getFileId(), responseHandler);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        progress.dismiss();
                    }
                } else {
                    mValues.remove(holder.mItem);
                    notifyDataSetChanged();
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
        public final TextView mLabelView;
        public final EditText mLabelEdit;
        public final EditText mValueEdit;
        public final EditPasswordTextItem mValuePasswordEdit;
        public final AppCompatImageButton deleteButton;
        public final LinearLayout displayCustomFieldLayout;
        public final LinearLayout editCustomFieldLayout;
        public final RelativeLayout customFieldsRelativeLayout;
        public CustomField mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mLabelView = (TextView) view.findViewById(R.id.customFieldLabel);
            mLabelEdit = (EditText) view.findViewById(R.id.customFieldEditLabel);
            mValueEdit = (EditText) view.findViewById(R.id.customFieldEditValue);
            mValuePasswordEdit = (EditPasswordTextItem) view.findViewById(R.id.customFieldEditValuePassword);
            deleteButton = (AppCompatImageButton) view.findViewById(R.id.deleteCustomFieldButton);
            displayCustomFieldLayout = (LinearLayout) view.findViewById(R.id.displayCustomFieldLayout);
            editCustomFieldLayout = (LinearLayout) view.findViewById(R.id.editCustomFieldLayout);
            customFieldsRelativeLayout = (RelativeLayout) view.findViewById(R.id.customFieldsRelativeLayout);

            displayCustomFieldLayout.setVisibility(View.INVISIBLE);
            editCustomFieldLayout.setVisibility(View.VISIBLE);
            mValueEdit.setVisibility(View.VISIBLE);
            mLabelView.setVisibility(View.INVISIBLE);
            mLabelEdit.setVisibility(View.VISIBLE);
            mValuePasswordEdit.setHint(mView.getContext().getString(R.string.password));

            WindowManager vm = (WindowManager) mView.getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final Rect bounds = vm.getCurrentWindowMetrics().getBounds();
                mValueEdit.setMaxWidth(bounds.width() - deleteButton.getWidth() - 200);
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                vm.getDefaultDisplay().getMetrics(displayMetrics);
                mValueEdit.setMaxWidth(displayMetrics.widthPixels - deleteButton.getWidth() - 200);
            }
        }
    }
}
