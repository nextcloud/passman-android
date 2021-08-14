# Passman Android

[![Latest Release](https://img.shields.io/github/v/tag/nextcloud/passman-android?label=latest+release&sort=semver)](https://github.com/nextcloud/passman-android/releases)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
alt="Get it on Play Store"
height="80">](https://play.google.com/store/apps/details?id=es.wolfi.app.passman.alpha)
[<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/app/es.wolfi.app.passman)

This app is only compatible with passman V2.x or higher.   
The passwords will be provided by [passman](https://github.com/nextcloud/passman).

## Current features
- Setup app (enter the nextcloud server settings)
- App start password option based on the android user authentication
- Display vault list
- Login to vault
- Display credential list
- View, add, edit and delete credentials
- Add, download and delete files
- OTP generation
- Basic Android autofill implementation

## FAQ

### How do I setup the app correctly after installation?
- Choose whether you want to use https or http for the server connection (recommended is using https, but it depends on your server setup)
- Fill in your Nextclouds hostname or IP address, and also the port if it's not the protocols default (443 for https; 80 for http)
   - Examples for allowed hostnames:
      - 10.0.2.2
      - 10.0.2.2:8080
      - mycloud.example.com/
- Fill in your Nextcloud user and password
- Press the connect button

### What is the design (intention) for log out on the vaults?
- If you unlocked a vault and the app is still running in the background, the vault stays open (so that you easily can switch between apps)
- If you close the app all vaults will be locked
- As long as the app is running you can switch between vaults (and they remain unlocked)
- If you set a tick to save the vault password, technically the "vault is locked" if the app is closed but you don't have to manually unlock it until you press (manually!) the lock button
- Pressing the lock button will lock the vault and also remove a saved vault password (if it was saved before)

### How does the back button work?
- Hitting the back button from where ever you are, ends in going back to the "parent" place (that's usually the page you have seen before)
- If you press back in the vaults overview, the app remains in the background

### How does the refresh button work?
- If you are in an opened vault, the refresh button will refresh the current vault. And thus also the password list.
   - If the vaults encryption key did not change the vault remains unlocked
- If you are in the vaults overview, the refresh button will clear and refresh the complete vaults storage
   - This does not affect the offline cache of an explicitly selected autofill vault

### How can I use the autofill feature?
- It requires at least Android 8
- Passman Android currently offers the autofill feature automatically to the system.
- To use Passman as autofill service, you need to select the app in the Android settings as autofill service
   - where to find that setting is different on any Android device
   - an example path in the Android settings could be `More Settings` -> `Language and Input` -> `Additional Keyboard Settings` -> `Autofill Service`
- If you select a custom vault in the Passman Android settings as autofill vault, it will be cached offline, and if you also save the vault password, you won't need a network connection or manually open the Passman app  before using autofill in an other app.
- By default in the Passman Android settings the autofill vault is set to automatically, so that Passman needs to run in the background and the currently unlocked vault is used for the autofill service

### How does deleting credentials work?
- If you delete a credential in the Passman Android app, it will be moved to the trash on the server side ("Deleted credentials" section in the web view)
- You don't have to be anxious to accidentally delete it, you have to confirm the operation
- You can't access the deleted credentials with the Passman Android app at the moment

### I don't have enough storage on my phone to install Passman Android from an App Store, what can I do?
- You could try to install the apk from the GithHub release which matches to your phones CPU architecture and is usually smaller than the App Stores version
   - https://github.com/nextcloud/passman-android/releases/latest
- The apks that are delivered from the App Stores combines the required files for all supported architectures

## Build locally

### Required packages
- cmake
- gcc
- git

### Commands

1. Clone the repo
1. Setup the git submodules with `git submodule update --init --recursive`
1. Open project in Android Studio and install the SDK an the NDK
1. Copy `openssl.conf.example` to `openssl.conf`
1. Edit the `ANDROID_NDK_HOME` in openssl.conf to match your local NDK path
1. Edit the `HOST_TAG` in openssl.conf to match your system arch
1. Run `build-openssl.sh`
1. If you want to compile either an alpha or release version, create a keystore either
   with Android Studio or `keytool` and add at least a key for the alpha build:
    ```
    keytool -genkey -v -keystore keystore.jks -alias beta -keyalg rsa
    ```
1. Create a `gradle.properties` file based on `gradle.properties.example` and fill in the
   appropriate values for your keystore. If you only build debug builds you can leave
   the default values.
1. Use Android Studio to build or otherwise build with gradle.

## Testing server
[Here](https://demo.passman.cc/) you can use our demo system.
