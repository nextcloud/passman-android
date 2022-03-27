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
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.List;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.adapters.CustomFieldEditAdapter;
import es.wolfi.passman.API.CustomField;

public class CustomFieldFileDeleteResponseHandler extends AsyncHttpResponseHandler {

    private final ProgressDialog progress;
    private final CustomFieldEditAdapter.ViewHolder holder;
    private final List<CustomField> mValues;
    private final CustomFieldEditAdapter customFieldEditAdapter;

    public CustomFieldFileDeleteResponseHandler(ProgressDialog progress, CustomFieldEditAdapter.ViewHolder holder, List<CustomField> mValues, CustomFieldEditAdapter customFieldEditAdapter) {
        super();

        this.progress = progress;
        this.holder = holder;
        this.mValues = mValues;
        this.customFieldEditAdapter = customFieldEditAdapter;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        if (statusCode == 200) {
            mValues.remove(holder.mItem);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    customFieldEditAdapter.notifyDataSetChanged();
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
                Toast.makeText(progress.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
            }
        });
        progress.dismiss();
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
