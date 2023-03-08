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
  printf 'Generate Jacoco html report\n'
  printf 'Usage : %s <tests_path>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}
# Parameters:
[ -z ${1+x} ] && usage "Parameter 'tests_path' is needed."

_TESTS_PATH=${1}

main() (
  printf '##############################################\n'
  printf 'Jacoco generate html report from:\n'
  printf '%s\n' "${_TESTS_PATH}"
  printf '##############################################\n'

  jacoco_html
)

function jacoco_html {
  mvn surefire-report:report-only --file "${_TESTS_PATH}"
  mvn site --define generateReports=false --file "${_TESTS_PATH}"
	printf '##############################################\n'
}

main "$@"
