#!/bin/bash

set -x

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier"
GRADLE_CMD="../gradlew"
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

echo "==> Running tests"
cd ../..
$GRADLE_CMD check || {
  echo "==> Tests failed - collecting results"
  cp -r ./build/reports/tests ./test-results
}

echo "✅ Running tests done."
