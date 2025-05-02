#!/bin/bash

set -xeuo pipefail

pushd ..
# --------------------------------------------
echo "1. Check Java & Gradle (assuming Java and Gradle are pre-installed)"
# --------------------------------------------
echo "Using Java version:" && java -version
echo "Using Gradle version:" && ./gradlew --version

echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))


echo "Using Gradle version:"
$GRADLE_CMD --version

# --------------------------------------------
echo "2. Running 'gradlew properties' to get version"
# --------------------------------------------

PROPERTIES=$($GRADLE_CMD properties --console=plain -q)
VERSION=$(echo "$PROPERTIES" | grep "^version:" | cut -f2- -d ' ')
echo "Version: $VERSION"

popd

echo "✅ All done."