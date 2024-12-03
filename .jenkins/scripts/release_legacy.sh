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
# $1: Branch
# $2: current version
# $3: extra build args for all mvn cmd
main() {
  local branch="${1?Missing branch}"
  local currentVersion="${2?Missing project version}"
  local extraBuildParams="${3}"
  local release="${currentVersion/-SNAPSHOT/}"
  local tag=component-runtime-"${release}"
  # bump
  local maj
  local min
  local rev
  local maintenance_branch
  maj=$(echo "${release}" | cut -d. -f1)
  min=$(echo "${release}" | cut -d. -f2)
  rev=$(echo "${release}" | cut -d. -f3)
  # variables according branch
  if [[ ${branch} == 'master' ]]; then
    maintenance_branch="maintenance/${maj}.${min}"
    maintenance_version="${maj}.${min}.$((rev + 1))-SNAPSHOT"
    min=$((min + 1))
    rev="0"
  else
    rev=$((rev + 1))
  fi
  local dev_version=${maj}.${min}.${rev}-SNAPSHOT
  ###
  echo ">> Maven prepare release $release (next-dev: ${dev_version}; maintenance: ${maintenance_branch} with ${maintenance_version})"
  mvn release:prepare \
    --batch-mode \
    --errors \
    --define tag="${tag}" \
    --define releaseVersion="${release}" \
    --define developmentVersion="${dev_version}" \
    --define arguments="-DskipTests -DskipITs" \
    --activate-profiles ossrh,release,gpg2 \
    ${extraBuildParams} \
    --settings .jenkins/settings.xml
  echo ">> Maven perform release $release"
  mvn release:perform \
    --batch-mode \
    --errors \
    --define arguments="-DskipTests -DskipITs" \
    --activate-profiles ossrh,release,gpg2 \
    ${extraBuildParams} \
    --settings .jenkins/settings.xml
  ###
  echo ">> Reset repo"
  git reset --hard
  git push -u origin "${branch}" --follow-tags
  echo ">> Checkout the release tag"
  git checkout -b "${tag}" "${tag}"
  ### docker build call
  local tag_latest=""
  if [[ ${branch} == 'master' ]]; then
    tag_latest="true"
  fi
  bash .jenkins/scripts/docker_build.sh "${release}" "${tag_latest}"
  ###
  echo ">> Rebuilding ${branch} and updating it (doc) for next iteration"
  git reset --hard
  git checkout "${branch}"
  mvn clean install -DskipTests -Dinvoker.skip=true ${extraBuildParams} || true
  git commit -a -m ">> Updating doc for next iteration" || true
  git push -u origin "${branch}" || true
  ###
  if [[ ${branch} == 'master' ]]; then
    echo ">> Creating ${maintenance_branch?Missing branch} with ${maintenance_version?Missing version}"
    git checkout -b "${maintenance_branch}"
    # downgrade to maintenance version
    mvn versions:set \
      --batch-mode \
      --settings .jenkins/settings.xml \
      --define generateBackupPoms=false \
      --define newVersion="${maintenance_version}"
    git commit -a -m "[jenkins-release] prepare for next development iteration ${maintenance_branch}"
    git push -u origin "${maintenance_branch}"
  fi
}

main "$@"
