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

  local release_profiles="release,gpg-keyring,gpg2"

  # Release artifacts are published on the packages of this repository (QTDI-3791), see the release profile.
  # Studio modules are released by release-3.1-perform-studio.sh and component-runtime-di is not deployable (system scope).
  mvn release:perform \
    --batch-mode \
    --errors \
    --define arguments="-DskipTests -DskipITs -Dinvoker.skip=true -Dcheckstyle.skip -Denforcer.skip=true -Drat.skip --projects !:component-studio,!:component-runtime-di --fail-at-end" \
    --settings .jenkins/settings.xml \
    --activate-profiles "$release_profiles" \
    "${extraBuildParams[@]}"
}

main "$@"
