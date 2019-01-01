package es.wolfi.app.passman.autofill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import android.widget.Toast;

import es.wolfi.app.passman.R;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CredentialAutofillService extends AutofillService {

    private static final String TAG = "CredentialAutofillSvc";

    /**
     * Number of datasets sent on each request - we're simple, that value is hardcoded in our DNA!
     */
    private static final int NUMBER_DATASETS = 4;

    /**
     * Called by the Android system do decide if a screen can be autofilled by the service.
     *
     * <p>Service must call one of the {@link FillCallback} methods (like
     * {@link FillCallback#onSuccess(FillResponse)}
     * or {@link FillCallback#onFailure(CharSequence)})
     * to notify the result of the request.
     *
     * @param request            the {@link FillRequest request} to handle.
     *                           See {@link FillResponse} for examples of multiple-sections requests.
     * @param cancellationSignal signal for observing cancellation requests. The system will use
     *                           this to notify you that the fill result is no longer needed and you should stop
     *                           handling this fill request in order to save resources.
     * @param callback           object used to notify the result of the request.
     */
    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback) {
        Log.d(TAG, "onFillRequest()");

        // Find autofillable fields
        AssistStructure structure = getLatestAssistStructure(request);
        Map<String, AutofillId> fields = getAutofillableFields(structure);
        Log.d(TAG, "autofillable fields:" + fields);

        if (fields.isEmpty()) {
            toast("No autofill hints found");
            callback.onSuccess(null);
            return;
        }

        // Create the base response
        FillResponse.Builder response = new FillResponse.Builder();

        // 1.Add the dynamic datasets
        String packageName = getApplicationContext().getPackageName();
        for (int i = 1; i <= NUMBER_DATASETS; i++) {
            Dataset.Builder dataset = new Dataset.Builder();
            for (Entry<String, AutofillId> field : fields.entrySet()) {
                String hint = field.getKey();
                AutofillId id = field.getValue();
                String value = i + "-" + hint;
                // We're simple - our dataset values are hardcoded as "N-hint" (for example,
                // "1-username", "2-username") and they're displayed as such, except if they're a
                // password
                String displayValue = hint.contains("password") ? "password for #" + i : value;
                RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
                dataset.setValue(id, AutofillValue.forText(value), presentation);
            }
            response.addDataset(dataset.build());
        }

        // 2.Add save info
        Collection<AutofillId> ids = fields.values();
        AutofillId[] requiredIds = new AutofillId[ids.size()];
        ids.toArray(requiredIds);
        response.setSaveInfo(
                // We're simple, so we're generic
                new SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_GENERIC, requiredIds).build());

        // 3.Profit!
        callback.onSuccess(response.build());
    }

    /**
     * Called when the user requests the service to save the contents of a screen.
     *
     * <p>Service must call one of the {@link SaveCallback} methods (like
     * {@link SaveCallback#onSuccess()} or {@link SaveCallback#onFailure(CharSequence)})
     * to notify the Android System of the result of the request.
     *
     * <p>If the service could not handle the request right away&mdash;for example, because it must
     * launch an activity asking the user to authenticate first or because the network is
     * down&mdash;the service could keep the {@link SaveRequest request} and reuse it later,
     * but the service must call {@link SaveCallback#onSuccess()} right away.
     *
     * <p><b>Note:</b> To retrieve the actual value of fields input by the user, the service
     * should call
     * {@link ViewNode#getAutofillValue()}; if it calls
     * {@link ViewNode#getText()} or other methods, there is no
     * guarantee such method will return the most recent value of the field.
     *
     * @param request  the {@link SaveRequest request} to handle.
     *                 See {@link FillResponse} for examples of multiple-sections requests.
     * @param callback object used to notify the result of the request.
     */
    @Override
    public void onSaveRequest(SaveRequest request,  SaveCallback callback) {
        Log.d(TAG, "onSaveRequest()");
        toast("Save not supported");
        callback.onSuccess();
    }

    /**
     * Parses the {@link AssistStructure} representing the activity being autofilled, and returns a
     * map of autofillable fields (represented by their autofill ids) mapped by the hint associate
     * with them.
     *
     */
    @NonNull
    private Map<String, AutofillId> getAutofillableFields(@NonNull AssistStructure structure) {
        Map<String, AutofillId> fields = new ArrayMap<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node);
        }
        return fields;
    }

    /**
     * Adds any autofillable view from the {@link ViewNode} and its descendants to the map.
     */
    private void addAutofillableFields(@NonNull Map<String, AutofillId> fields,
                                       @NonNull ViewNode node) {
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            // We're simple, we only care about the first hint
            String hint = hints[0].toLowerCase();

            if (hint != null) {
                AutofillId id = node.getAutofillId();
                if (!fields.containsKey(hint)) {
                    Log.v(TAG, "Setting hint '" + hint + "' on " + id);
                    fields.put(hint, id);
                } else {
                    Log.v(TAG, "Ignoring hint '" + hint + "' on " + id
                            + " because it was already set");
                }
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i));
        }
    }

    /**
     * Helper method to get the {@link AssistStructure} associated with the latest request
     * in an autofill context.
     */
    @NonNull
    static AssistStructure getLatestAssistStructure(@NonNull FillRequest request) {
        List<FillContext> fillContexts = request.getFillContexts();
        return fillContexts.get(fillContexts.size() - 1).getStructure();
    }

    /**
     * Helper method to create a dataset presentation with the given text.
     */
    @NonNull
    static RemoteViews newDatasetPresentation(@NonNull String packageName,
                                              @NonNull CharSequence text) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.autofill_list_item);
        presentation.setTextViewText(R.id.autofilltext, text);
        return presentation;
    }

    /**
     * Displays a toast with the given message.
     */
    private void toast(@NonNull CharSequence message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
