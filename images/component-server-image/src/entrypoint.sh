#!/bin/bash
# Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

# Fail on error
set -e

if [ "$PROM_JMX_EXPORTER_AGENT_ON" = true ]; then
    # Add Prometheus JMX Exporter as Java Agent
    PROM_JMX_EXPORTER_AGENT_OPTS="-javaagent:/opt/talend/prometheus-agent/jmx_prometheus_javaagent.jar=$PROM_JMX_EXPORTER_AGENT_PORT:/opt/talend/prometheus-agent/jmx_exporter_config.yaml"
    export JAVA_OPTS="$JAVA_OPTS $PROM_JMX_EXPORTER_AGENT_OPTS"
fi

# From JDK 8u131+ and JDK 9, VM option that allows the JVM to read the memory values from CGgroups (no need for Java 10+: enabled by default)
# See https://developers.redhat.com/blog/2017/03/14/java-inside-docker/
if [[ $JAVA_VERSION == 8* ]] || [[ $JAVA_VERSION == 9* ]]; then
    export JAVA_OPTS="$JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
fi

# Set Java classpath
if [ -e "${TALEND_APP_HOME}/.tsbi.service.classpath" ]; then
    export CLASSPATH=$CLASSPATH:$(cat ${TALEND_APP_HOME}/.tsbi.service.classpath)
fi

# Workaround for Spring Boot failing to find properties files in classpath with some services (investigations provide no explanation)
if [ -n "$SPRINGBOOT_VERSION" ]; then
    export JAVA_OPTS="$JAVA_OPTS -Dspring.config.additional-location=file:${TALEND_APP_HOME}/BOOT-INF/classes/,file:${TALEND_APP_HOME}/WEB-INF/classes/"
fi

# Use LOGGING_LAYOUT to switch between JSON and plain test logs
# See https://github.com/Talend/policies/blob/a40fbee4160cdd3f015a702deb356ba71540090a/official/LoggingConventions.md#layout-1
if [ "$LOGGING_LAYOUT" = "TEXT" ]; then
    echo "JAVA_OPTS=$JAVA_OPTS"
    echo "Starting process $@"
fi

if [ "$1" = "java" ]; then
    shift
    exec java $JAVA_OPTS $@
else
    exec "$@"
fi