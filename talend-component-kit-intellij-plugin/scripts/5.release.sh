#!/bin/bash
#
#  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
set -euo pipefail

# --------------------------------------------
# Configuration
# --------------------------------------------
GRADLE_CMD="gradle"
PUBLISH_TOKEN="${JETBRAINS_TOKEN:?Missing PUBLISH_TOKEN}"

# --------------------------------------------
# 1. Publish plugin to JetBrains Marketplace
# --------------------------------------------
echo "Publishing plugin to JetBrains Marketplace..."
export PUBLISH_TOKEN="$PUBLISH_TOKEN"
$GRADLE_CMD publishPlugin

echo "✅ All done."