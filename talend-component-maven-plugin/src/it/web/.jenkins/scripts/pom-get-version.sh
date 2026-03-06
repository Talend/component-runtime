#!/usr/bin/env bash
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

set -xe

function usage(){
  printf 'Get the version from a pom file\n'
  printf 'Usage : %s <pom_file_path>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}

# Parameters:
[ -z ${1+x} ] && usage 'Parameter "pom_file_path" is needed.'
_POM_FILE_PATH=${1}

main() (

  # shellcheck disable=SC2016
  mvn --quiet \
      --file "${_POM_FILE_PATH}" \
      --non-recursive \
      --define exec.executable=printf \
      --define exec.args='${project.version}' \
      exec:exec
)

main "$@"
