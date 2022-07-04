#!/usr/bin/env bash
#
#  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

setMavenVersion() (
  version="$1"
  mvn 'versions:set' \
    --batch-mode \
    --define "newVersion=${version}"
)

setMavenProperty() (
  propertyName="$1"
  propertyValue="$2"
  mvn 'versions:set-property' \
    --batch-mode \
    --define "property=${propertyName}" \
    --define "newVersion=${propertyValue}"
)

# Change pom versions
main() (
  element='component-runtime.version'
  version="${1:?Missing version}"
  connectorPath="${2:?Missing connector path}" # TODO optional, use mvn -f?

  if [ "default" = "${version}" ]; then
    echo "No version change in the pom, keep the default one"
  else
    echo "Change version in the pom ${element} to ${version} in ${connectorPath}"
    cd "${connectorPath}"
    pwd
    setMavenProperty "${element}" "${version}"
    # setMavenProperty "component-runtime.version" "1.45.0-SNAPSHOT"
  fi
)

main "$@"
