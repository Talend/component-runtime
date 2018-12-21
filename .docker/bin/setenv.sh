#! /bin/sh
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

ARTIFACT_ID=artifactId

TALEND_COMPONENT_LOG4J2_PROFILE=$LOGGING_LAYOUT
[ -z "$TALEND_COMPONENT_LOG4J2_PROFILE" ] && TALEND_COMPONENT_LOG4J2_PROFILE="default"
if [ ! -f $MEECROWAVE_BASE"/conf/log4j2-"$ARTIFACT_ID"-"$TALEND_COMPONENT_LOG4J2_PROFILE".xml" ] ; then
  echo "No log4j2 configuration file found for profile '"$TALEND_COMPONENT_LOG4J2_PROFILE"'"
  exit 1
fi

# patches
if [ -d "$MEECROWAVE_BASE/classes" ]; then
    export CLASSPATH="$MEECROWAVE_BASE/classes"
fi

export MEECROWAVE_PID="$MEECROWAVE_BASE/conf/server.pid"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Djava.security.egd=file:/dev/./urandom"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.component.exit-on-destroy=true"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dlog4j.configurationFile="$MEECROWAVE_BASE"/conf/log4j2-"$ARTIFACT_ID"-"$TALEND_COMPONENT_LOG4J2_PROFILE".xml"

