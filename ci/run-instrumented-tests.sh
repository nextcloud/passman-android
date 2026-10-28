#!/usr/bin/env bash
# Installs the given app apk (e.g. one ABI split) and the androidTest apk on the connected device
# and runs the instrumented tests of the given packages. Fails if any test fails or the app crashes.
#
# Usage: ci/run-instrumented-tests.sh <app.apk> <androidTest.apk> <test packages> [am instrument args...]
# Example: ci/run-instrumented-tests.sh app-arm64-v8a-ciRelease.apk app-ciRelease-androidTest.apk \
#              es.wolfi.app.passman.offline,es.wolfi.app.passman.e2e -e serverUrl http://10.0.2.2:8080
#
# Select the device with ANDROID_SERIAL if more than one is connected.
# The instrumentation output, logcat and the screenshot of a failed e2e step end up in TEST_OUTPUT_DIR
# (default: ci-output). The e2e test disables the screenshot protection of the app for that,
# pass -e screenshotProtection true to keep it enabled.

set -euo pipefail

if [ $# -lt 3 ]; then
    sed -n '2,11p' "$0"
    exit 2
fi

APP_APK=$1
TEST_APK=$2
TEST_PACKAGES=$3
shift 3

ANDROID_HOME=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}
AAPT2=$(printf '%s\n' "$ANDROID_HOME"/build-tools/*/aapt2 | sort -V | tail -1)
APP_ID=$("$AAPT2" dump packagename "$APP_APK")
TEST_ID=$("$AAPT2" dump packagename "$TEST_APK")
OUTPUT_DIR=${TEST_OUTPUT_DIR:-ci-output}
mkdir -p "$OUTPUT_DIR"

collect_diagnostics() {
    local status=$?
    adb logcat -d > "$OUTPUT_DIR/logcat.txt" 2>&1 || true
    if [ $status -ne 0 ]; then
        # taken by the e2e test at the moment of the failure
        adb pull /data/local/tmp/passman-e2e-failure.png "$OUTPUT_DIR/" >/dev/null 2>&1 || true
    fi
}
trap collect_diagnostics EXIT

echo "Device: $(adb shell getprop ro.product.model) (API $(adb shell getprop ro.build.version.sdk), ABIs $(adb shell getprop ro.product.cpu.abilist))"

# animations make Espresso flaky
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# start from a clean state, the tests log in and unlock themselves
adb uninstall "$TEST_ID" >/dev/null 2>&1 || true
adb uninstall "$APP_ID" >/dev/null 2>&1 || true
adb install -r "$APP_APK"
adb install -r -t "$TEST_APK"

# Android 17+ asks for the permission to reach the test server in the local network (10.0.2.2) on login,
# the tests can't answer the system dialog
if [ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" -ge 37 ]; then
    adb shell pm grant "$APP_ID" android.permission.ACCESS_LOCAL_NETWORK
fi

echo "Native library ABI of $APP_ID: $(adb shell dumpsys package "$APP_ID" | grep -m1 -o 'primaryCpuAbi=[^ ]*')"

adb logcat -c || true
adb shell rm -f /data/local/tmp/passman-e2e-failure.png
adb shell am instrument -w -e package "$TEST_PACKAGES" "$@" "$TEST_ID/androidx.test.runner.AndroidJUnitRunner" \
    | tee "$OUTPUT_DIR/instrumentation.txt"

# am instrument always exits with 0, the summary line tells the result
grep -q '^OK (' "$OUTPUT_DIR/instrumentation.txt"
