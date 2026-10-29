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

# Create a changelog on github.
# $1: repository name on github
# $2: from this version calculate a diff
# $3: to this version calculate a diff
# $4: flag do we need to create draft changelog on github
# $5: branch name
# $6: github login
# $7: github token
main() (
    repository="${1:?Missing repository name}"
    fromVersion="${2:?Missing from version}"
    toVersion="${3:?Missing to version}"
    draft="${4:?Missing flag draft changelog}"
    branchName="${5:?Missing branch name}"
    githubLogin="${6:?Missing github login}"
    githubToken="${7:?Missing github token}"

    printf "Versions %s...%s\n" "${fromVersion}" "${toVersion}"

    printf "Fetching all tags.\n"
    #Too many unnecessary logged info
    git fetch --tags --quiet --force

    # NOTE. we run the script after the release is done, so current release tag should exist
    printf "Getting last commit hash.\n"
    # pre check, to avoid application execution if we don't find the commit
    lastCommitHash=$(
          git log --format="%H" "release/${fromVersion}"..."release/${toVersion}" \
          | head --lines=-1 | tail --lines=1)

    if [[ -z "${lastCommitHash}" ]]; then
        printf "Cannot evaluate last commit hash. Changelog won't be generated.\n"
        exit 1
    else
        printf "Last commit hash - %s\n" "${lastCommitHash}"
        printf "Draft - %s\n" "${draft}"

        repoDirectory=$(pwd)
        git -C .. clone https://github.com/Talend/connectivity-tools.git && \
        mvn --file ../connectivity-tools/release-notes/pom.xml clean package -DskipTests

        # first find the jar file in the target folder
        # then call java application with env variables
        REPOSITORY="${repository}" RELEASE_VERSION="${toVersion}" DRAFT="${draft}" \
            BRANCH_NAME="${branchName}" GITHUB_LOGIN="${githubLogin}" GITHUB_TOKEN="${githubToken}" \
            java -jar "$(find ../connectivity-tools/release-notes/target -maxdepth 1 -name "*.jar")" \
            "${repoDirectory}" "release/${fromVersion}" "release/${toVersion}"
    fi
)

main "$@"
