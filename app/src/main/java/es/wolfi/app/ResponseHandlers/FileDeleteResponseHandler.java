/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
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

package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.List;

import es.wolfi.app.passman.adapters.FileEditAdapter;
import es.wolfi.app.passman.R;
import es.wolfi.passman.API.File;

public class FileDeleteResponseHandler extends AsyncHttpResponseHandler {

    private final ProgressDialog progress;
    private final FileEditAdapter.ViewHolder holder;
    private final List<File> mValues;
    private final View view;

    public FileDeleteResponseHandler(ProgressDialog progress, FileEditAdapter.ViewHolder holder, List<File> mValues, View view) {
        super();

        this.progress = progress;
        this.holder = holder;
        this.mValues = mValues;
        this.view = view;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        if (statusCode == 200) {
            mValues.remove(holder.mItem);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    holder.mContentView.setTextColor(view.getResources().getColor(R.color.disabled));
                    holder.deleteButton.setVisibility(View.INVISIBLE);
                }
            });
        }
        progress.dismiss();
    }

    @Override
    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
        error.printStackTrace();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
            }
        });
        progress.dismiss();
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
