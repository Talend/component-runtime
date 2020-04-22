# Copyright (C) 2006-2020 Talend Inc. - www.talend.com
#  Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
FROM ${DOCKER_REGISTRY_HOST}${TSBI_IMAGE_PATH}/java8-svc-base:${TSBI_VERSION}

ARG DOCKER_REGISTRY_HOST
ARG TSBI_IMAGE_PATH
ARG TSBI_VERSION
#
ARG PROJECT_VERSION
ARG TIMESTAMP
#
ARG ADDITIONAL_FILES
ARG imageWorkDir=/opt/talend/component-kit
ARG boundPort=8080
#
ARG TALEND_JDK_SERIAL_FILTER
ARG LOGGING_LAYOUT

EXPOSE ${boundPort}

COPY ${ADDITIONAL_FILES}/* ${imageWorkDir}/

ENTRYPOINT ["java",
            "-Djdk.serialFilter=${TALEND_JDK_SERIAL_FILTER:-!*}",
            "-Djava.security.egd=file:/dev/./urandom",
            "-Djava.io.tmpdir=${imageWorkDir}/temp",
            "-Dmeecrowave.home=${imageWorkDir}",
            "-Dmeecrowave.base=${imageWorkDir}",
            "-Dlog4j.configurationFile=${imageWorkDir}/conf/log4j2-component-server-${env:LOGGING_LAYOUT}.xml",
            "-Dgeronimo.metrics.sigar.refreshInterval=0",
            "-Dtalend.component.exit-on-destroy=true",
            "-Dtalend.component.manager.services.cache.eviction.defaultEvictionTimeout=30_000",
            "-Dtalend.component.manager.services.cache.eviction.defaultMaxSize=5_000",
            "-Dtalend.component.manager.services.cache.eviction.maxDeletionPerEvictionRun=-1",
            "-cp",
            "${imageWorkDir}/custom/*:${imageWorkDir}/resources:${imageWorkDir}/classes:${imageWorkDir}/libs/*",
             "org.apache.meecrowave.runner.Cli",
             "--http=${boundPort}"
             ]

LABEL com.talend.name="Talend Component Kit Server"
LABEL com.talend.application="component-server"
LABEL com.talend.service="component-server"
LABEL com.talend.description="Talend Component Kit Backend Server"
LABEL com.talend.version="${PROJECT_VERSION}"
LABEL com.talend.build-date="${TIMESTAMP}"
LABEL com.talend.vendor="Talend"
LABEL com.talend.maintainer="Talend <support@talend.com>"
LABEL com.talend.url="https://www.talend.com"
LABEL com.talend.git.repositories="${project.scm.url}"
LABEL com.talend.git.branches="${git.branch}"
LABEL com.talend.git.commits="${git.commit.id}"
LABEL com.talend.docker.cmd="docker run -d -p ${boundPort}:${boundPort} tacokit/component-server:${PROJECT_VERSION}"
LABEL com.talend.docker.params="_JAVA_OPTIONS=<JVM options (system properties etc), ex: -Dtalend.component.server.component.registry=/path/to/component-registry.propertes -Dtalend.component.server.maven.repository=/path/to/m2>"
LABEL com.talend.docker.healthcheck="curl --fail http://localhost:${boundPort}/api/v1/environment"

ENV MEECROWAVE_HOME       ${imageWorkDir}
ENV MEECROWAVE_BASE       ${imageWorkDir}
ENV MEECROWAVE_PID        ${imageWorkDir}/conf/server.pid
ENV LANG                  en_US.UTF-8
ENV LOGGING_LAYOUT        TEXT
ENV LD_LIBRARY_PATH       /opt/talend/component-kit/sigar
ENV TRACING_SAMPLING_RATE 1
