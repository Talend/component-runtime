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
# Note: you can set the env variable MEECROWAVE_OPTS to customize the server,
#       by default it is installed in /opt/talend/component-kit and maven repository
#       is the default one of the machine, you can change it setting the system property
#       talend.component.server.maven.repository=/path/to/your/m2
#

#
# Sample build command:
# $ docker build --build-arg SERVER_VERSION=1.0.0-SNAPSHOT --tag talend-component-kit:1.0.0-snapshot .
#
# Note: for another "server" of the project you can also set ARTIFACT_ID=xxx, for instance:
# $ docker build --build-arg SERVER_VERSION=1.0.0-SNAPSHOT ARTIFACT_ID=component-starter-server --tag talend-component-kit-starter:1.0.0-snapshot .
#

# Sample run command once built (assuming previous command name)
# $ docker run -p 8080:8080 talend-component-kit:1.0.0-snapshot
#

# In practise and during development you will want to use this kind of command to see the logs without connecting to the server:
# $ docker run -p 8080:8080 -e CONSOLE_LOG_LEVEL=INFO registry.datapwn.com/talend-component-kit:1.0.0-snapshot
#
# And if you want to deploy some components you can configure which ones in MEECROWAVE_OPTS (see server doc online)
# and redirect your local m2:
# $ docker run \
#       -p 8080:8080 \
#       -v ~/.m2:/root/.m2 \
#       -e CONSOLE_LOG_LEVEL=INFO \
#       -e MEECROWAVE_OPTS="-Dtalend.component.server.component.coordinates=g:a:v,g2:a2:v2,..." \
#       talend-component-kit:<tag>
#

# not used cause of licensing
# FROM anapsix/alpine-java:8_server-jre_unlimited
FROM openjdk:8-jre-alpine

MAINTAINER tacokit@talend.com

ENV LC_ALL en_US.UTF-8

RUN set -xe && \
    apk add --no-cache gnupg ca-certificates openssl && \
    update-ca-certificates && \
    gpg --keyserver ha.pool.sks-keyservers.net --recv-keys E16448E7EC79DD12245C4ADFFA5FA52B5B7B42F0 && \
    gpg --keyserver ha.pool.sks-keyservers.net --recv-keys CF80A055A2AD28E9EFBF942A73129F58DE61ECBD

ENV MEECROWAVE_BASE /opt/talend/component-kit
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

ARG SERVER_VERSION
RUN set -ex && \
    [ -n "$SERVER_VERSION" ] || { echo "[ERROR] No version defined, set environment variable SERVER_VERSION when launching the image"; exit 1; } && \
    export NEXUS_BASE=${NEXUS_BASE:-https://oss.sonatype.org} && \
    export GROUP_ID=${GROUP_ID:-org.talend.sdk.component} && \
    export ARTIFACT_ID=${ARTIFACT_ID:-component-server} && \
    export REPOSITORY=$([[ "${SERVER_VERSION%-SNAPSHOT}" != "$SERVER_VERSION" ]] && echo 'snapshots' || echo 'releases') && \
    export DOWNLOAD_URL="$NEXUS_BASE/service/local/artifact/maven/content?r=$REPOSITORY&g=$GROUP_ID&a=$ARTIFACT_ID&v=$SERVER_VERSION&e=zip" && \
    echo "Using artifact $GROUP_ID:$ARTIFACT_ID:zip:$SERVER_VERSION" && \
    wget $DOWNLOAD_URL.asc -O $ARTIFACT_ID.zip.asc && \
    wget $DOWNLOAD_URL -O $ARTIFACT_ID.zip && \
    gpg --batch --verify $ARTIFACT_ID.zip.asc $ARTIFACT_ID.zip && \
    unzip $ARTIFACT_ID.zip && \
    mv $ARTIFACT_ID-distribution/* $MEECROWAVE_BASE && \
    rm -Rf $ARTIFACT_ID-distribution && \
    rm $ARTIFACT_ID.zip* && \
    echo "$GROUP_ID:$ARTIFACT_ID:zip:$SERVER_VERSION" > conf/build.gav && \
    date > conf/build.date
COPY conf/* $MEECROWAVE_BASE/conf/
COPY bin/* $MEECROWAVE_BASE/bin/
RUN chmod +x bin/*.sh

EXPOSE 8080
CMD [ "./bin/meecrowave.sh", "run" ]
