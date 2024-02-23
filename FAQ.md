# Frequently asked questions

- [How do I setup the app correctly after installation?](#how-do-i-setup-the-app-correctly-after-installation)
- [What is the design (intention) for log out on the vaults?](#what-is-the-design-intention-for-log-out-on-the-vaults)
- [How does the back button work?](#how-does-the-back-button-work)
- [How does the refresh button work?](#how-does-the-refresh-button-work)
- [How can I use the autofill feature?](#how-can-i-use-the-autofill-feature)
- [How does deleting credentials work?](#how-does-deleting-credentials-work)
- [Is there a smaller apk to download?](#i-dont-have-enough-storage-on-my-phone-to-install-passman-android-from-an-app-store-what-can-i-do)
- [What means "Encrypted offline cache"?](#what-means-encrypted-offline-cache)
- [How far can I trust the local storage encryption?](#how-far-can-i-trust-the-local-storage-encryption-is-it-save-to-store-my-vault-password-on-the-device)
- [How can I use a self signed certificate to connect the my Nextcloud server?](#how-can-i-use-a-self-signed-certificate-to-connect-the-my-nextcloud-server)
- [How can I connect to my 2FA secured account?](#how-can-i-connect-to-my-2fa-secured-account)


## How do I setup the app correctly after installation?
- Choose whether you want to use https or http for the server connection (recommended is using https, but it depends on your server setup)
- Fill in your Nextclouds hostname or IP address, and also the port if it's not the protocols default (443 for https; 80 for http)
    - Examples for allowed hostnames:
        - 10.0.2.2
        - 10.0.2.2:8080
        - mycloud.example.com/
- Fill in your Nextcloud user and password (will be stored encrypted)
- Press the connect button

## What is the design (intention) for log out on the vaults?
- If you unlocked a vault and the app is still running in the background, the vault stays open (so that you easily can switch between apps)
- If you close the app all vaults will be locked
- As long as the app is running you can switch between vaults (and they remain unlocked)
- If you set a tick to save the vault password, technically the "vault is locked" if the app is closed but you don't have to manually unlock it until you press (manually!) the lock button
- Pressing the lock button will lock the vault and also remove a saved vault password (if it was saved before)

## How does the back button work?
- Hitting the back button from where ever you are, ends in going back to the "parent" place (that's usually the page you have seen before)
- If you press back in the vaults overview, the app remains in the background

## How does the refresh button work?
- If you are in an opened vault, the refresh button will refresh the current vault. And thus also the password list.
    - If the vaults encryption key did not change the vault remains unlocked
- If you are in the vaults overview, the refresh button will clear and refresh the complete vaults storage
    - This does not affect the offline cache of an explicitly selected autofill vault

## How can I use the autofill feature?
- It requires at least Android 8
- Passman Android currently offers the autofill feature automatically to the system.
- To use Passman as autofill service, you need to select the app in the Android settings as autofill service
    - where to find that setting is different on any Android device
    - an example path in the Android settings could be `More Settings` -> `Language and Input` -> `Additional Keyboard Settings` -> `Autofill Service`
- If you select a custom vault in the Passman Android settings as autofill vault, it will be cached offline, and if you also save the vault password, you won't need a network connection or manually open the Passman app  before using autofill in an other app.
- By default in the Passman Android settings the autofill vault is set to automatically, so that Passman needs to run in the background and the currently unlocked vault is used for the autofill service

## How does deleting credentials work?
- If you delete a credential in the Passman Android app, it will be moved to the trash on the server side ("Deleted credentials" section in the web view)
- You don't have to be anxious to accidentally delete it, you have to confirm the operation
- You can't access the deleted credentials with the Passman Android app at the moment

## I don't have enough storage on my phone to install Passman Android from an App Store, what can I do?
- You could try to install the apk from the GitHub release which matches to your phones CPU architecture and is usually smaller than the App Stores version
    - https://github.com/nextcloud/passman-android/releases/latest
- The apks that are delivered from the App Stores combines the required files for all supported architectures

## What means "Encrypted offline cache"?
- By default vaults and credentials are stored in the offline cache
- If your device has at least Android 6 / API 23 the offline cache will be stored encrypted
    - Since credentials are already encrypted with the vault password, they will be encrypted twice
- It's called cache because it works like a read-only fallback mode in case your cloud is not reachable over the network
    - that means vaults and credentials can not be edited without a working cloud connection

## How far can I trust the local storage encryption? Is it save to store my vault password on the device?
- The [Android keystore system](https://developer.android.com/training/articles/keystore) is used to encrypt a random generated password with AES/GCM
    - The Android keystore system uses special hardware mechanisms to protect the key
- This random generated password is used to encrypt all locally stored sensitive data (like the offline cache and stored vault passwords) with the AES-256 encryption that is already used to encrypt credentials
- If you trust the Android keystore system it should be safe to store your vault password on the device
    - But don't forget that the security of the saved passwords depends on the access protection of your Android phone if you store your vault password on the device!

## How can I use a self signed certificate to connect the my Nextcloud server?
- Since version 1.0.0 the app supports user CA (certificate authority) certificates (requires at least Android 7 / API level 24).
- The custom CA has to be imported in the Android trusted certificates section.
    - It should be somewhere like `Android Settings -> Security -> Install certificate from storage`

**This is an example how a CA and certificate could be generated that will be accepted by Android and an apache2 webserver:**

Create an auxiliary file "android_options.txt" with this line inside:

    basicConstraints=CA:true


Create self-signed certificate using these commands:

    openssl genrsa -out CA.key 2048
    openssl req -new -days 3650 -key CA.key -out CA.pem
    openssl x509 -req -days 3650 -in CA.pem -signkey CA.key -extfile ./android_options.txt -out CA.crt 

Now our CA.crt certificate is almost ready.
Convert certificate to DER format:

    openssl x509 -inform PEM -outform DER -in CA.crt -out CA.der.crt


Generate a server key and request for signing (CSR):

Make sure the "Common Name" matches the used host name (or ip address if no host name is used).

    openssl genrsa -des3 -out server.key 4096
    openssl req -new -key server.key -out server.csr

Sign a certificate with CA:

    openssl x509 -req -days 365 -in server.csr -CA CA.crt -CAkey CA.key -CAcreateserial -out server.crt

Remove the passphrase from the certificate key to use it with apache2 without entering the password on service start:

    openssl rsa -in server.key -out server.key.insecure

Use `server.crt` as certificate and `server.key.insecure` as key for your apache2 host configuration.

Import `CA.der.crt` as android user certificate.

## How can I connect to my 2FA secured account?
- Unfortunately Passman Android does not implement a native 2FA login
- Workaround 1: Connect Passman Android using Single-Sign-On (SSO) with the Nextcloud Files App
- Workaround 2: You need to create a device password (see https://github.com/nextcloud/passman-android/issues/70#issuecomment-691544624)

