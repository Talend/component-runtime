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

# Quick tcomp build without any test and facultative modules (documentation...)
# $1: 'folder_to_build'

set -xe

# check parameters
[ -z ${1+x} ] && usage "Parameter 'folder_to_build'"

folder_to_build=${1}


MAVEN_FAST_INSTALL_CMD="mvn clean install \
                -Dspotless.apply.skip=true \
                -Dspotbugs.skip=true \
                -Dcheckstyle.skip=true -Drat.skip=true \
                -DskipTests \
                -Dmaven.javadoc.skip=true \
                -Dinvoker.skip=true \
                -Dmaven.artifact.threads=25"


main() (
  echo "##############################################"
  echo "Maven fast build"
  echo "##############################################"

  ${MAVEN_FAST_INSTALL_CMD} \
  --file "${folder_to_build}" \
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
  --projects \!org.talend.sdk.component:component-kitap
)

function usage(){
  echo "Build the given folder in fast mode"
  echo "Usage : $0 <folder_to_build>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

main "$@"