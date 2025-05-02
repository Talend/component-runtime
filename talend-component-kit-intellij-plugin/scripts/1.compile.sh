#!/bin/bash

set -x

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier"
GRADLE_CMD="../gradlew"
ARTIFACTS_DIR="../build/distributions"
# ---------------------------------

# --------------------------------------------
echo "1. Check Java & Gradle (assuming Java and Gradle are pre-installed)"
# --------------------------------------------
echo "Using Java version:" && java -version
echo "Using Gradle version:" && ./gradlew --version

echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH="$JAVA_HOME/bin:$PATH"


# --------------------------------------------
echo "2. Running 'gradlew properties' to get version"
# --------------------------------------------

PROPERTIES=$($GRADLE_CMD properties --console=plain -q)
VERSION=$(echo "$PROPERTIES" | grep "^version:" | cut -f2- -d ' ')
echo "Version: $VERSION"

# --------------------------------------------
echo "2. Get info"
# --------------------------------------------
==> Building plugin"
$GRADLE_CMD buildPlugin

echo "✅ All done."
