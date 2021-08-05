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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.app.ResponseHandlers.CredentialAddFileResponseHandler;
import es.wolfi.app.ResponseHandlers.CredentialSaveResponseHandler;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.app.passman.adapters.CustomFieldEditAdapter;
import es.wolfi.app.passman.adapters.FileEditAdapter;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.CustomField;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.FileUtils;
import es.wolfi.utils.ProgressUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialAdd#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialAdd extends Fragment implements View.OnClickListener {
    public static String CREDENTIAL = "credential";

    @BindView(R.id.add_credential_label_header)
    TextView label_header;
    @BindView(R.id.add_credential_label)
    EditText label;
    @BindView(R.id.add_credential_user)
    EditText user;
    @BindView(R.id.add_credential_password)
    EditText password;
    @BindView(R.id.add_credential_email)
    EditText email;
    @BindView(R.id.add_credential_url)
    EditText url;
    @BindView(R.id.add_credential_description)
    EditText description;
    @BindView(R.id.filesList)
    RecyclerView filesList;
    @BindView(R.id.customFieldsList)
    RecyclerView customFieldsList;
    @BindView(R.id.customFieldType)
    Spinner customFieldType;

    private OnCredentialFragmentInteraction mListener;
    private Credential credential;
    private FileEditAdapter fed;
    private CustomFieldEditAdapter cfed;
    private RecyclerView filesListRecyclerView;
    private RecyclerView customFieldsListRecyclerView;
    private AtomicBoolean alreadySaving = new AtomicBoolean(false);

    public CredentialAdd() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment CredentialAdd.
     */
    public static CredentialAdd newInstance() {
        return new CredentialAdd();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_credential_add, container, false);

        FloatingActionButton saveCredentialButton = (FloatingActionButton) view.findViewById(R.id.SaveCredentialButton);
        saveCredentialButton.setOnClickListener(this);
        saveCredentialButton.setVisibility(View.VISIBLE);

        Button addFileButton = (Button) view.findViewById(R.id.AddFileButton);
        addFileButton.setOnClickListener(this.getAddFileButtonListener());
        addFileButton.setVisibility(View.VISIBLE);

        Button addCustomFieldButton = (Button) view.findViewById(R.id.AddCustomFieldButton);
        addCustomFieldButton.setOnClickListener(this.getAddCustomFieldButtonListener());
        addCustomFieldButton.setVisibility(View.VISIBLE);

        fed = new FileEditAdapter(credential);
        cfed = new CustomFieldEditAdapter(credential);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCredentialFragmentInteraction) {
            mListener = (OnCredentialFragmentInteraction) context;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        filesListRecyclerView = (RecyclerView) view.findViewById(R.id.filesList);
        filesListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        filesListRecyclerView.setAdapter(fed);

        customFieldsListRecyclerView = (RecyclerView) view.findViewById(R.id.customFieldsList);
        customFieldsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        customFieldsListRecyclerView.setAdapter(cfed);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        this.credential = new Credential();
        this.credential.setVault(v);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void addSelectedFile(String encodedFile, String fileName, String mimeType, int fileSize, int requestCode) {
        Context context = getContext();
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
        final AsyncHttpResponseHandler responseHandler = new CredentialAddFileResponseHandler(progress, getView(), fileName, requestCode, fed, cfed);

        // Start encryption a little later so that the main thread does not get stuck in the file selection dialog and it can close.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                credential.uploadFile(context, encodedFile, fileName, mimeType, fileSize, responseHandler, progress);
            }
        }, 100);
    }

    public View.OnClickListener getAddFileButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((PasswordListActivity) requireActivity()).selectFileToAdd(FileUtils.activityRequestFileCode.credentialAddFile.ordinal());
            }
        };
    }

    public View.OnClickListener getAddCustomFieldButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (customFieldType.getSelectedItem().toString().equals("File")) {
                    ((PasswordListActivity) requireActivity()).selectFileToAdd(FileUtils.activityRequestFileCode.credentialAddCustomFieldFile.ordinal());
                } else {
                    CustomField cf = new CustomField();
                    cf.setLabel("newLabel" + (cfed.getItemCount() + 1));
                    cf.setSecret(customFieldType.getSelectedItem().toString().toLowerCase().equals("password"));
                    cf.setFieldType(customFieldType.getSelectedItem().toString().toLowerCase());
                    cf.setValue("");

                    cfed.addCustomField(cf);
                    cfed.notifyDataSetChanged();
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        if (alreadySaving.get()) {
            return;
        }

        if (label.getText().toString().equals("")) {
            label_header.setTextColor(getResources().getColor(R.color.danger));
            return;
        }

        this.credential.setLabel(label.getText().toString());
        this.credential.setUsername(user.getText().toString());
        this.credential.setPassword(password.getText().toString());
        this.credential.setEmail(email.getText().toString());
        this.credential.setUrl(url.getText().toString());
        this.credential.setDescription(description.getText().toString());
        this.credential.setOtp("{}");
        this.credential.setFiles(fed.getFilesString());
        this.credential.setCustomFields(cfed.getCustomFieldsString());
        this.credential.setTags("");
        this.credential.setFavicon("");
        this.credential.setCompromised(false);
        this.credential.setHidden(false);

        alreadySaving.set(true);

        Context context = getContext();
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
        final AsyncHttpResponseHandler responseHandler = new CredentialSaveResponseHandler(alreadySaving, false, progress, view, (PasswordListActivity) getActivity(), getFragmentManager());

        this.credential.save(context, responseHandler);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnCredentialFragmentInteraction {
        // TODO: Update argument type and name
        void onCredentialFragmentInteraction(Credential credential);

        void saveCredential(Credential credential, Context c, FutureCallback<String> cb);
    }
}
