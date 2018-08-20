#!/usr/bin/env bash

#
#  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

SERVER_VERSION=$(grep "<version>" pom.xml  | head -n 1 | sed "s/.*>\\(.*\\)<.*/\\1/")
DOCKER_IMAGE_VERSION=${DOCKER_IMAGE_VERSION:-$SERVER_VERSION}
if [[ "$DOCKER_IMAGE_VERSION" = *"SNAPSHOT" ]]; then
    DOCKER_IMAGE_VERSION=$(echo $SERVER_VERSION | sed "s/-SNAPSHOT//")_$(date +%Y%m%d%I%M%S)
fi
IMAGE=$(echo "talend/component-server-proxy-runtime:$DOCKER_IMAGE_VERSION")

TALEND_REGISTRY=${TALEND_REGISTRY:-registry.datapwn.com}

function visibleMessage() {
    echo ">>"
    echo ">>"
    echo ">>"
    echo ">> $1"
    echo ">>"
    echo ">>"
    echo ">>"
}

echo "Building image >$IMAGE<"

docker build --tag "$IMAGE" \
  --build-arg TALEND_REGISTRY=$TALEND_REGISTRY \
  --build-arg DOCKER_IMAGE_VERSION=$DOCKER_IMAGE_VERSION \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg GIT_URL=$(git config --get remote.origin.url) \
  --build-arg GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD) \
  --build-arg GIT_REF=$(git rev-parse HEAD) . && \
docker tag "$IMAGE" "$TALEND_REGISTRY/$IMAGE" || exit 1

visibleMessage "Built image >$IMAGE<"

if [ "x${COMPONENT_PROXY_SERVER_RUNTIME_DOCKER_BUILD}" == "xtrue" ]; then
    echo "Pushing the tag $IMAGE"
    # retry cause if the server has a bad time during the first push
    echo "$DOCKER_PASSWORD" | docker login "$TALEND_REGISTRY" -u "$DOCKER_LOGIN" --password-stdin
    for i in {1..5}; do
        docker push "$DOCKER_REGISTRY/$IMAGE" && exit 0
    done
else
    echo "Not pushing the tag as requested through \$COMPONENT_PROXY_SERVER_RUNTIME_DOCKER_BUILD"
fi
