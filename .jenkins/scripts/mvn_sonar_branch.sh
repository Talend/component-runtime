#!/usr/bin/env bash
#
#  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

# Execute sonar analysis
# $1: Sonar analyzed branch
# $@: the extra parameters to be used in the maven commands
main() (
  branch="${1?Missing branch name}"; shift
  extraBuildParams=("$@")

  declare -a LIST_FILE_ARRAY=( $(find $(pwd) -type f -name 'jacoco.xml') )
  LIST_FILE=$(IFS=, ; echo "${LIST_FILE_ARRAY[*]}")

  # Why sonar plugin is not declared in pom.xml: https://blog.sonarsource.com/we-had-a-dream-mvn-sonarsonar
  # TODO https://jira.talendforge.org/browse/TDI-48980 (CI: Reactivate Sonar cache)
  mvn sonar:sonar \
      --define 'sonar.host.url=https://sonar-eks.datapwn.com' \
      --define "sonar.login=${SONAR_LOGIN}" \
      --define "sonar.password=${SONAR_PASSWORD}" \
      --define "sonar.branch.name=${branch}" \
      --define "sonar.coverage.jacoco.xmlReportPaths='${LIST_FILE}'" \
      --define "sonar.analysisCache.enabled=false" \
      --activate-profiles SONAR \
      "${extraBuildParams[@]}"
)

main "$@"
