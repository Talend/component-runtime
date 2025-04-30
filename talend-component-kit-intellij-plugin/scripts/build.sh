#!/bin/bash

set -x

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier"
GRADLE_CMD="../gradlew"
ARTIFACTS_DIR="../build/distributions"
# ---------------------------------

# --------------------------------------------
# 1. CHeck Java & Gradle (assuming Java and Gradle are pre-installed)
# --------------------------------------------
echo "Using Java version:"
java -version

echo "Using Gradle version:"
./gradlew --version


echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH="$JAVA_HOME/bin:$PATH"


# --------------------------------------------
#2. Get info
# --------------------------------------------
echo "==> Running 'gradlew properties' to get version"
PROPERTIES=$($GRADLE_CMD properties --console=plain -q)
VERSION=$(echo "$PROPERTIES" | grep "^version:" | cut -f2- -d ' ')
echo "Version: $VERSION"

echo "==> Building plugin"
$GRADLE_CMD buildPlugin

echo "==> Preparing plugin artifact"
cd "$ARTIFACTS_DIR"
FILENAME=$(ls *.zip)
unzip "$FILENAME" -d content
PLUGIN_DIR="${FILENAME%.zip}"

echo "==> Running tests"
cd ../..
$GRADLE_CMD check || {
  echo "==> Tests failed - collecting results"
  cp -r ./build/reports/tests ./test-results
}

echo "==> Verifying plugin with IntelliJ Plugin Verifier"
mkdir -p "$PLUGIN_VERIFIER_HOME/ides"
$GRADLE_CMD verifyPlugin -Dplugin.verifier.home.dir="$PLUGIN_VERIFIER_HOME"

echo "✅ All done."
