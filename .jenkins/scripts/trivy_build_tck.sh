#!/usr/bin/env bash
#
#  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

set -xe

main() {
  echo "Building project with Maven skipping most of the checks"

  mvn clean install --projects '!talend-component-maven-plugin' \
                    --projects '!documentation' \
                    --settings .jenkins/settings.xml \
                    --batch-mode \
                    --update-snapshots \
                    --show-version \
                    --define skipTests \
                    --define gpg.skip=true \
                    --define enforcer.skip=true\
                    --define spotless.apply.skip=true \
                    --define spotbugs.skip=true \
                    --define checkstyle.skip=true \
                    --define rat.skip=true \
                    --define maven.test.skip=true \
                    --define maven.javadoc.skip=true \
                    --define invoker.skip=true \
                    --define maven.artifact.threads=25
}

main "$@"
