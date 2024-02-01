#!/usr/bin/env bash
#
#  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

set -xe

# Parameters:
# $1: release version
# $2: current version
main() {
  local releaseVersion="${1?Missing release version}"
  local currentVersion="${2?Missing actual project version}"

  printf ">> Preparing and installing BOM to release %s from %s\n" "${releaseVersion}" "${currentVersion}"

  printf "Install the BOM (SNAPSHOT)\n"
  mvn install --file bom/pom.xml

  printf "Set version to release value\n"
  mvn versions:set \
    --define newVersion="${releaseVersion}" \
    --define generateBackupPoms=false \
    --file bom/pom.xml

  printf "Install the BOM (FUTURE version)\n"
  mvn install --file bom/pom.xml

  printf "Set version back to currentVersion value\n"
  mvn versions:set \
    --define newVersion="${currentVersion}" \
    --define generateBackupPoms=false \
    --file bom/pom.xml
}

main "$@"
