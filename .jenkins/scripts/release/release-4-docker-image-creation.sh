#!/usr/bin/env bash
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

set -xe

# Parameters:
# $1: release version
# $2: tag name
# $3: branch name
main() {
  local releaseVersion="${1?Missing release version}"
  local tagName="${2?Missing actual project version}"
  local branchName="${3?Missing actual project version}"

  printf ">> Docker image creation from branch %s with tag: %s on version %s\n" "$releaseVersion" "$tagName" "$branchName"
  printf "Reset repo\n"
  git reset --hard
  git push -u origin "${branchName}" --follow-tags

  printf "Checkout the release tag as a branch\n"
  git checkout -b "${tagName}" "${tagName}"

  printf "Activate lastest tagging for master branch\n"
  local tag_latest=""
  if [[ ${branchName} == 'master' ]]; then
    tag_latest="true"
  fi

  printf "Docker build call\n"
  bash .jenkins/scripts/docker_build.sh "${releaseVersion}" "${tag_latest}"
}

main "$@"
