package es.wolfi.app.passman;

import android.app.Application;

import timber.log.Timber;

/**
 * @version ${VERSION}
 * @since ${VERSION}
 */
public
class App extends Application
{
	@Override
	public
	void onCreate ()
	{
		super.onCreate();

		if (BuildConfig.DEBUG)
		{
			Timber.plant(new Timber.DebugTree());
		}
		else
		{
			//Timber.plant( new CrashReportingTree() );
		}
	}
}
