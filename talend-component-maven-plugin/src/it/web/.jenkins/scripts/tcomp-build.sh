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

  ${MAVEN_FAST_INSTALL_CMD} -f "${folder_to_build}" -pl \!documentation -pl \!reporting -pl \!sample-parent
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
