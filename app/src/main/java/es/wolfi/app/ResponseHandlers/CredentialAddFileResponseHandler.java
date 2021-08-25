package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;
import android.view.View;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import es.wolfi.app.passman.CustomFieldEditAdapter;
import es.wolfi.app.passman.FileEditAdapter;
import es.wolfi.app.passman.R;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;
import es.wolfi.utils.FileUtils;

public class CredentialAddFileResponseHandler extends AsyncHttpResponseHandler {

    private final ProgressDialog progress;
    private final View view;
    private final String fileName;
    private final int requestCode;
    private final FileEditAdapter fed;
    private final CustomFieldEditAdapter cfed;

    public CredentialAddFileResponseHandler(ProgressDialog progress, View view, String fileName, int requestCode, FileEditAdapter fed, CustomFieldEditAdapter cfed) {
        super();

        this.progress = progress;
        this.view = view;
        this.fileName = fileName;
        this.requestCode = requestCode;
        this.fed = fed;
        this.cfed = cfed;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        String result = new String(responseBody);
        if (statusCode == 200) {
            try {
                JSONObject fileObject = new JSONObject(result);
                if (fileObject.has("message") && fileObject.getString("message").equals("Current user is not logged in")) {
                    throw new Exception(fileObject.getString("message"));
                }

                fileObject.put("filename", fileName);
                File file = new File(fileObject);

                if (requestCode == FileUtils.activityRequestFileCode.credentialAddFile.ordinal()) {
                    fed.addFile(file);
                    fed.notifyDataSetChanged();
                }
                if (requestCode == FileUtils.activityRequestFileCode.credentialAddCustomFieldFile.ordinal()) {
                    CustomField cf = new CustomField();
                    cf.setLabel("newLabel" + cfed.getItemCount() + 1);
                    cf.setSecret(false);
                    cf.setFieldType("file");
                    cf.setJValue(file.getAsJSONObject());

                    cfed.addCustomField(cf);
                    cfed.notifyDataSetChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(view.getContext(), e.getMessage() != null ? e.getMessage() : view.getContext().getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(view.getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
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
