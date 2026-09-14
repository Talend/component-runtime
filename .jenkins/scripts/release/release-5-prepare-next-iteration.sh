#!/usr/bin/env bash
#
#  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
#
# Environment:
# DRY_RUN: when "true", the final push is simulated with "git push --dry-run" without actually
#          updating the remote. Note: this only catches repository-level access issues (e.g. no
#          access at all to the repo); it does NOT reliably reproduce a server-side rejection of a
#          specific ref (e.g. branch protection, restricted credentials) since that only happens
#          during the real object transfer, which --dry-run skips. Defaults to false.
main() {
  local branchName="${1?Missing actual project version}"; shift
  local extraBuildParams=("$@")

  local pushDryRunParams=()
  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    printf ">> DRY RUN: push to github will be simulated with --dry-run\n"
    pushDryRunParams=(--dry-run)
  fi

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
  # "|| true" is here to avoid blocking the jenkins pipeline when there is nothing to commit
  git commit -a -m "Updating doc for next iteration" || true
  # NOT swallowing failures here: a rejected push (e.g. permission denied) must fail the stage
  git push "${pushDryRunParams[@]}" -u origin "${branchName}"

}

main "$@"
