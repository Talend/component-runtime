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

# For usage instruction and details, please see the README.adoc file from the same folder

# not used cause of licensing
# FROM anapsix/alpine-java:8_server-jre_unlimited
FROM openjdk:8-jre-alpine

ARG SERVER_VERSION
ARG ARTIFACT_ID=component-starter-server
ARG CENTRAL=http://repo.apache.maven.org/maven2

MAINTAINER tacokit@talend.com

ENV LC_ALL en_US.UTF-8

RUN set -xe && \
    apk add --no-cache gnupg ca-certificates openssl && \
    update-ca-certificates && \
    for key in E16448E7EC79DD12245C4ADFFA5FA52B5B7B42F0 CF80A055A2AD28E9EFBF942A73129F58DE61ECBD; do \
        gpg --keyserver keyserver.ubuntu.com --recv-keys $key || \
        gpg --keyserver ha.pool.sks-keyservers.net --recv-keys $key || \
        gpg --keyserver pgp.mit.edu --recv-keys $key || \
        gpg --keyserver keyserver.pgp.com --recv-keys $key; \
    done

ENV MEECROWAVE_BASE /opt/talend/$ARTIFACT_ID
RUN mkdir -p $MEECROWAVE_BASE
WORKDIR $MEECROWAVE_BASE

RUN date

RUN set -ex && \
    echo "Preparing $ARTIFACT_ID v$SERVER_VERSION" && \
    [ -n "$SERVER_VERSION" ] || { echo "[ERROR] No version defined, set environment variable SERVER_VERSION when launching the image"; exit 1; } && \
    export NEXUS_BASE=${NEXUS_BASE:-https://oss.sonatype.org} && \
    export GROUP_ID=${GROUP_ID:-org.talend.sdk.component} && \
    export REPOSITORY=$([[ "${SERVER_VERSION%-SNAPSHOT}" != "$SERVER_VERSION" ]] && echo 'snapshots' || echo 'releases') && \
    export DOWNLOAD_URL="$NEXUS_BASE/service/local/artifact/maven/content?r=$REPOSITORY&g=$GROUP_ID&a=$ARTIFACT_ID&v=$SERVER_VERSION&e=zip" && \
    echo "Using artifact $GROUP_ID:$ARTIFACT_ID:zip:$SERVER_VERSION" && \
    wget $DOWNLOAD_URL.asc -O $ARTIFACT_ID.zip.asc && \
    wget $DOWNLOAD_URL -O $ARTIFACT_ID.zip && \
    gpg --batch --verify $ARTIFACT_ID.zip.asc $ARTIFACT_ID.zip && \
    unzip $ARTIFACT_ID.zip && mv $ARTIFACT_ID-distribution/* $MEECROWAVE_BASE && \
    rm -Rf $ARTIFACT_ID-distribution $ARTIFACT_ID.zip* $MEECROWAVE_BASE/conf/log4j2.xml && \
    echo "$GROUP_ID:$ARTIFACT_ID:zip:$SERVER_VERSION" > conf/build.gav && \
    date > conf/build.date

COPY conf/log4j2-$ARTIFACT_ID-*.xml $MEECROWAVE_BASE/conf/
COPY conf/meecrowave.properties $MEECROWAVE_BASE/conf/meecrowave.properties
COPY bin/* $MEECROWAVE_BASE/bin/
RUN set -ex && sed -i "s/artifactId/$ARTIFACT_ID/" $MEECROWAVE_BASE/bin/setenv.sh
RUN chmod +x bin/*.sh

EXPOSE 8080
CMD [ "./bin/meecrowave.sh", "run" ]
