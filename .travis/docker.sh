#!/usr/bin/env bash

export KAFKA_VERSION=1.1.0
export SERVER_VERSION=$(grep "<version>" pom.xml  | head -n 1 | sed "s/.*>\\(.*\\)<.*/\\1/")
export IMAGE=$(echo "talend/component-kit:$(echo $SERVER_VERSION | sed "s/-SNAPSHOT//")")

echo "Prebuilding the project"
mvn clean install -e -q $DEPLOY_OPTS

echo "Grabbing kafka client"
mvn dependency:copy -Dartifact=org.apache.kafka:kafka-clients:$KAFKA_VERSION -DoutputDirectory=.

if [[ "$SERVER_VERSION" = *"SNAPSHOT" ]]; then
  export IMAGE=$IMAGE-$(git rev-parse HEAD)
fi

echo "Building image $IMAGE"
docker build --tag "$IMAGE" --build-arg SERVER_VERSION=$SERVER_VERSION --build-arg KAFKA_CLIENT_VERSION=$KAFKA_VERSION . && \
docker tag "$IMAGE" "$TALEND_REGISTRY/$IMAGE" || exit 1

echo "Pushing the tag $IMAGE"
echo "$DOCKER_PASSWORD" | docker login "$TALEND_REGISTRY" -u "$DOCKER_LOGIN" --password-stdin
# retry cause if the server has a bad time during the first push
for i in {1..5}; do
    docker push "$TALEND_REGISTRY/$IMAGE" && exit 0
done
