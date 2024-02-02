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
# $1: branch name
# $2: extra build args for all mvn cmd
main() {
  local branchName="${1?Missing actual project version}"; shift
  local extraBuildParams=("$@")

  printf ">> Rebuilding %s and updating it (doc) for next iteration\n" "${branchName}"
  git reset --hard
  git checkout "${branchName}"

  # "|| true" is here to avoid blocking the jenkins pipeline in case of failure
  mvn clean install --define skipTests \
                    --define invoker.skip=true \
                    --define checkstyle.skip \
                    --define enforcer.skip=true \
                    --define rat.skip \
                    "${extraBuildParams[@]}" || true

  printf "Push to github\n"
  git commit -a -m "Updating doc for next iteration" || true
  git push -u origin "${branchName}" || true

}

main "$@"
