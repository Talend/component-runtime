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
# $1: docker tag version
# $2: should tag as latest (true/false) default is false
# $3: requested image, if not given, all will be pushed

_TAG="${1?Missing tag}"
_IS_LATEST="${2-false}"
_ONLY_ONE_IMAGE="${3}"

dockerBuild() {
  _IMAGE="${1}"
  printf ">> Building and push %s:%s\n" "{$_IMAGE}" "${_TAG}"
  if [[ ${_IS_LATEST} == 'true' ]]; then
    printf ">>The image will be tagged as LATEST\n"
  fi

  local skip_for_release="-DskipTests -DskipITs -Dcheckstyle.skip -Denforcer.skip=true -Drat.skip"

  mvn package jib:build@build \
    --file "images/${_IMAGE}-image/pom.xml" \
    --define docker.talend.image.tag="${_TAG}" \
    $skip_for_release

  if [[ ${_IS_LATEST} == 'true' ]]; then
    mvn package jib:build@build \
    --file "images/${_IMAGE}-image/pom.xml" \
    --define docker.talend.image.tag=latest \
    $skip_for_release
  fi
}

if [[ -n "${_ONLY_ONE_IMAGE}" ]]; then
  dockerBuild "${_ONLY_ONE_IMAGE}"
else
  dockerBuild "component-server"
  dockerBuild "component-starter-server"
  dockerBuild "remote-engine-customizer"
fi

