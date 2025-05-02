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
echo "2. Preparing plugin artifact"
# --------------------------------------------

echo "==> "
cd "$ARTIFACTS_DIR"
FILENAME=$(ls *.zip)
unzip "$FILENAME" -d content
PLUGIN_DIR="${FILENAME%.zip}"

echo "✅ All done."
