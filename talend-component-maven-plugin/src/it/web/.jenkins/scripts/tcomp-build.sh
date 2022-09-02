#!/usr/bin/env bash
#
#  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

# set -xe

function usage(){
  printf 'Quick tcomp build without any test and facultative modules (documentation...)\n'
  printf 'Usage : %s <pom_file_path>\n' "${0}"
  printf '\n'
  printf "%s\n" "${1}"
  printf '\n'
  exit 1
}

# Parameters
[ -z ${1+x} ] && usage 'Parameter "pom_file_path" is needed.\n'

_POM_FILE_PATH="${1}"

# Constants
_MAVEN_FAST_INSTALL_CMD="mvn clean install \
                --define spotless.apply.skip=true \
                --define spotbugs.skip=true \
                --define checkstyle.skip=true \
                --define rat.skip=true \
                --define skipTests \
                --define maven.javadoc.skip=true \
                --define invoker.skip=true \
                --define maven.artifact.threads=25"

main() (
  printf '##############################################\n'
  printf 'Maven fast build\n'
  printf '##############################################\n'

  ${_MAVEN_FAST_INSTALL_CMD} \
  --file "${_POM_FILE_PATH}" \
  --projects \!documentation \
  --projects \!reporting \
  --projects \!sample-parent \
  --projects \!sample-parent/sample \
  --projects \!sample-parent/sample-beam \
  --projects \!sample-parent/documentation-sample \
  --projects \!org.talend.sdk.component:activeif-component \
  --projects \!org.talend.sdk.component:checkbox-component \
  --projects \!org.talend.sdk.component:code-component \
  --projects \!org.talend.sdk.component:credentials-component \
  --projects \!org.talend.sdk.component:datastorevalidation-component \
  --projects \!org.talend.sdk.component:dropdownlist-component \
  --projects \!org.talend.sdk.component:integer-component \
  --projects \!org.talend.sdk.component:minmaxvalidation-component \
  --projects \!org.talend.sdk.component:multiselect-component \
  --projects \!org.talend.sdk.component:patternvalidation-component \
  --projects \!org.talend.sdk.component:requiredvalidation-component \
  --projects \!org.talend.sdk.component:suggestions-component \
  --projects \!org.talend.sdk.component:table-component \
  --projects \!org.talend.sdk.component:textarea-component \
  --projects \!org.talend.sdk.component:textinput-component \
  --projects \!org.talend.sdk.component:updatable-component \
  --projects \!org.talend.sdk.component:urlvalidation-component \
  --projects \!org.talend.sdk.component:talend-component-kit-intellij-plugin \
  --projects \!org.talend.sdk.component:remote-engine-customizer \
  --projects \!org.talend.sdk.component:images \
  --projects \!org.talend.sdk.component:component-server-image \
  --projects \!org.talend.sdk.component:component-starter-server-image \
  --projects \!org.talend.sdk.component:component-server-vault-proxy-image \
  --projects \!org.talend.sdk.component:remote-engine-customizer-image \
  --projects \!org.talend.sdk.component:singer-parent \
  --projects \!org.talend.sdk.component:singer-java \
  --projects \!org.talend.sdk.component:component-kitap \
  --projects \!org.talend.sdk.component:talend-component-maven-plugin \
  --projects \!org.talend.sdk.component:gradle-talend-component \
  --projects \!org.talend.sdk.component:component-runtime-testing \
  --projects \!org.talend.sdk.component:component-runtime-testing-spark \
  --projects \!org.talend.sdk.component:component-runtime-junit \
  --projects \!org.talend.sdk.component:component-runtime-http-junit \
  --projects \!org.talend.sdk.component:component-runtime-junit-base \
  --projects \!org.talend.sdk.component:component-runtime-beam-junit
)

main "$@"
