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
FROM alpine:3.7 as stagingImage

ARG SERVER_VERSION
ARG KAFKA_CLIENT_VERSION
ARG GERONIMO_OPENTRACING_VERSION
ARG OPENTRACING_API_VERSION
ARG MICROPROFILE_OPENTRACING_API_VERSION


RUN date

ENV MEECROWAVE_BASE /opt/talend/component-kit
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

ADD kafka-clients-$KAFKA_CLIENT_VERSION.jar kafka.jar
ADD geronimo-opentracing-$GERONIMO_OPENTRACING_VERSION.jar geronimo-opentracing.jar
ADD opentracing-api-$OPENTRACING_API_VERSION.jar opentracing-api.jar
ADD microprofile-opentracing-api-$MICROPROFILE_OPENTRACING_API_VERSION.jar microprofile-opentracing-api.jar
ADD beam.zip beam.zip
ADD server.zip server.zip

RUN unzip server.zip && mv component-server-distribution/* . && rm -Rf component-server-distribution server.zip && \
    unzip beam.zip && mv component-runtime-beam-$SERVER_VERSION/* lib && rm -Rf component-runtime-beam-$SERVER_VERSION beam.zip && \
    mv kafka.jar lib/ && \
    mv geronimo-opentracing.jar lib/ && \
    mv opentracing-api.jar lib/ && \
    mv microprofile-opentracing-api.jar lib/

COPY conf/log4j2-component-server-*.xml $MEECROWAVE_BASE/conf/
COPY conf/meecrowave.properties $MEECROWAVE_BASE/conf/meecrowave.properties
COPY bin/* $MEECROWAVE_BASE/bin/

RUN set -ex && \
  sed -i "s/artifactId/component-server/" $MEECROWAVE_BASE/bin/setenv.sh && \
  chmod +x bin/*.sh && \
  rm $MEECROWAVE_BASE/conf/log4j2.xml


# not used cause of licensing
# FROM anapsix/alpine-java:8_server-jre_unlimited
FROM openjdk:8-jre-alpine

ARG SERVER_VERSION
ARG BUILD_DATE
ARG GIT_URL
ARG GIT_BRANCH
ARG GIT_REF
ARG DOCKER_IMAGE_VERSION

LABEL com.talend.maintainer="Talend <support@talend.com>" \
      com.talend.build-date="$BUILD_DATE" \
      com.talend.git.repositories="$GIT_URL" \
      com.talend.git.branches="$GIT_BRANCH" \
      com.talend.git.commits="$GIT_REF" \
      com.talend.name="Talend Component Kit Server" \
      com.talend.application="component-server" \
      com.talend.service="component-server" \
      com.talend.description="Talend Component Kit Backend Server" \
      com.talend.url="https://www.talend.com" \
      com.talend.vendor="Talend" \
      com.talend.version="$DOCKER_IMAGE_VERSION" \
      com.talend.docker.cmd="docker run -d -p 8080:8080 tacokit/component-kit:$DOCKER_IMAGE_VERSION" \
      com.talend.docker.params="MEECROWAVE_OPTS=<JVM options (system properties etc), ex: -Dtalend.component.server.component.registry=/path/to/component-registry.propertes -Dtalend.component.server.maven.repository=/path/to/m2> CONSOLE_LOG_LEVEL=<INFO, default to OFF. Allows to get console log on 'run'>" \
      com.talend.docker.healthcheck="curl --fail http://localhost:8080/api/v1/environment"

ENV LC_ALL en_US.UTF-8

ENV MEECROWAVE_BASE /opt/talend/component-kit
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

COPY --from=stagingImage $MEECROWAVE_BASE $MEECROWAVE_BASE

EXPOSE 8080
CMD [ "./bin/meecrowave.sh", "run" ]
