#!/bin/bash

set -xeuo pipefail

# --------- CONFIGURATION ---------
GRADLE_CMD="./gradlew"
# ---------------------------------

# go up into plugin root folder
pushd ..

echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))

# --------------------------------------------

echo "==> Running tests"
cd ../..
$GRADLE_CMD check || {
  echo "==> Tests failed - collecting results"
  cp -r ./build/reports/tests ./test-results
}

popd

echo "✅ Running tests done."
