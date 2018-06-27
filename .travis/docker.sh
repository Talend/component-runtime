#!/usr/bin/env bash

KAFKA_VERSION=1.1.0
SERVER_VERSION=$(grep "<version>" pom.xml  | head -n 1 | sed "s/.*>\\(.*\\)<.*/\\1/")
DOCKER_IMAGE_VERSION="$SERVER_VERSION"
# if snapshot use the date as version
if [[ "$DOCKER_IMAGE_VERSION" = *"SNAPSHOT" ]]; then
    DOCKER_IMAGE_VERSION=$(echo $SERVER_VERSION | sed "s/-SNAPSHOT//")_$(date +%Y%m%d%I%M%S)
fi
IMAGE=$(echo "$DOCKER_LOGIN/component-kit:$DOCKER_IMAGE_VERSION")
DOCKER_TMP_DIR=docker_workdir

# assumed set DOCKER_CONTENT_TRUST=1
# assumed set DOCKER_CONTENT_TRUST_SERVER="$DOCKER_REGISTRY"
# assumed set DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE=$DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE
# assumed set DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE=$DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE

echo "Prebuilding the project"
mvn clean install -e -q $DEPLOY_OPTS -T2C

# if we don't set up a custom buildcontext dir the whole project is taken as buildcontext and it makes gigs!
echo "Setting up buildcontext"
mkdir -p "$DOCKER_TMP_DIR"
cp component-runtime-beam/target/component-runtime-beam-${SERVER_VERSION}-dependencies.zip $DOCKER_TMP_DIR/beam.zip
cp component-server-parent/component-server/target/component-server-meecrowave-distribution.zip $DOCKER_TMP_DIR/server.zip
cp Dockerfile $DOCKER_TMP_DIR/Dockerfile
cp -r .docker/conf $DOCKER_TMP_DIR/conf
cp -r .docker/bin $DOCKER_TMP_DIR/bin
cd $DOCKER_TMP_DIR

echo "Grabbing kafka client"
mvn dependency:copy -Dartifact=org.apache.kafka:kafka-clients:$KAFKA_VERSION -DoutputDirectory=.

echo "Building image $IMAGE"
docker build --tag "$IMAGE" \
  --build-arg SERVER_VERSION=$SERVER_VERSION \
  --build-arg DOCKER_IMAGE_VERSION=$DOCKER_IMAGE_VERSION \
  --build-arg KAFKA_CLIENT_VERSION=$KAFKA_VERSION \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) . && \
docker tag "$IMAGE" "$DOCKER_REGISTRY/$IMAGE" || exit 1

echo "Pushing the tag $IMAGE"
# retry cause if the server has a bad time during the first push
echo "$DOCKER_PASSWORD" | docker login "$DOCKER_REGISTRY" -u "$DOCKER_LOGIN" --password-stdin
for i in {1..5}; do
    docker push "$DOCKER_REGISTRY/$IMAGE" && exit 0
done
