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

# --------- CONFIGURATION ---------
PLUGIN_VERIFIER_HOME="${HOME}/.pluginVerifier" # this one should be in EFS
GRADLE_CMD="./gradlew"


# --------------------------------------------
#2. Verify plugin
# --------------------------------------------

echo "==> Verifying plugin with IntelliJ Plugin Verifier"
mkdir -p "$PLUGIN_VERIFIER_HOME/ides"
$GRADLE_CMD verifyPlugin -Dplugin.verifier.home.dir="$PLUGIN_VERIFIER_HOME"

echo "✅ All done."
