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
# $1: docker tag version
# $2: should tag as latest (true/false) default is false
# $3: requested image, if not given, all will be pushed
#
# Environment:
# DRY_RUN: when "true", images are built to a local tar file ("jib:buildTar") instead of being built
#          and pushed to the registry ("jib:build"). This lets the whole stage be exercised (including
#          the Maven/Jib configuration) without needing registry credentials or a daemon, and without
#          publishing anything. Defaults to false.

_TAG="${1?Missing tag}"
_IS_LATEST="${2-false}"
_ONLY_ONE_IMAGE="${3}"

_JIB_GOAL="jib:build@build"
if [[ "${DRY_RUN:-false}" == "true" ]]; then
  printf ">> DRY RUN: images will be built to a local tar file instead of being pushed\n"
  _JIB_GOAL="jib:buildTar@build"
fi

dockerBuild() {
  _IMAGE="${1}"
  printf ">> Building and push %s:%s\n" "{$_IMAGE}" "${_TAG}"
  if [[ ${_IS_LATEST} == 'true' ]]; then
    printf ">>The image will be tagged as LATEST\n"
  fi

  local skip_for_docker_build="-DskipTests -DskipITs -Dcheckstyle.skip -Denforcer.skip=true -Drat.skip -Dspotless.skip=true"

  mvn package "${_JIB_GOAL}" \
    --file "images/${_IMAGE}-image/pom.xml" \
    --define docker.talend.image.tag="${_TAG}" \
    $skip_for_docker_build

  if [[ ${_IS_LATEST} == 'true' ]]; then
    mvn package "${_JIB_GOAL}" \
    --file "images/${_IMAGE}-image/pom.xml" \
    --define docker.talend.image.tag=latest \
    $skip_for_docker_build
  fi
}

if [[ -n "${_ONLY_ONE_IMAGE}" ]]; then
  dockerBuild "${_ONLY_ONE_IMAGE}"
else
  dockerBuild "component-server"
  dockerBuild "component-starter-server"
  dockerBuild "remote-engine-customizer"
fi

