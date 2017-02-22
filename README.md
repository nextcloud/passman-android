# passman-android
Passman Android app

### Build local

- Setup the git submodule with `git submodule update --init --recursive`
- Install NDK
- Copy `Openssl.conf.sample` to `Openssl.conf`
- Edit the NDK path in Openssl.conf to match your local NDK path
- Run `SetupOpenssl.sh`

## Current features
- Setup app (enter the nextcloud server settings)
- Display vault list
- Login to vault
- Display credential list
- Display a single credential (with edit fields)


## Testing server
If you want access to a passman testing server, poke us on irc (freenode/#passman-dev)