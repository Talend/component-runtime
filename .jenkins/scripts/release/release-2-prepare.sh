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
# $1: release version
# $2: next version
# $3: tag name
# $4: extra build args for all mvn cmd
main() {
  local releaseVersion="${1?Missing release version}"; shift
  local nextVersion="${1?Missing actual project version}"; shift
  local tagName="${1?Missing actual project version}"; shift
  local extraBuildParams=("$@")

  printf ">> Maven prepare release %s (next-dev: %s; tag: %s)\n" "${releaseVersion}" "${nextVersion}" "${tagName}"
  mvn release:prepare \
    --batch-mode \
    --errors \
    --define tag="${tagName}" \
    --define releaseVersion="${releaseVersion}" \
    --define developmentVersion="${nextVersion}" \
    --define arguments="-DskipTests -DskipITs -Dcheckstyle.skip -Denforcer.skip=true -Drat.skip" \
    --activate-profiles ossrh,release,gpg2 \
    --settings .jenkins/settings.xml \
    "${extraBuildParams[@]}"
}

main "$@"
