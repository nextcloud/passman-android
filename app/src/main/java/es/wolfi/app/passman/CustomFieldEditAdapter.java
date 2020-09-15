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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
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

import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;

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

    @SuppressLint("DefaultLocale")
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
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
                String filenameToPrint = String.format("%s (%s)", file.getFilename(), humanReadableByteCount((Double.valueOf(file.getSize())).longValue(), true));
                holder.mValueEdit.setText(filenameToPrint);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            holder.mValueEdit.setText(customField.getValue());
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

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = holder.mView.getContext();

                if (holder.mItem.getFieldType().equals("file")) {
                    final ProgressDialog progress = new ProgressDialog(context);
                    progress.setTitle(context.getString(R.string.loading));
                    progress.setMessage(context.getString(R.string.wait_while_loading));
                    progress.setCancelable(false);
                    progress.show();

                    AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                            if (statusCode == 200) {
                                mValues.remove(holder.mItem);
                                notifyDataSetChanged();
                            }
                            progress.dismiss();
                        }

                        @Override
                        public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                            error.printStackTrace();
                            progress.dismiss();
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    };

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
            deleteButton = (AppCompatImageButton) view.findViewById(R.id.deleteCustomFieldButton);
            displayCustomFieldLayout = (LinearLayout) view.findViewById(R.id.displayCustomFieldLayout);
            editCustomFieldLayout = (LinearLayout) view.findViewById(R.id.editCustomFieldLayout);
            customFieldsRelativeLayout = (RelativeLayout) view.findViewById(R.id.customFieldsRelativeLayout);

            displayCustomFieldLayout.setVisibility(View.INVISIBLE);
            editCustomFieldLayout.setVisibility(View.VISIBLE);
            mValueEdit.setVisibility(View.VISIBLE);
            mLabelView.setVisibility(View.INVISIBLE);
            mLabelEdit.setVisibility(View.VISIBLE);

            WindowManager vm = (WindowManager) mView.getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            final Rect bounds = vm.getCurrentWindowMetrics().getBounds();
            mValueEdit.setMaxWidth(bounds.width() - deleteButton.getWidth() - 200);
        }
    }
}
