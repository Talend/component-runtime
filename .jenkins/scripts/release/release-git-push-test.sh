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

# Offline simulation of the release push done by release-4-docker-image-creation.sh,
# using a bare repository as "origin". Not wired into CI: run it manually.
#
#   .jenkins/scripts/release/release-git-push-test.sh

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_4="${SCRIPT_DIR}/release-4-docker-image-creation.sh"
ROOT="$(mktemp -d)"
trap 'rm -rf "${ROOT}"' EXIT

export GIT_AUTHOR_NAME=t GIT_AUTHOR_EMAIL=t@t GIT_COMMITTER_NAME=t GIT_COMMITTER_EMAIL=t@t
export GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_SYSTEM=/dev/null

failures=0
check() { # <description> <condition exit code>
  if [[ "$2" -eq 0 ]]; then echo "  ok   - $1"; else echo "  FAIL - $1"; failures=$((failures + 1)); fi
}
remote_has_tag() { git -C "$1" rev-parse -q --verify "refs/tags/$2" >/dev/null; }

# Creates origin.git and a work clone that looks like the state after release:prepare:
# one release commit on master, tagged locally, with a stubbed docker_build.sh.
setup() { # <case name>
  CASE="${ROOT}/$1"
  ORIGIN="${CASE}/origin.git"
  WORK="${CASE}/work"
  mkdir -p "${CASE}"
  git init -q --bare -b master "${ORIGIN}"
  git clone -q "${ORIGIN}" "${WORK}" 2>/dev/null
  (
    cd "${WORK}" || exit 1
    git checkout -q -b master
    mkdir -p .jenkins/scripts
    printf '#!/usr/bin/env bash\necho "docker build $*"\n' >.jenkins/scripts/docker_build.sh
    git add . && git commit -q -m "base" && git push -q -u origin master
    git commit -q --allow-empty -m "[maven-release-plugin] prepare release 1.0.0"
    git tag 1.0.0
    git commit -q --allow-empty -m "[maven-release-plugin] prepare for next development iteration"
  )
}
run_release() { (cd "${WORK}" && "$@" bash "${RELEASE_4}" 1.0.0 1.0.0 master >"${CASE}/out.log" 2>&1); }

echo "case 1: normal release, branch and tag both reach the remote"
setup normal
run_release env DRY_RUN=false
check "release exits 0" $?
check "remote tag 1.0.0 exists" "$(remote_has_tag "${ORIGIN}" 1.0.0 && echo 0 || echo 1)"
[[ "$(git -C "${ORIGIN}" rev-parse master)" == "$(git -C "${WORK}" rev-parse master)" ]]
check "remote master equals local master" $?

echo "case 2: remote branch moved during the release, aborted before any push"
setup moved
git clone -q "${ORIGIN}" "${CASE}/other" 2>/dev/null
git -C "${CASE}/other" commit -q --allow-empty -m "concurrent commit"
git -C "${CASE}/other" push -q origin master
before="$(git -C "${ORIGIN}" rev-parse master)"
run_release env DRY_RUN=false
check "release fails" "$([[ $? -ne 0 ]] && echo 0 || echo 1)"
check "remote tag not created" "$(remote_has_tag "${ORIGIN}" 1.0.0 && echo 1 || echo 0)"
[[ "$(git -C "${ORIGIN}" rev-parse master)" == "${before}" ]]
check "remote master untouched" $?
grep -q "moved during the release" "${CASE}/out.log"
check "explicit abort message" $?

echo "case 3: remote refuses the branch update, the tag does not land either"
setup refused
printf '#!/bin/sh\nwhile read old new ref; do [ "$ref" = refs/heads/master ] && exit 1; done\nexit 0\n' >"${ORIGIN}/hooks/pre-receive"
chmod +x "${ORIGIN}/hooks/pre-receive"
before="$(git -C "${ORIGIN}" rev-parse master)"
run_release env DRY_RUN=false
check "release fails" "$([[ $? -ne 0 ]] && echo 0 || echo 1)"
check "remote tag not created" "$(remote_has_tag "${ORIGIN}" 1.0.0 && echo 1 || echo 0)"
[[ "$(git -C "${ORIGIN}" rev-parse master)" == "${before}" ]]
check "remote master untouched" $?

echo "case 4: DRY_RUN=true updates no ref"
setup dry
before="$(git -C "${ORIGIN}" rev-parse master)"
run_release env DRY_RUN=true
check "release exits 0" $?
check "remote tag not created" "$(remote_has_tag "${ORIGIN}" 1.0.0 && echo 1 || echo 0)"
[[ "$(git -C "${ORIGIN}" rev-parse master)" == "${before}" ]]
check "remote master untouched" $?

echo "case 5: git fetch fails, aborted fail-closed"
setup nofetch
git -C "${WORK}" remote set-url origin "${CASE}/does-not-exist.git"
run_release env DRY_RUN=false
check "release fails" "$([[ $? -ne 0 ]] && echo 0 || echo 1)"
grep -q "cannot fetch" "${CASE}/out.log"
check "explicit abort message" $?
check "remote tag not created" "$(remote_has_tag "${ORIGIN}" 1.0.0 && echo 1 || echo 0)"

echo
if [[ "${failures}" -eq 0 ]]; then echo "ALL PASSED"; else echo "${failures} FAILED"; exit 1; fi
