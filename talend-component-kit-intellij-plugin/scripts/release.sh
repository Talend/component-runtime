#!/bin/bash
set -euo pipefail

# --------------------------------------------
# Configuration
# --------------------------------------------

RELEASE_TAG="${GITHUB_REF_NAME:-}"  # Expected: v1.2.3
RELEASE_BODY="${RELEASE_BODY:-}"   # Should be set to GitHub release body
PUBLISH_TOKEN="${PUBLISH_TOKEN:?Missing PUBLISH_TOKEN}"
CERTIFICATE_CHAIN="${CERTIFICATE_CHAIN:?Missing CERTIFICATE_CHAIN}"
PRIVATE_KEY="${PRIVATE_KEY:?Missing PRIVATE_KEY}"
PRIVATE_KEY_PASSWORD="${PRIVATE_KEY_PASSWORD:?Missing PRIVATE_KEY_PASSWORD}"

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
  -PpublishToken="$PUBLISH_TOKEN" \
  -PcertificateChain="$CERTIFICATE_CHAIN" \
  -PprivateKey="$PRIVATE_KEY" \
  -PprivateKeyPassword="$PRIVATE_KEY_PASSWORD"

echo "✅ All done."