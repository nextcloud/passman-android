package es.wolfi.app.ResponseHandlers;

import android.app.ProgressDialog;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import es.wolfi.app.passman.CustomFieldEditAdapter;
import es.wolfi.app.passman.FileEditAdapter;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.File;
import es.wolfi.utils.FileUtils;

public class CredentialAddFileResponseHandler extends AsyncHttpResponseHandler {

    private final ProgressDialog progress;
    private final String fileName;
    private final int requestCode;
    private final FileEditAdapter fed;
    private final CustomFieldEditAdapter cfed;

    public CredentialAddFileResponseHandler(ProgressDialog progress, String fileName, int requestCode, FileEditAdapter fed, CustomFieldEditAdapter cfed) {
        super();

        this.progress = progress;
        this.fileName = fileName;
        this.requestCode = requestCode;
        this.fed = fed;
        this.cfed = cfed;
    }

    @Override
    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
        String result = new String(responseBody);
        if (statusCode == 200 && !result.equals("")) {
            try {
                JSONObject fileObject = new JSONObject(result);
                if (fileObject.has("message") && fileObject.getString("message").equals("Current user is not logged in")) {
                    return;
                }
                if (fileObject.has("file_id") && fileObject.has("filename") && fileObject.has("guid")
                        && fileObject.has("size") && fileObject.has("created") && fileObject.has("mimetype")) {
                    fileObject.put("filename", fileName);
                    File file = new File(fileObject);

                    if (requestCode == FileUtils.activityRequestFileCodes.get("credentialAddFile")) {
                        fed.addFile(file);
                        fed.notifyDataSetChanged();
                    }
                    if (requestCode == FileUtils.activityRequestFileCodes.get("credentialAddCustomFieldFile")) {
                        CustomField cf = new CustomField();
                        cf.setLabel("newLabel" + cfed.getItemCount() + 1);
                        cf.setSecret(false);
                        cf.setFieldType("file");
                        cf.setJValue(file.getAsJSONObject());

                        cfed.addCustomField(cf);
                        cfed.notifyDataSetChanged();
                    }
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
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
}
