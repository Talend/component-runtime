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
set -xeuo pipefail

# --------- CONFIGURATION ---------
GRADLE_CMD="gradle"
# ---------------------------------


echo "==> Running tests"

$GRADLE_CMD check || {
  echo "==> Tests failed - collecting results"
  cp -r ./build/reports/tests ./test-results
}

echo "✅ Running tests done."
