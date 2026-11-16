#!/bin/bash
# Compila o APK sem Gradle, usando o SDK do Ubuntu (android-23) + dx.
set -euo pipefail
cd "$(dirname "$0")"
SDK=/usr/lib/android-sdk
JAR=$SDK/platforms/android-23/android.jar
rm -rf build && mkdir -p build/gen build/classes build/dex app
aapt2 compile --dir res -o build/res.zip
aapt2 link -I "$JAR" --manifest AndroidManifest.xml -o build/base.apk \
  --java build/gen --min-sdk-version 23 --target-sdk-version 34 build/res.zip
javac --release 8 -nowarn -Xlint:-options -cp "$JAR" -d build/classes \
  $(find src build/gen -name '*.java')
dalvik-exchange --dex --min-sdk-version=23 --output=build/dex/classes.dex build/classes
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
zipalign -f 4 build/unsigned.apk build/aligned.apk
[ -f chave.jks ] || keytool -genkeypair -keystore chave.jks -storepass mensagem -keypass mensagem \
  -alias app -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Mensagem Rapida" >/dev/null 2>&1
apksigner sign --ks chave.jks --ks-pass pass:mensagem --ks-key-alias app \
  --out app/Chegou.apk build/aligned.apk
apksigner verify app/Chegou.apk && rm -f app/Chegou.apk.idsig && echo "OK: $(pwd)/app/Chegou.apk"
