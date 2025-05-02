#!/bin/bash
set -euo pipefail

# --------------------------------------------
# Configuration
# --------------------------------------------
GRADLE_CMD="./gradlew"
RELEASE_TAG="${GITHUB_REF_NAME:-}"  # Expected: v1.2.3
RELEASE_BODY="${RELEASE_BODY:-}"   # Should be set to GitHub release body
PUBLISH_TOKEN="${PUBLISH_TOKEN:?Missing PUBLISH_TOKEN}"

pushd ..

# --------------------------------------------
# 1. Checkout release tag
# --------------------------------------------
git fetch --tags
git checkout "$RELEASE_TAG"

# --------------------------------------------
# 3. Extract changelog from release body
# --------------------------------------------
CHANGELOG="$(echo "$RELEASE_BODY" | sed -e 's/^[[:space:]]*$//g' -e '/./,$!d')"

# --------------------------------------------
# 3. Patch Changelog
# --------------------------------------------
if [[ -n "$CHANGELOG" ]]; then
  echo "Patching changelog..."
  $GRADLE_CMD patchChangelog --release-note="$CHANGELOG"
fi

# --------------------------------------------
# 4. Publish plugin to JetBrains Marketplace
# --------------------------------------------
echo "Publishing plugin to JetBrains Marketplace..."
./gradlew publishPlugin \
  -PpublishToken="$PUBLISH_TOKEN"

popd

echo "✅ All done."