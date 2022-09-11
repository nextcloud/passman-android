# Passman Android

[![Latest Release](https://img.shields.io/github/v/tag/nextcloud/passman-android?label=latest+release&sort=semver)](https://github.com/nextcloud/passman-android/releases)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
alt="Get it on Play Store"
height="80">](https://play.google.com/store/apps/details?id=es.wolfi.app.passman.alpha)
[<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/app/es.wolfi.app.passman)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
alt="Get it on IzzyOnDroid"
height="80">](https://apt.izzysoft.de/fdroid/index/apk/es.wolfi.app.passman)

This app is only compatible with Passman V2.x or higher.   
The passwords will be provided by [Passman](https://github.com/nextcloud/passman).

## Current features
- Setup app (enter the nextcloud server settings or use SSO)
- App start password option based on the android user authentication
- View, add, rename and delete vaults
- Login to vault
- Display credential list
- View, add, edit and delete credentials
- Add, download and delete files
- OTP generation
- Basic Android autofill implementation
- Password generator
- Encrypted offline cache
- Encrypted stored vault and cloud connection passwords

## FAQ
Read our [frequently asked questions article](FAQ.md)

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

## Support Passman
Passman is open source but we’ll gladly accept a beer *or pizza!* Please consider donating:
* [PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=6YS8F97PETVU2)
* [Patreon](https://www.patreon.com/user?u=4833592)
* [Flattr](https://flattr.com/@passman)
* bitcoin: 1H2c5tkGX54n48yEtM4Wm4UrAGTW85jQpe
