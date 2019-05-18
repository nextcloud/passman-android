package es.wolfi.app.passman;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import es.wolfi.passman.API.Core;
import timber.log.Timber;

public
class SplashActivity extends AppCompatActivity
{
	SharedPreferences settings;
	SingleTon         ton;

	@Override
	protected
	void onCreate ( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		Timber.d( "onCreate" );

		settings = PreferenceManager.getDefaultSharedPreferences( this );
		ton = SingleTon.getTon();

		if (Core.haveHost( this ) )
		{
			Timber.d( "have host! launch password list!" );

			// go to PasswordList
			//
			PasswordList.launch( this );

			Timber.d( "finish!" );
			finish();
			return;
		}

		// go to login

		Timber.d( "go to login!" );
		LoginActivity.launch( this, new LoginICallback() );
		finish();
	}

	private static
	class LoginICallback implements ICallback
	{
		@Override
		public
		void onTaskFinished ()
		{
			// nada
		}
	}
}
