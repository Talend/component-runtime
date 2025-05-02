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
#2. Verify plugin
# --------------------------------------------

echo "==> Verifying plugin with IntelliJ Plugin Verifier"
mkdir -p "$PLUGIN_VERIFIER_HOME/ides"
$GRADLE_CMD verifyPlugin -Dplugin.verifier.home.dir="$PLUGIN_VERIFIER_HOME"

echo "✅ All done."
