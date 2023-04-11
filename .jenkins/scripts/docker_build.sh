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
# $1: docker image name
# $2: docker tag version
# $3: should tag as latest

_IMAGE="${1?Missing image name}"
_TAG="${2?Missing tag}"
_LATEST="${3:-false}"

echo ">> Building $_IMAGE:${_TAG}"

mvn package jib:build@build \
  --file "images/${_IMAGE}-image/pom.xml" \
  --define docker.talend.image.tag="${_TAG}"

if [[ ${_LATEST} == 'true' ]]; then
  mvn package jib:build@build \
  --file "images/${_IMAGE}-image/pom.xml" \
  --define docker.talend.image.tag=latest
fi

