#!/usr/bin/env bash

KAFKA_VERSION=1.1.0
SERVER_VERSION=$(grep "<version>" pom.xml  | head -n 1 | sed "s/.*>\\(.*\\)<.*/\\1/")
DOCKER_IMAGE_VERSION=${DOCKER_IMAGE_VERSION:-$SERVER_VERSION}
# if snapshot use the date as version
if [[ "$DOCKER_IMAGE_VERSION" = *"SNAPSHOT" ]]; then
    DOCKER_IMAGE_VERSION=$(echo $SERVER_VERSION | sed "s/-SNAPSHOT//")_$(date +%Y%m%d%H%M%S)
fi
IMAGE=$(echo "${DOCKER_LOGIN:-tacokit}/component-server:$DOCKER_IMAGE_VERSION")
DOCKER_TMP_DIR="$(pwd)/target/docker_workdir"

echo "Prebuilding the project"
if [ "x${COMPONENT_SERVER_DOCKER_BUILD_ONLY}" != "xtrue" ]; then
    mvn clean install -pl component-server-parent/component-server \
        -am -T2C -e $DEPLOY_OPTS \
        -Dmaven.ext.class.path=/tmp/maven-travis-output-1.0.0.jar
else
    echo "Assuming build is done as requested through \$COMPONENT_SERVER_DOCKER_BUILD_ONLY"
fi

# if we don't set up a custom buildcontext dir the whole project is taken as buildcontext and it makes gigs!
echo "Setting up buildcontext"
mkdir -p "$DOCKER_TMP_DIR"
cp -v component-runtime-beam/target/component-runtime-beam-${SERVER_VERSION}-dependencies.zip $DOCKER_TMP_DIR/beam.zip
cp -v component-server-parent/component-server/target/component-server-meecrowave-distribution.zip $DOCKER_TMP_DIR/server.zip
cp -v Dockerfile $DOCKER_TMP_DIR/Dockerfile
cp -v -r .docker/conf $DOCKER_TMP_DIR/conf
cp -v -r .docker/bin $DOCKER_TMP_DIR/bin
cd $DOCKER_TMP_DIR

echo "Grabbing kafka client"
mvn dependency:copy -Dartifact=org.apache.kafka:kafka-clients:$KAFKA_VERSION -DoutputDirectory=.

echo "Building image >$IMAGE<"
docker build --tag "$IMAGE" \
  --build-arg SERVER_VERSION=$SERVER_VERSION \
  --build-arg DOCKER_IMAGE_VERSION=$DOCKER_IMAGE_VERSION \
  --build-arg KAFKA_CLIENT_VERSION=$KAFKA_VERSION \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg GIT_URL=$(git config --get remote.origin.url) \
  --build-arg GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD) \
  --build-arg GIT_REF=$(git rev-parse HEAD) . && \
docker tag "$IMAGE" "${DOCKER_REGISTRY:-docker.io}/$IMAGE" || exit 1

if [ "x${COMPONENT_SERVER_DOCKER_BUILD_ONLY}" != "xtrue" ]; then
    echo "Pushing the tag $IMAGE"
    # retry cause if the server has a bad time during the first push
    echo "$DOCKER_PASSWORD" | docker login "$DOCKER_REGISTRY" -u "$DOCKER_LOGIN" --password-stdin
    for i in {1..5}; do
        docker push "$DOCKER_REGISTRY/$IMAGE" && exit 0
    done
else
    echo "Not pushing the tag as request through \$COMPONENT_SERVER_DOCKER_BUILD_ONLY"
fi
