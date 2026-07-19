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

**Passman for Android** is the official mobile companion for the [Passman](https://github.com/nextcloud/passman) Nextcloud extension. It provides a secure, self-hosted alternative to proprietary password managers, keeping your credentials synchronized across your devices without compromising your privacy.

> [!NOTE]
> This app requires a running Nextcloud instance with the Passman extension installed.

## Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="200">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="200">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="200">
</p>

## Features

### Vault Management
- View, add, rename, and delete vaults
- Secure vault login with encrypted stored passwords
- Support for multiple vaults with easy switching
- Option to close the vault after a period of not being used

### Credential Handling
- Full CRUD operations: View, add, edit, and delete credentials
- OTP (One-Time Password) generation for 2FA
- Integrated password generator
- Support for adding, downloading, and deleting file attachments

### Security & Integration
- Modern Android user authentication (Biometrics/PIN/Pattern) for app start
- Android Autofill implementation (requires Android 8+)
- Encrypted offline cache for reliable access without a network connection
- Secure storage using the Android Keystore system
- Optional screenshot protection

## FAQ
For troubleshooting and detailed guides, please read our [Frequently Asked Questions](FAQ.md).

## Demo System
You can test the Passman ecosystem using our [demo system](https://demo.passman.cc/).

## Development

### Prerequisites
- CMake, GCC, and Git
- Android Studio with SDK and NDK installed

### Build Steps
1. **Clone & Initialize**:
   ```bash
   git clone https://github.com/nextcloud/passman-android.git
   cd passman-android
   git submodule update --init --recursive
   ```
2. **OpenSSL Configuration**:
   - Copy `openssl.conf.example` to `openssl.conf`.
   - Update `ANDROID_NDK_HOME` and `HOST_TAG` in `openssl.conf` to match your environment.
   - Run `./build-openssl.sh`.
3. **Signing (Optional for Release/Alpha)**:
   - Create a keystore: `keytool -genkey -v -keystore keystore.jks -alias beta -keyalg rsa`.
   - Configure `gradle.properties` based on `gradle.properties.example`.
   - Fill in the appropriate values for your keystore. If you only build debug builds you can leave the default values.
4. **Android Studio**: If not already done, open the project in Android Studio and install the SDK an the NDK
5. **Build**: Build via Android Studio or run `./gradlew assembleDebug`.

## Support Passman
Passman is open source and thrives on community contributions. Whether it's [pull requests](https://github.com/nextcloud/passman-android/pulls) or feedback, all help is welcome!

Please consider donating (we'll gladly accept a _pizza_):
* [Patreon](https://www.patreon.com/passman)
* [Ko-Fi](https://ko-fi.com/passman)
