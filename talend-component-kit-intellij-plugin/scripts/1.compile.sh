#!/bin/bash

set -xeuo pipefail

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier"
GRADLE_CMD="./gradlew"
ARTIFACTS_DIR="build/distributions"
# ---------------------------------

# go up into plugin root folder
pushd ..
# --------------------------------------------
echo "1. Build plugin"
# --------------------------------------------
echo "==> Building plugin"
$GRADLE_CMD buildPlugin

popd

echo "✅ All done."
