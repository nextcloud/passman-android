package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;
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

            holder.mContentView.setTextColor(view.getResources().getColor(R.color.disabled));
            holder.deleteButton.setVisibility(View.INVISIBLE);
        }
        progress.dismiss();
    }

    @Override
    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
        error.printStackTrace();
        Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
        progress.dismiss();
    }

    @Override
    public void onRetry(int retryNo) {
        // called when request is retried
    }
}
