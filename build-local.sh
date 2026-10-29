#!/usr/bin/env bash
# Minimal dependency-free build using official Android SDK tools (no Gradle download required).
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 17 or newer}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
export PATH="$JAVA_HOME/bin:$PATH"
BT="$ANDROID_HOME/build-tools/36.0.0"
ANDROID="$ANDROID_HOME/platforms/android-36/android.jar"
OUT="$PWD/build-local"
rm -rf "$OUT/classes" "$OUT/generated" "$OUT/dex"
mkdir -p "$OUT/classes" "$OUT/generated" "$OUT/dex"
"$BT/aapt2" compile --dir app/src/main/res -o "$OUT/resources.zip"
sed 's/<manifest /<manifest package="cn.local.quickrecorder" /' app/src/main/AndroidManifest.xml > "$OUT/AndroidManifest.xml"
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$ANDROID" --manifest "$OUT/AndroidManifest.xml" \
    --min-sdk-version 29 --target-sdk-version 36 --version-code 3 --version-name 0.3-test \
    --java "$OUT/generated" "$OUT/resources.zip"
find app/src/main/java "$OUT/generated" -name '*.java' | sort > "$OUT/sources.txt"
javac -encoding UTF-8 -source 17 -target 17 -classpath "$ANDROID" -d "$OUT/classes" @"$OUT/sources.txt"
jar --create --file "$OUT/classes.jar" -C "$OUT/classes" .
"$BT/d8" --lib "$ANDROID" --min-api 29 --output "$OUT/dex" "$OUT/classes.jar"
(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
# Local test-only signing identity; do not publish/include its private key in source archives.
KEYDIR="$PWD/.test-signing"
mkdir -p "$KEYDIR"
if [ ! -f "$KEYDIR/test.jks" ]; then
    keytool -genkeypair -keystore "$KEYDIR/test.jks" -storepass android -keypass android \
      -alias androiddebugkey -dname "CN=QuickRecorder Local Test,O=Local Test,C=CN" \
      -keyalg RSA -keysize 2048 -validity 3650 >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KEYDIR/test.jks" --ks-pass pass:android --ks-key-alias androiddebugkey \
    --out "$OUT/QuickRecorder-0.3-test.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --verbose --print-certs "$OUT/QuickRecorder-0.3-test.apk"
"$BT/zipalign" -c -v 4 "$OUT/QuickRecorder-0.3-test.apk"
sha256sum "$OUT/QuickRecorder-0.3-test.apk"
