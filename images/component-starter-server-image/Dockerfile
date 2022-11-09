# Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
ARG DOCKER_REGISTRY_HOST
ARG TSBI_IMAGE_PATH
ARG TSBI_VERSION
FROM ${DOCKER_REGISTRY_HOST}${TSBI_IMAGE_PATH}/java17-svc-base:${TSBI_VERSION}

ENV BOUND_PORT ${BOUND_PORT:-8080}
EXPOSE $BOUND_PORT

ARG PROJECT_VERSION
ARG TIMESTAMP
ARG GIT_BRANCH
ARG GIT_COMMIT
LABEL com.talend.name="Talend Component Kit Server Starter"
LABEL com.talend.application="component-starter-server"
LABEL com.talend.service="component-starter-server"
LABEL com.talend.description="Talend Component Kit Server Starter"
LABEL com.talend.version="$PROJECT_VERSION"
LABEL com.talend.build-date="$TIMESTAMP"
LABEL com.talend.docker.cmd="docker run -d -p ${BOUND_PORT}:${BOUND_PORT} tacokit/component-starter-server:$PROJECT_VERSION"
LABEL com.talend.docker.params="_JAVA_OPTIONS=<JVM options>"
LABEL com.talend.docker.healthcheck="curl --fail http://localhost:${BOUND_PORT}/api/v1/environment"
LABEL com.talend.git.repositories="https://github.com/talend/component-runtime"
LABEL com.talend.git.branches="${GIT_BRANCH}"
LABEL com.talend.git.commits="${GIT_COMMIT}"

COPY --chown=talend:talend target/classes/* ${TALEND_APP_HOME}/conf/

ENV MEECROWAVE_HOME       ${TALEND_APP_HOME}
ENV MEECROWAVE_BASE       ${TALEND_APP_HOME}
ENV MEECROWAVE_PID        ${TALEND_APP_HOME}/conf/server.pid
ENV TALEND_JDK_SERIAL_FILTER ${TALEND_JDK_SERIAL_FILTER:-oracle.sql.**;!*}
ENV CLASSPATH ${TALEND_HOME}/app/lib/*:${TALEND_HOME}/custom/*:${TALEND_HOME}/extensions/*:${TALEND_APP_HOME}/resources:${TALEND_APP_HOME}/classes
ENV JAVA_OPTS " -Djdk.serialFilter=${TALEND_JDK_SERIAL_FILTER} -Djava.security.egd=file:/dev/./urandom -Djava.io.tmpdir=${TALEND_APP_HOME}/temp -Dhttp=${BOUND_PORT} -Dmeecrowave.home=${TALEND_APP_HOME} -Dmeecrowave.base=${TALEND_APP_HOME} -Dmeecrowave-properties=${TALEND_APP_HOME}/conf/meecrowave.properties -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=${TALEND_APP_HOME}/conf/log4j2.xml --add-opens java.base/java.util=ALL-UNNAMED"

CMD ["java", "org.apache.meecrowave.runner.Cli"]
