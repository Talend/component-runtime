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

RUN date

ENV MEECROWAVE_BASE /opt/talend/component-kit
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

ADD kafka-clients-$KAFKA_CLIENT_VERSION.jar kafka.jar
ADD component-runtime-beam/target/component-runtime-beam-1.0.1-SNAPSHOT-dependencies.zip beam.zip
ADD component-server-parent/component-server/target/component-server-meecrowave-distribution.zip server.zip

RUN unzip server.zip && mv component-server-distribution/* . && rm -Rf component-server-distribution server.zip && \
    unzip beam.zip && mv component-runtime-beam-$SERVER_VERSION/* . && rm -Rf component-runtime-beam-$SERVER_VERSION beam.zip && \
    mv kafka.jar lib/

COPY .docker/conf/log4j2-component-server-*.xml $MEECROWAVE_BASE/conf/
COPY .docker/conf/meecrowave.properties $MEECROWAVE_BASE/conf/meecrowave.properties
COPY .docker/bin/* $MEECROWAVE_BASE/bin/

RUN set -ex && sed -i "s/artifactId/component-server/" $MEECROWAVE_BASE/bin/setenv.sh && chmod +x bin/*.sh

# not used cause of licensing
# FROM anapsix/alpine-java:8_server-jre_unlimited
FROM openjdk:8-jre-alpine

ARG SERVER_VERSION
ARG KAFKA_CLIENT_VERSION

MAINTAINER tacokit@talend.com

ENV LC_ALL en_US.UTF-8

ENV MEECROWAVE_BASE /opt/talend/component-kit
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

COPY --from=stagingImage $MEECROWAVE_BASE $MEECROWAVE_BASE

EXPOSE 8080
CMD [ "./bin/meecrowave.sh", "run" ]
