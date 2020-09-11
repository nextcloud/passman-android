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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.File;

/**
 * {@link RecyclerView.Adapter} that can display a {@link File}.
 * TODO: Replace the implementation with code for your data type.
 */
public class FileEditAdapter extends RecyclerView.Adapter<FileEditAdapter.ViewHolder> {

    private final Credential credential;
    private final List<File> mValues;

    public FileEditAdapter(Credential cred) {
        credential = cred;
        mValues = cred.getFilesList();
    }

    public String getFilesString() {
        JSONArray files = new JSONArray();
        for (File f : mValues) {
            try {
                files.put(f.getAsJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return files.toString().replaceAll("\\\\/", "/");
    }

    public boolean addFile(File file) {
        return mValues.add(file);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_credential_file_item, parent, false);
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
        String filenameToPrint = String.format("%s (%s)", mValues.get(position).getFilename(), humanReadableByteCount((Double.valueOf(mValues.get(position).getSize())).longValue(), true));
        holder.mContentView.setText(filenameToPrint);

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = holder.mView.getContext();
                final ProgressDialog progress = new ProgressDialog(context);
                progress.setTitle(context.getString(R.string.loading));
                progress.setMessage(context.getString(R.string.wait_while_loading));
                progress.setCancelable(false);
                progress.show();

                AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                        String result = new String(responseBody);
                        if (statusCode == 200) {
                            mValues.remove(holder.mItem);
                            holder.mContentView.setTextColor(v.getResources().getColor(R.color.disabled));
                            holder.deleteButton.setVisibility(View.INVISIBLE);
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

                credential.sendFileDeleteRequest(context, holder.mItem.getFileId(), responseHandler);
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
        public final AppCompatImageButton deleteButton;
        public File mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.content);
            deleteButton = (AppCompatImageButton) view.findViewById(R.id.DeleteFileButton);
            deleteButton.setVisibility(View.VISIBLE);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
