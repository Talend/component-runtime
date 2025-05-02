#!/bin/bash
set -euo pipefail

# --------------------------------------------
# Configuration
# --------------------------------------------

RELEASE_TAG="${GITHUB_REF_NAME:-}"  # Expected: v1.2.3
PUBLISH_TOKEN="${PUBLISH_TOKEN:?Missing PUBLISH_TOKEN}"

# --------------------------------------------
# 1. Checkout release tag
# --------------------------------------------
git fetch --tags
git checkout "$RELEASE_TAG"

# --------------------------------------------
# 2. CHeck Java & Gradle (assuming Java and Gradle are pre-installed)
# --------------------------------------------
echo "Using Java version:"
java -version

echo "Using Gradle version:"
./gradlew --version

# --------------------------------------------
# 3. Extract changelog from release body
# --------------------------------------------
CHANGELOG="$(echo "$RELEASE_BODY" | sed -e 's/^[[:space:]]*$//g' -e '/./,$!d')"

# --------------------------------------------
# 4. Patch Changelog
# --------------------------------------------
if [[ -n "$CHANGELOG" ]]; then
  echo "Patching changelog..."
  ./gradlew patchChangelog --release-note="$CHANGELOG"
fi

# --------------------------------------------
# 5. Publish plugin to JetBrains Marketplace
# --------------------------------------------
echo "Publishing plugin to JetBrains Marketplace..."
./gradlew publishPlugin \
  -PpublishToken="$PUBLISH_TOKEN"

echo "✅ All done."