/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package es.wolfi.app.passman;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.koushikdutta.ion.Ion;
import com.nextcloud.android.sso.AccountImporter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Core;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

public class LoginActivity extends AppCompatActivity
{

    public static final String ACTION_AUTH_RETURN
          = "es.wolfi.app.passman.AUTH_RETURN";

    public static final String REDIR_SCHEME       = "passman";
    public static final String REDIR_URI          = REDIR_SCHEME + "://login/oauth_callback";
    private static final int REQUEST_GET_ACCOUNTS = 1;
    private static final int REQ_CHOOSE_ACCOUNT   = 2;

    private AccountManager mAccountManager;

    @BindView(R.id.login_account_list)
    ListView mListView;

    SharedPreferences settings;
    SingleTon ton;

    private
    ItemClickListener mItemClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mAccountManager = AccountManager.get( this );
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ton = SingleTon.getTon();

        mItemClickListener = new ItemClickListener( this );

        mListView.setOnItemClickListener( mItemClickListener );

        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String action = intent.getAction();

//        if (action == null || !action.contentEquals( ACTION_AUTH_RETURN ))
//        {
//            Timber.d( "get host!" );
//            // show get host page!
//            return;
//        }

        getAccount();

        // handle login!

        Timber.d( "data: %s", intent.getDataString());

    }

    @Override
    protected
    void onActivityResult ( final int requestCode, final int resultCode, final Intent data )
    {
        switch ( requestCode )
        {
            case REQ_CHOOSE_ACCOUNT:
                if (data != null)
                {
                    Timber.d( "choose account: %s", data.toString() );
                }
                else {
                    Timber.w( "request failed?!" );
                }
                break;

            default:
               super.onActivityResult( requestCode, resultCode, data );
               break;
        }
    }

    private
    void getAccount()
    {
//        Intent chooseIntent = AccountPicker.newChooseAccountIntent( null, null, new String[]{"nextcloud"},true, null, "org.nextcloud", null, null );
//        startActivityForResult( chooseIntent, REQ_CHOOSE_ACCOUNT );

        List<Account> accountsImporter = AccountImporter.findAccounts( this );
        Timber.d( "accounts Importer: %d", accountsImporter.size() );

        //Intent chooseIntent = AccountManager.newChooseAccountIntent( null, null, new String[] { "nextcloud"} , false,null,"org.nextcloud", null, null);
        //startActivityForResult( chooseIntent, REQ_CHOOSE_ACCOUNT );

        //        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M )
//        {
//            Intent intent =
//                  AccountManager.newChooseAccountIntent( null, null,
//                                                         new String[] { "nextcloud" },
//                                                         false,
//                                                         null,
//                                                         "org.nextcloud",
//                                                         null,
//                                                         null );
//
//            startActivityForResult( intent, REQ_CHOOSE_ACCOUNT );
//        }

        AccountManager accountManager = AccountManager.get( this );
        Account[] accountsByTypeNextcloud = accountManager.getAccountsByType( "nextcloud" );
        Account[] accountsByTypeOrgNextcloud = accountManager.getAccountsByType( "org.nextcloud" );
        Account[] accountsByTypeNull = accountManager.getAccountsByType( null );
        Account[] accounts = accountManager.getAccounts( );
        Timber.d( "accounts Nextcloud: %d", accountsByTypeNextcloud.length );
        Timber.d( "accounts Org.Nextcloud: %d", accountsByTypeOrgNextcloud.length );
        Timber.d( "accounts null: %d", accountsByTypeNull.length );
        Timber.d( "accounts: %d", accounts.length );
        //accountManager.

        mItemClickListener.setAccounts( accountsByTypeNextcloud );

        mListView.setAdapter( new AccountListAdapter( this, R.layout.login_list_item, accountsByTypeNextcloud) );

        for ( Account account : accountsByTypeNextcloud )
        {
            Timber.d( "account: %s", account.toString() );
        }
    }

    //@OnClick(R.id.next)
    public void onNextClick() {
        Timber.e( "begin" );

        /*final String protocol = input_protocol.getSelectedItem().toString().toLowerCase();
        final String host = protocol + "://" + input_host.getText().toString();
        final String user = input_user.getText().toString();
        final String pass = input_pass.getText().toString();

        final Activity c = this;

        ton.addString(SettingValues.HOST.toString(), host);
        ton.addString(SettingValues.USER.toString(), user);
        ton.addString(SettingValues.PASSWORD.toString(), pass);

        Core.checkLogin(this, true, new FutureCallback<Boolean>() {
            @Override
            public void onCompleted(Exception e, Boolean result) {
                if (result) {
                    settings.edit()
                            .putString(SettingValues.HOST.toString(), host)
                            .putString(SettingValues.USER.toString(), user)
                            .putString(SettingValues.PASSWORD.toString(), pass)
                            .apply();

                    ton.getCallback(CallbackNames.LOGIN.toString()).onTaskFinished();
                    c.finish();
                }
                else {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());
                }

            }
        });*/
    }

    /**
     * Displays this activity
     * @param c
     * @param cb
     */
    public static void launch(Context c, ICallback cb) {
        SingleTon.getTon().addCallback(CallbackNames.LOGIN.toString(), cb);
        Intent i = new Intent(c, LoginActivity.class);
        c.startActivity(i);
    }

    @Override
    protected
    void onSaveInstanceState ( final Bundle outState )
    {
        String host = ton.getString( SettingValues.HOST.toString() );

        if ( host != null )
        {
            String user = ton.getString( SettingValues.USER.toString() );
            String pass = ton.getString( SettingValues.PASSWORD.toString() );

            outState.putString( SettingValues.USER.toString(), user );
            outState.putString( SettingValues.PASSWORD.toString(), pass );
            outState.putString( SettingValues.HOST.toString(), host );
        }

        super.onSaveInstanceState( outState );
    }

    private static
    class AccountListAdapter extends ArrayAdapter<Account>
    {

        private AccountManager mAccountManager;
        public
        AccountListAdapter (
              @NonNull final Context context, final int resource, @NonNull final Account[] objects )
        {
            super( context, resource, objects );
            mAccountManager = AccountManager.get( context );
        }


        @NonNull
        @Override
        public
        View getView (
              final int position, @Nullable final View convertView,
              @NonNull final ViewGroup parent )
        {
            View listItem = convertView;

            if ( listItem == null )
                listItem = LayoutInflater.from( getContext() )
                      .inflate( R.layout.login_list_item, parent, false );

            Account account = getItem( position );

//            ImageView image = (ImageView) listItem.findViewById( R.id.imageView_poster );
//            image.setImageResource( account.getmImageDrawable() );

            boolean haveAt = true;
            int atIndex = account.name.indexOf( '@' );
            if (atIndex < 0)
            {
                atIndex = account.name.length()-1;
                haveAt = false;
            }

            String userName = account.name.substring( 0, atIndex );
            String hostName = haveAt ? account.name.substring( atIndex+1 ) : account.type;

            TextView name = (TextView) listItem.findViewById( R.id.login_list_name );
            name.setText( userName );


            TextView host = (TextView) listItem.findViewById( R.id.login_list_host );
            String hostNameText = "(" + hostName + ")";
            host.setText( hostNameText );

            ImageView avatarView = (ImageView) listItem.findViewById( R.id.login_list_avatar );

            // avatars!
            // dav: https://cloud.tomasu.org/remote.php/dav/avatars/$userid/96
            //   Doesn't work, dav seems to be authenticated by default?
            // private url: https://host/index.php/avatar/$userid/96
            String uri = String.format( Locale.getDefault(), "https://%s/index.php/avatar/%s/%d", hostName, userName, 96 );
            Timber.d( "Get avatar: %s", uri );
            Ion.getDefault( getContext() ).getConscryptMiddleware().enable( false );
            Ion.with(getContext()).load( uri ).intoImageView( avatarView );

            return listItem;
        }
    }

    private static
    class NCAccountManagerCallback implements AccountManagerCallback< Bundle >
    {
        private LoginActivity mActivity;

        public NCAccountManagerCallback(@NonNull LoginActivity activity )
        {
            mActivity = checkNotNull( activity, "Null context?!");
        }


        @Override
        public
        void run (
              final AccountManagerFuture< Bundle > future )
        {
            Timber.d( "in account manager callback!" );
            try
            {
                SingleTon ton = SingleTon.getTon();


                Bundle results = future.getResult();

                Timber.d( "got auth token: %s",
                              results.toString() );


                String authUser = results.getString( "authAccount", "" );
                int atIndex = authUser.indexOf( '@' );

                String username = authUser.substring( 0, atIndex );
                String host = "https://" + authUser.substring( atIndex+1, authUser.length() );

                ton.addString( SettingValues.USER.toString(), username);

                String authtoken = results.getString( "authtoken" );
                if (authtoken == null)
                {
                    Timber.e( "failed to get an auth token?!" );
                    return;
                }

                ton.addString( SettingValues.PASSWORD.toString(), authtoken );

                ton.addString( SettingValues.HOST.toString(), host );

                Core.setUpAPI(host, username, authtoken);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                      mActivity );

                SharedPreferences.Editor editor = settings.edit();
                editor.putString( SettingValues.HOST.toString(), host );
                editor.putString( SettingValues.USER.toString(), username );
                editor.putString( SettingValues.PASSWORD.toString(), authtoken );
                editor.apply();

                PasswordList.launch( mActivity );
                mActivity.finish();
            }
            catch ( OperationCanceledException e )
            {
                e.printStackTrace();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            catch ( AuthenticatorException e )
            {
                e.printStackTrace();
            }
        }
    }


    private static
    class ItemClickListener implements AdapterView.OnItemClickListener
    {
        private AccountManagerFuture< Bundle > authTokenFuture;
        private AccountManager                 mAccountManager;
        private LoginActivity                  mActivity;
        private Account[] mAccounts;

        public ItemClickListener(@NonNull LoginActivity activity )
        {
            mActivity = checkNotNull( activity, "Null activity?!" );
            mAccountManager = AccountManager.get( mActivity );
        }

        private
        void setAccounts ( final Account[] accounts )
        {
            mAccounts = accounts;
        }

        @Override
        public
        void onItemClick (
              final AdapterView< ? > parent, final View view, final int position, final long id )
        {
            authTokenFuture = mAccountManager.getAuthToken( mAccounts[position], "nextcloud.password", null,
                                                            mActivity, new NCAccountManagerCallback(mActivity), null );
        }
    }
}
