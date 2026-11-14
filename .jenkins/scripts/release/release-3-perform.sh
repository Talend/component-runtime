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
# $1: extra build args for all mvn cmd
main() {
  local extraBuildParams=("$@")

  printf ">> Maven perform release"

  local release_profiles="release,ossrh,gpg2"

  mvn release:perform \
    --batch-mode \
    --errors \
    --define arguments="-DskipTests -DskipITs -Dinvoker.skip=true -Dcheckstyle.skip -Denforcer.skip=true -Drat.skip" \
    --settings .jenkins/settings.xml \
    --activate-profiles "$release_profiles" \
    "${extraBuildParams[@]}"

  # Release artifacts are also published on the packages of this repository (QTDI-3791).
  # Studio modules are released by release-3.1-perform-studio.sh. Non-blocking while Sonatype stays the reference.
  (
    cd target/checkout
    mvn deploy \
      --batch-mode \
      --errors \
      --define skipTests=true \
      --define skipITs=true \
      --define invoker.skip=true \
      --define checkstyle.skip=true \
      --define enforcer.skip=true \
      --define rat.skip=true \
      --define altDeploymentRepository=github_packages_release::default::https://maven.pkg.github.com/Talend/component-runtime \
      --projects '!:component-studio,!:component-runtime-di' \
      --fail-at-end \
      --settings "${OLDPWD}/.jenkins/settings.xml" \
      --activate-profiles "release-github-packages,gpg2" \
      "${extraBuildParams[@]}"
  ) || echo "WARN: GitHub Packages deploy failed (non-blocking)"
}

main "$@"
