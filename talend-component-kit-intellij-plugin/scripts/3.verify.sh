#!/bin/bash

set -euo pipefail

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier" # this one should be in EFS
GRADLE_CMD="./gradlew"

# ---------------------------------
pushd ..

echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))


# --------------------------------------------
#2. Verify plugin
# --------------------------------------------

echo "==> Verifying plugin with IntelliJ Plugin Verifier"
mkdir -p "$PLUGIN_VERIFIER_HOME/ides"
$GRADLE_CMD verifyPlugin -Dplugin.verifier.home.dir="$PLUGIN_VERIFIER_HOME"

popd

echo "✅ All done."
