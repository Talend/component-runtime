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
# $1: maintenance branch name
# $2: maintenance version
main() {
  local maintenanceBranch="${1?Missing maintenance branch name}"
  local maintenanceVersion="${2?Missing maintenance version}"

  printf ">> Creating %s with %s\n" "${maintenanceBranch}" "${maintenanceVersion}"

  git checkout -b "${maintenanceBranch}"

  printf "Downgrade project to maintenance version\n"
  mvn versions:set \
    --batch-mode \
    --settings .jenkins/settings.xml \
    --define generateBackupPoms=false \
    --define newVersion="${maintenanceVersion}"

  printf "Push maintenance branch\n"
  git commit -a -m "[jenkins-release] prepare for next development iteration ${maintenanceBranch}"
  git push -u origin "${maintenanceBranch}"

}

main "$@"
