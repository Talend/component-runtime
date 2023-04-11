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
# $3: docker registry to push on

_IMAGE="${1?Missing image name}"
_TAG="${2?Missing tag}"
_REGISTRY="${3?Missing registry}"


echo ">> Pushing ${_IMAGE}:${_TAG} to ${_REGISTRY}"

mvn install jib:build@build \
    --file "images/${_IMAGE}-image/pom.xml" \
    --define image.currentVersion="${_TAG}" \
    --define talend.server.image.registry="${_REGISTRY}" \
    --activate-profiles release \
    -DskipTests -Dinvoker.skip=true -T1C

