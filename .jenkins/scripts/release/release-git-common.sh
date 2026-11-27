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

# Shared git helpers for the release scripts. This file is meant to be sourced, not executed.

# Fail-closed guard to run before pushing a release.
#
# Aborts (returns 1, nothing is pushed) when:
#  - the remote branch cannot be fetched (network, auth, missing branch...): the state of the remote
#    is unknown, so pushing is not safe;
#  - the remote branch contains commits that are not in the local HEAD (someone pushed while the
#    release was running): the push would be rejected or would need a manual analysis.
#
# $1: branch name (on origin)
assert_remote_branch_not_ahead() {
  local branch="${1?Missing branch name}"

  if ! git fetch origin "${branch}"; then
    printf >&2 "ERROR: cannot fetch origin/%s, the state of the remote is unknown. Aborting before any push.\n" "${branch}"
    return 1
  fi

  if ! git merge-base --is-ancestor FETCH_HEAD HEAD; then
    printf >&2 "ERROR: origin/%s moved during the release: it contains commits that are not in the local HEAD (remote %s, local %s).\n" \
      "${branch}" "$(git rev-parse --short FETCH_HEAD)" "$(git rev-parse --short HEAD)"
    printf >&2 "Aborting before any push. Nothing was pushed: re-run the release once the branch is stable.\n"
    return 1
  fi
}
