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

package es.wolfi.app.passman.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.koushikdutta.async.future.FutureCallback;
import com.vdurmont.semver4j.Semver;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.fragments.VaultDeleteFragment;
import es.wolfi.app.passman.fragments.VaultEditFragment;
import es.wolfi.app.passman.fragments.VaultFragment.OnListFragmentInteractionListener;
import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.ColorUtils;
import es.wolfi.utils.ProgressUtils;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Vault} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class VaultViewAdapter extends RecyclerView.Adapter<VaultViewAdapter.ViewHolder> {
    private static final String TAG = VaultViewAdapter.class.getSimpleName();

    private final List<Vault> mValues;
    private final OnListFragmentInteractionListener mListener;
    private final FragmentManager fragmentManager;

    public VaultViewAdapter(List<Vault> items, OnListFragmentInteractionListener listener, FragmentManager fragmentManager) {
        mValues = items;
        mListener = listener;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_vault, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.name.setText(mValues.get(position).name);

        DateFormat f = DateFormat.getDateInstance();
        holder.created.setText(f.format(holder.mItem.getCreatedTime()));
        holder.last_access.setText(f.format(holder.mItem.getLastAccessTime()));

        try {
            holder.name.setTextColor(ColorUtils.calculateColor(mValues.get(position).name));
        } catch (Exception e) {
            Log.w(TAG, "Error calculating vault item color.");
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });

        holder.vault_edit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
                Vault.getVault(context, holder.mItem.guid, new FutureCallback<Vault>() {
                    @Override
                    public void onCompleted(Exception e, Vault result) {
                        progress.dismiss();
                        if (e != null) {
                            Log.e(TAG, "Unknown network error", e);

                            Toast.makeText(context, context.getString(R.string.net_error), Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Update the local vault record
                        ((HashMap<String, Vault>) SingleTon.getTon().getExtra(SettingValues.VAULTS.toString())).put(result.guid, result);

                        fragmentManager
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                                .replace(R.id.content_password_list, VaultEditFragment.newInstance(holder.mItem.guid), "vault")
                                .addToBackStack(null)
                                .commit();
                    }
                });
            }
        });

        Core.getAPIVersion(holder.mView.getContext(), new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (result != null && new Semver(result).isGreaterThanOrEqualTo("2.3.1336") || true) {
                    holder.vault_delete_button.setColorFilter(holder.mView.getResources().getColor(R.color.danger));
                    holder.vault_delete_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Context context = holder.mView.getContext();
                            final ProgressDialog progress = ProgressUtils.showLoadingSequence(context);
                            progress.show();

                            Vault.getVault(context, holder.mItem.guid, new FutureCallback<Vault>() {
                                @Override
                                public void onCompleted(Exception e, Vault result) {
                                    progress.dismiss();
                                    if (e != null) {
                                        Log.e(TAG, "Unknown network error", e);

                                        Toast.makeText(context, context.getString(R.string.net_error), Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    SingleTon ton = SingleTon.getTon();

                                    // Update the vault record to avoid future loads
                                    ((HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString())).put(result.guid, result);

                                    ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), result);

                                    fragmentManager
                                            .beginTransaction()
                                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                                            .replace(R.id.content_password_list, VaultDeleteFragment.newInstance(holder.mItem.guid), "vault")
                                            .addToBackStack(null)
                                            .commit();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.vault_name)
        TextView name;
        @BindView(R.id.vault_created)
        TextView created;
        @BindView(R.id.vault_last_access)
        TextView last_access;
        @BindView(R.id.vault_edit_button)
        ImageView vault_edit_button;
        @BindView(R.id.vault_delete_button)
        ImageView vault_delete_button;

        public final View mView;
        public Vault mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            ButterKnife.bind(this, view);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.name + "'";
        }
    }
}
