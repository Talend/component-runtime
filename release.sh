#! /bin/bash

#
#  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

release=$(grep version pom.xml | grep -v xml | head -n 1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | sed 's/-SNAPSHOT//')
trace=${TRACE_FILE:-/tmp/release.log}

echo ">> Maven release $release" | tee "$trace" && \
mvn release:prepare release:perform | tee -a "$trace" && \
echo ">> Reset repo" | tee -a "$trace" &&
git reset --hard | tee -a "$trace" && \
git push --follow-tags | tee -a "$trace" && \
echo ">> Checkouting the release tag" | tee -a "$trace" && \
git checkout -b component-runtime-$release component-runtime-$release | tee -a "$trace" && \
echo ">> Building and pushing docker images $release" | tee -a "$trace" && \
cd images && mvn -DskipTests -Dinvoker.skip=true -T1C clean install jib:build@build -Dimage.currentVersion=$release -Dtalend.server.image.registry=registry.hub.docker.com/ -Djib.httpTimeout=60000 | tee -a "$trace" && cd - && \
echo ">> Rebuilding master and updating it (doc) for next iteration" | tee -a "$trace" && \
git reset --hard | tee -a "$trace" && \
git checkout master | tee -a "$trace" && \
mvn clean install -DskipTests -Dinvoker.skip=true -T1C | tee -a "$trace" && \
git commit -a -m ">> Updating doc for next iteration" | tee -a "$trace" && \
git push | tee -a "$trace"