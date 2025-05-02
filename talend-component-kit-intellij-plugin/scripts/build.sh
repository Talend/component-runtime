#!/bin/bash

set -xeuo pipefail

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier" # this one should be in EFS
GRADLE_CMD="./gradlew"
ARTIFACTS_DIR="build/distributions"
# ---------------------------------

# --------------------------------------------
# 1. Check Java & Gradle (assuming Java and Gradle are pre-installed)
# --------------------------------------------
echo "Using Java version:"
java -version

# go up into plugin root folder
pushd ..

echo "Using Gradle version:"
$GRADLE_CMD --version


echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))


# --------------------------------------------
# 2. Get info
# --------------------------------------------
echo "==> Running 'gradlew properties' to get version"
PROPERTIES=$($GRADLE_CMD properties --console=plain -q)
VERSION=$(echo "$PROPERTIES" | grep "^version:" | cut -f2- -d ' ')
echo "Version: $VERSION"

echo "==> Building plugin"
$GRADLE_CMD buildPlugin

echo "==> Running tests"
$GRADLE_CMD check || {
  echo "==> Tests failed - collecting results"
  cp -r ./build/reports/tests ./test-results
}

echo "==> Verifying plugin with IntelliJ Plugin Verifier"
mkdir -p "$PLUGIN_VERIFIER_HOME/ides"
$GRADLE_CMD verifyPlugin -Dplugin.verifier.home.dir="$PLUGIN_VERIFIER_HOME"

# get back to the script folder
popd

echo "✅ All done."
