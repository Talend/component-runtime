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

# Parameters:
# $1: docker tag version
# $2: docker registry to push on

_TAG="${1?Missing tag}"
_REGISTRY="${2?Missing registry}"

dockerPush() {
  _IMAGE="${1}"
  echo ">> Building and pushing ${_IMAGE}:${_TAG} to ${_REGISTRY}"
  cd "images/${_IMAGE}-image"

  # TODO check where is -Prelease in docker build
  # TODO use --file
  mvn -DskipTests -Dinvoker.skip=true -T1C \
      clean install \
      jib:build@build \
      --define image.currentVersion=${_TAG} \
      --define talend.server.image.registry=${_REGISTRY}
#      --define talend.server.image.registry=registry.hub.docker.com/
  cd ../..
}

dockerPush "component-server"
dockerPush "component-starter-server"
# TODO see if needed dockerPush "remote-engine-customizer"
