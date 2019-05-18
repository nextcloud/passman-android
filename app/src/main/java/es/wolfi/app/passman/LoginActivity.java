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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import java.io.IOException;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Core;
import timber.log.Timber;

public class LoginActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mAccountManager = AccountManager.get( this );
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ton = SingleTon.getTon();

        mListView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public
            void onItemClick (
                  final AdapterView< ? > parent, final View view, final int position,
                  final long id )
            {
                Account account = (Account) parent.getItemAtPosition( position );

                AccountManagerFuture< Bundle > authTokenFuture =
                      mAccountManager.getAuthToken( account,
                                                    "nextcloud",
                                                    null,
                                                    LoginActivity.this,
                                                    new NCAccountManagerCallback(), null );
            }
        } );

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String action = intent.getAction();

//        if (action == null || !action.contentEquals( ACTION_AUTH_RETURN ))
//        {
//            Timber.d( "get host!" );
//            // show get host page!
//            return;
//        }

        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.GET_ACCOUNTS ) !=
              PackageManager.PERMISSION_GRANTED )
        {
            Timber.d( "don't have GET_ACCOUNTS permission" );

            if ( ActivityCompat.shouldShowRequestPermissionRationale( this, Manifest.permission.GET_ACCOUNTS ) )
            {
                // show explanation..
                Timber.d( "should show request permission!" );
            }
            // else {
            //
            //}

            Timber.d( "request GET_ACCOUNTS!" );
            ActivityCompat.requestPermissions( this,
                                               new String[] { android.Manifest.permission.GET_ACCOUNTS },
                                               REQUEST_GET_ACCOUNTS );
        }
        else
        {
            Timber.d( "already have GET_ACCOUNTS permission" );
            getAccount();
        }

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
                Timber.d( "choose account: %s", data.toString() );
                break;

            default:
               super.onActivityResult( requestCode, resultCode, data );
               break;
        }
    }

    @Override
    public
    void onRequestPermissionsResult (
          final int requestCode, @NonNull final String[] permissions,
          @NonNull final int[] grantResults )
    {
        Timber.d( "onRequestPermissionsResult" );

        if (requestCode != REQUEST_GET_ACCOUNTS)
        {
            Timber.w( "unsupported request code?!" );
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            getAccount();
        }
        else
        {
            Timber.w( "permission denied >:(" );
            // we need this permission, try and reask or bail?
        }
    }

    private
    void getAccount()
    {
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
        Account[] accounts = accountManager.getAccountsByType("nextcloud");
        Timber.d( "accounts: %d", accounts.length );
        //accountManager.

        mListView.setAdapter( new AccountListAdapter( this, R.layout.login_list_item, accounts) );

        for ( Account account : accounts )
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

    private
    class NCAccountManagerCallback implements AccountManagerCallback< Bundle >
    {
        @Override
        public
        void run (
              final AccountManagerFuture< Bundle > future )
        {
            Timber.d( "in account manager callback!" );
            try
            {
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

                SharedPreferences.Editor editor = settings.edit();
                editor.putString( SettingValues.HOST.toString(), host );
                editor.putString( SettingValues.USER.toString(), username );
                editor.putString( SettingValues.PASSWORD.toString(), authtoken );
                editor.apply();

                PasswordList.launch( LoginActivity.this );
                finish();
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

    @Override
    protected
    void onSaveInstanceState ( final Bundle outState )
    {
        String host = ton.getString( SettingValues.HOST.toString() );

        if (host != null)
        {
            String user = ton.getString( SettingValues.USER.toString() );
            String pass = ton.getString( SettingValues.PASSWORD.toString() );

            outState.putString( SettingValues.USER.toString(), user );
            outState.putString( SettingValues.PASSWORD.toString(), pass );
            outState.putString( SettingValues.HOST.toString(), host );
        }

        super.onSaveInstanceState( outState );
    }
}
