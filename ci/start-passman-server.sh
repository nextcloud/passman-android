#!/usr/bin/env bash
# Starts a fresh Passman test server (Nextcloud with Passman) in docker and restores the test vault
# of ci/test-vault into it. The Android emulator reaches it via http://10.0.2.2:<port>.
#
# Usage: ci/start-passman-server.sh [docker image]
# Environment: PASSMAN_SERVER_PORT (default 8080), PASSMAN_CONTAINER (default passman-ci)

set -euo pipefail

# todo: change to :latest, once the cmd-backups branch is merged and published
IMAGE=${1:-docker.io/binsky/passman-dev:cmd-backups}
CONTAINER=${PASSMAN_CONTAINER:-passman-ci}
PORT=${PASSMAN_SERVER_PORT:-8080}
TEST_VAULT_DIR=$(cd "$(dirname "$0")/test-vault" && pwd)
NEXTCLOUD_USER=$(sed -n 's/^nextcloud.user=//p' "$TEST_VAULT_DIR/test-vault.properties")
NEXTCLOUD_PASSWORD=$(sed -n 's/^nextcloud.password=//p' "$TEST_VAULT_DIR/test-vault.properties")
VAULT_GUID=$(sed -n 's/^vault.guid=//p' "$TEST_VAULT_DIR/test-vault.properties")

occ() {
    docker exec -u www-data "$CONTAINER" php /var/www/html/occ "$@"
}

docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
docker run -d --name "$CONTAINER" -p "$PORT:80" "$IMAGE" >/dev/null

# the entrypoint enables Passman before it starts the web server
echo "Waiting for Nextcloud on port $PORT ..."
for _ in $(seq 1 30); do
    if curl -fsS -m 5 "http://localhost:$PORT/status.php" 2>/dev/null | grep -q '"installed":true'; then
        break
    fi
    sleep 2
done
curl -fsS -m 5 "http://localhost:$PORT/status.php"
echo

# requests of the emulator arrive with the host header 10.0.2.2:<port>
occ config:system:set trusted_domains 5 --value="10.0.2.2:$PORT"

docker cp "$TEST_VAULT_DIR/backup.json" "$CONTAINER:/tmp/backup.json"
docker exec "$CONTAINER" chown www-data /tmp/backup.json
occ passman:restore --input=/tmp/backup.json --mode=merge --no-interaction

# the vault has to be visible through the API the app uses
curl -fsS -m 30 -u "$NEXTCLOUD_USER:$NEXTCLOUD_PASSWORD" \
    "http://localhost:$PORT/index.php/apps/passman/api/v2/vaults/$VAULT_GUID" | grep -q "\"guid\":\"$VAULT_GUID\""
echo "Passman test server is ready: http://localhost:$PORT (emulator: http://10.0.2.2:$PORT)"
