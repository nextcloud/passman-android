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

## Build locally

1. Clone the repo
1. Setup the git submodules with `git submodule update --init --recursive`
1. Install the SDK an the NDK
1. Copy `Openssl.conf.sample` to `Openssl.conf`
1. Edit the NDK path in Openssl.conf to match your local NDK path
1. Run `SetupOpenssl.sh`
1. Use android studio to build or otherwise build with gradle.

## Testing server
If you want access to a passman testing server, poke us on irc (freenode/#passman-dev)