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

#
# Build command:
# $ docker
#

FROM alpine:3.7 as staging

RUN date

ENV MEECROWAVE_BASE /opt/talend/component-server-proxy-runtime
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

ADD target/component-server-proxy-runtime-meecrowave-distribution.zip component-server-proxy-runtime-meecrowave-distribution.zip

RUN unzip component-server-proxy-runtime-meecrowave-distribution.zip && \
    mv component-server-proxy-runtime-distribution/* . && \
    rm -Rf component-server-proxy-runtime*-distribution

COPY log4j2.xml $MEECROWAVE_BASE/conf/
COPY meecrowave.properties $MEECROWAVE_BASE/conf/meecrowave.properties
COPY setenv.sh $MEECROWAVE_BASE/bin/setenv.sh

RUN set -ex && chmod +x bin/*.sh


FROM openjdk:8-jre-alpine

ARG BUILD_DATE
ARG GIT_URL
ARG GIT_BRANCH
ARG GIT_REF
ARG DOCKER_IMAGE_VERSION
ARG TALEND_REGISTRY

LABEL com.talend.maintainer="Talend <support@talend.com>" \
      com.talend.build-date="$BUILD_DATE" \
      com.talend.git.repositories="$GIT_URL" \
      com.talend.git.branches="$GIT_BRANCH" \
      com.talend.git.commits="$GIT_REF" \
      com.talend.name="Talend Component Kit Server Proxy Runtime" \
      com.talend.application="component-server-proxy-runtime" \
      com.talend.service="component-server" \
      com.talend.description="Talend Component Kit Server Proxy Runtime" \
      com.talend.url="https://www.talend.com" \
      com.talend.vendor="Talend" \
      com.talend.version="$DOCKER_IMAGE_VERSION" \
      com.talend.docker.cmd="docker run -d -p 8080:8080 $TALEND_REGISTRY/talend/component-server-proxy-runtime:$DOCKER_IMAGE_VERSION" \
      com.talend.docker.params="MEECROWAVE_OPTS=<JVM options>" \
      com.talend.docker.healthcheck="<none>"

ENV LC_ALL en_US.UTF-8

ENV MEECROWAVE_BASE /opt/talend/component-server-proxy-runtime
ENV CONNECTOR_REPOSITORY /opt/talend/connectors-se
RUN mkdir -p $MEECROWAVE_BASE $CONNECTOR_REPOSITORY

COPY --from=registry.datapwn.com/talend/connectors-se:1.0.0_20180817023752 $CONNECTOR_REPOSITORY $CONNECTOR_REPOSITORY
COPY --from=staging $MEECROWAVE_BASE $MEECROWAVE_BASE

WORKDIR $MEECROWAVE_BASE
EXPOSE 8080
CMD [ "./bin/meecrowave.sh", "run" ]
