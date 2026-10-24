#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Ensure SecretsConfig exists
if [ ! -f "src/de/ailimits/app/SecretsConfig.java" ]; then
    echo "=== Creating SecretsConfig.java from template ==="
    cp src/de/ailimits/app/SecretsConfig.java.template src/de/ailimits/app/SecretsConfig.java
fi

# Detect environment or use bundled tools
JAVA_HOME="${JAVA_HOME:-/home/michael/.gemini/antigravity/scratch/tools/jdk-21}"
ANDROID_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/home/michael/.gemini/antigravity/scratch/tools/android-sdk}}"
BUILD_TOOLS="$ANDROID_SDK/build-tools/34.0.0"
PLATFORM="$ANDROID_SDK/platforms/android-34/android.jar"
KEYSTORE="${KEYSTORE:-/home/michael/.gemini/antigravity/scratch/tools/debug.keystore}"

# Generate debug keystore if missing
if [ ! -f "$KEYSTORE" ]; then
    echo "=== Generating debug.keystore ==="
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkey -v -keystore "$KEYSTORE" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

export PATH="$JAVA_HOME/bin:$BUILD_TOOLS:$ANDROID_SDK/platform-tools:$PATH"

echo "=== Cleaning build dir ==="
rm -rf build/compiled_res build/gen build/obj build/ai-limits*.apk build/classes.dex
mkdir -p build/compiled_res build/gen build/obj

echo "=== 1. Compiling resources with aapt2 ==="
"$BUILD_TOOLS/aapt2" compile --dir res -o build/compiled_res/res.zip

echo "=== 2. Linking resources with aapt2 ==="
"$BUILD_TOOLS/aapt2" link \
    -I "$PLATFORM" \
    --manifest AndroidManifest.xml \
    --java build/gen \
    -o build/ai-limits-unsigned.apk \
    --auto-add-overlay \
    build/compiled_res/res.zip

echo "=== 3. Compiling Java sources with javac ==="
javac -source 11 -target 11 \
    -cp "$PLATFORM" \
    -d build/obj \
    build/gen/de/ailimits/app/R.java \
    src/de/ailimits/app/*.java

echo "=== 4. Converting bytecode to DEX with d8 ==="
"$BUILD_TOOLS/d8" \
    --min-api 26 \
    --lib "$PLATFORM" \
    --output build/ \
    $(find build/obj -name "*.class")

echo "=== 5. Adding classes.dex to APK ==="
cd build
jar -uf ai-limits-unsigned.apk classes.dex
cd ..

echo "=== 6. Aligning APK with zipalign ==="
"$BUILD_TOOLS/zipalign" -v -p 4 build/ai-limits-unsigned.apk build/ai-limits-aligned.apk

echo "=== 7. Signing APK with apksigner ==="
"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out build/ai-limits.apk \
    build/ai-limits-aligned.apk

echo "=== Build complete! APK generated at: build/ai-limits.apk ==="
ls -lh build/ai-limits.apk
