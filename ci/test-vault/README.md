# CI test vault

Public test data for the instrumented tests. Never put real secrets in here.

| File | Content |
|---|---|
| `backup.json` | `occ passman:backup` export (scope `user`, encryption mode `portable`) of one vault. The CI restores it into a fresh `binsky/passman-dev` container. |
| `frontend-export.json` | Plaintext JSON export of the same vault, made with the Passman web client. The tests use it as expected values. |
| `test-vault.properties` | Nextcloud login, vault GUID, name and password. |

The tests compare `backup.json` and `frontend-export.json` by credential GUID, so they don't hardcode any credential.
The directory is packaged into the androidTest apk as assets (see `sourceSets.androidTest` in `app/build.gradle`).

## Replacing the test data

1. Create a vault with the Passman web client and fill it with test credentials (files, custom fields, OTP, ...).
2. Export it in the web client as plaintext JSON to `frontend-export.json`.
3. Export the vault owner's data on the server to `backup.json`:

   ```shell
   occ passman:backup --scope=user --user=<user> --output=/tmp/backup.json
   ```

   Don't edit `backup.json` afterwards, the restore checks its checksum.
4. Update `test-vault.properties`.
