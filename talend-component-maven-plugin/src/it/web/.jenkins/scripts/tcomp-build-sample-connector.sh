#!/usr/bin/env bash
#
#  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

function usage(){
  printf 'Quick tcomp build only for the sample connector\n'
  printf 'Usage : %s <pom_file_path>\n' "${0}"
  printf '\n'
  printf "%s\n" "${1}"
  printf '\n'
  exit 1
}

# Parameters
[ -z ${1+x} ] && usage 'Parameter "pom_file_path" is needed.\n'

_POM_FILE_PATH=${1}

# Constants
_MAVEN_TEST_SKIP="--define spotless.apply.skip=true \
                  --define spotbugs.skip=true \
                  --define checkstyle.skip=true \
                  --define rat.skip=true \
                  --define skipTests \
                  --define maven.javadoc.skip=true \
                  --define invoker.skip=true"
_MAVEN_FAST="--define maven.artifact.threads=25 \
             --threads 4C"

main() (
  printf '##############################################\n'
  printf 'Maven fast build\n'
  printf '##############################################\n'

  mvn install \
  --file "${_POM_FILE_PATH}" \
  --projects sample-parent/sample-connector \
  --also-make \
  ${_MAVEN_TEST_SKIP} \
  ${_MAVEN_FAST}

)

main "$@"
