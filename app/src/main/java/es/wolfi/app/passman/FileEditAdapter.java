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

import es.wolfi.app.ResponseHandlers.FileDeleteResponseHandler;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.File;
import es.wolfi.utils.FileUtils;
import es.wolfi.utils.ProgressUtils;

/**
 * {@link RecyclerView.Adapter} that can display a {@link File}.
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

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        String filenameToPrint = String.format("%s (%s)", mValues.get(position).getFilename(), FileUtils.humanReadableByteCount((Double.valueOf(mValues.get(position).getSize())).longValue(), true));
        holder.mContentView.setText(filenameToPrint);

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = holder.mView.getContext();
                final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
                final AsyncHttpResponseHandler responseHandler = new FileDeleteResponseHandler(progress, holder, mValues, view);

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
