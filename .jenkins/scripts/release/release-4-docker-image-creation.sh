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

# shellcheck source=release-git-common.sh
source "$(dirname "${BASH_SOURCE[0]}")/release-git-common.sh"

# Parameters:
# $1: release version
# $2: tag name
# $3: branch name
#
# Environment:
# DRY_RUN: when "true":
#          - the branch push is simulated with "git push --dry-run" (no ref is actually updated).
#            Note: this only catches repository-level access issues; it does NOT reliably reproduce
#            a server-side rejection of a specific ref (e.g. branch protection), since that only
#            happens during the real object transfer, which --dry-run skips.
#          - the release tag checkout is skipped, since under DRY_RUN no tag was actually created by
#            release:prepare (it also runs with -DdryRun=true): the docker image is instead built from
#            the current workspace state (still on $branchName, poms still at the SNAPSHOT version).
#          - the docker image is built to a local tar file instead of being pushed to the registry
#            (see docker_build.sh).
#          Defaults to false.
main() {
  local releaseVersion="${1?Missing release version}"
  local tagName="${2?Missing actual project version}"
  local branchName="${3?Missing actual project version}"

  # Branch and tag are pushed together, atomically: either the remote accepts both or none.
  # Under DRY_RUN no tag exists (release:prepare ran with -DdryRun=true), so only the branch is named.
  local pushParams=(--atomic)
  local pushRefs=("${branchName}")
  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    printf ">> DRY RUN: push to github will be simulated with --dry-run\n"
    pushParams+=(--dry-run)
  else
    pushRefs+=("${tagName}")
  fi

  printf ">> Docker image creation from branch %s with tag: %s on version %s\n" "$releaseVersion" "$tagName" "$branchName"
  printf "Reset repo\n"
  git reset --hard

  # Fail-closed (read-only, so also run under DRY_RUN): abort before any push if the remote branch
  # cannot be fetched or moved during the release.
  assert_remote_branch_not_ahead "${branchName}" || exit 1
  git push "${pushParams[@]}" -u origin "${pushRefs[@]}"

  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    printf ">> DRY RUN: skipping checkout of tag %s (nothing was tagged under DRY_RUN)\n" "${tagName}"
  else
    printf "Checkout the release tag as a branch\n"
    git checkout -b "${tagName}" "${tagName}"
  fi

  printf "Activate lastest tagging for master branch\n"
  local tag_latest=""
  if [[ ${branchName} == 'master' ]]; then
    tag_latest="true"
  fi

  printf "Docker build call\n"
  bash .jenkins/scripts/docker_build.sh "${releaseVersion}" "${tag_latest}"
}

main "$@"
