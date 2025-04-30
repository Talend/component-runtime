#!/bin/bash

set -euo pipefail

# --------- CONFIGURATION ---------
JAVA_VERSION=21
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier"
GRADLE_CMD="./gradlew"
ARTIFACTS_DIR="./build/distributions"
# ---------------------------------

# --------------------------------------------
# 2. Setup Java & Gradle (assuming Java and Gradle are pre-installed)
# --------------------------------------------
# You can use SDKMAN, asdf, or pre-install Java 21 via system package manager

echo "Using Java version:"
java -version

echo "Using Gradle version:"
./gradlew --version


echo "==> Setting up environment"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> Running 'gradlew properties' to get version"
PROPERTIES=$($GRADLE_CMD properties --console=plain -q)
VERSION=$(echo "$PROPERTIES" | grep "^version:" | cut -f2- -d ' ')
echo "Version: $VERSION"

echo "==> Getting changelog"
CHANGELOG=$($GRADLE_CMD getChangelog --unreleased --no-header --console=plain -q)
echo -e "Changelog:\n$CHANGELOG"

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

echo "==> Creating GitHub Release Draft"
# Assumes `gh auth login` has been done
DRAFT_ID=$(gh release list --limit 100 --json isDraft,id | jq '.[] | select(.isDraft) | .id' -r)
for ID in $DRAFT_ID; do
  gh api -X DELETE "/repos/${GITHUB_REPOSITORY}/releases/${ID}" || true
done

gh release create "v$VERSION" \
  --draft \
  --title "v$VERSION" \
  --notes "$CHANGELOG"

echo "✅ All done."
