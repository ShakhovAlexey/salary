#!/bin/bash
set -e
SDK=/home/claude/android-sdk; BT=$SDK/build-tools/35.0.0; JAR=$SDK/platforms/android-34/android.jar
KS=/mnt/user-data/outputs/salary-app-key.keystore
cd "$(dirname "$0")"; rm -rf build; mkdir -p build/res build/gen build/classes build/dex
$BT/aapt2 compile --dir res -o build/res/
$BT/aapt2 link -o build/base.apk -I $JAR --manifest AndroidManifest.xml --java build/gen build/res/*.flat --auto-add-overlay
javac -source 8 -target 8 -nowarn -XDstringConcat=inline -classpath $JAR -d build/classes $(find src build/gen -name '*.java') 2>&1 | grep -v "warning: \[options\]" || true
(cd build/classes && jar cf ../classes.jar .)
$BT/d8 --release --min-api 26 --lib $JAR --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk; (cd build/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 build/unsigned.apk build/aligned.apk
[ -f $KS ] || keytool -genkeypair -keystore $KS -alias salary -keyalg RSA -keysize 2048 -validity 10000 -storepass salary-app-2026 -keypass salary-app-2026 -dname "CN=Salary App, O=Aleksei" >/dev/null 2>&1
$BT/apksigner sign --ks $KS --ks-key-alias salary --ks-pass pass:salary-app-2026 --key-pass pass:salary-app-2026 --out build/Zarplata.apk build/aligned.apk
$BT/apksigner verify --print-certs build/Zarplata.apk | head -2
ls -la build/Zarplata.apk
