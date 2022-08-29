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

# Get the version from pom
# $1: 'pom_file_path'

[ -z ${1+x} ] && usage "Parameter 'pom_file_path'"
POM_FILE_PATH=${1}

main() (

  # shellcheck disable=SC2016
  mvn --quiet \
      --file "${POM_FILE_PATH}" \
      --non-recursive \
      -Dexec.executable=echo \
      -Dexec.args='${project.version}' \
      exec:exec
)

function usage(){
  echo "Get the version from a pom file"
  echo "Usage : $0 <pom_file_path>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

main "$@"
