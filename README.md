# passman-android
Passman Android application

This app is only compatible with passman V2.x or higher.   
The passwords will be provided by [passman](https://github.com/nextcloud/passman).
## Current features
- Setup app (enter the nextcloud server settings)
- Display vault list
- Login to vault
- Display credential list
- Display a single credential (with edit fields)
- OTP generation
- Create credential (Basic)
- Edit credential (Basic)
- Android autofill support (Basic)

## Build locally

1. Clone the repo
1. Setup the git submodules with `git submodule update --init --recursive`
1. Install the SDK an the NDK
1. Copy `Openssl.conf.sample` to `Openssl.conf`
1. Edit the NDK path in Openssl.conf to match your local NDK path
1. Run `SetupOpenssl.sh`
1. If you want to compile either an alpha or release version, create a keystore either
with Android Studio or `keytool` and add at least a key for the alpha build:
    ```
    keytool -genkey -v -keystore keystore.jks -alias beta
    ```
1. Create a gradle.properties file based on gradle.properties.example and fill in the
appropriate values for your keystore. If you only build debug builds you can leave
the default values.
1. Use Android Studio to build or otherwise build with gradle.

## Testing server
If you want access to a passman testing server, poke us on irc (freenode/#passman-dev)
