#!/usr/bin/env bash
#
#  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
  printf 'Generate Jacoco report\n'
  printf 'Usage : %s <install_dir> <coverage_dir>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}
# Parameters:
[ -z ${1+x} ] && usage "Parameter 'install_dir' is needed."
[ -z ${2+x} ] && usage "Parameter 'coverage_dir' is needed."

_INSTALL_DIR=${1}
_COVERAGE_DIR=${2}


# Constants
_DISTRIBUTION_DIR="${_INSTALL_DIR}/component-server-distribution"
_JACOCO_EXEC_PATH="${_DISTRIBUTION_DIR}/jacoco.exec"
_LIB_DIR="${_DISTRIBUTION_DIR}/lib"
_LIB_BACKUP_DIR="${_COVERAGE_DIR}/lib_backup"
_SOURCES_DIR="${_COVERAGE_DIR}/src"
_JACOCO_CLI_PATH="${_LIB_DIR}/jacococli.jar"

main() (
  printf '##############################################\n'
  printf 'Jacoco report creation with:\n'
  printf '%s\n' "${_JACOCO_CLI_PATH}"
  printf 'JACOCO_EXEC_PATH: %s\n' "${_JACOCO_EXEC_PATH}"
  printf 'LIB_BACKUP_DIR: %s\n' "${_LIB_BACKUP_DIR}"
  printf 'csv: %s\n' "${_COVERAGE_DIR}/report.csv"
  printf 'xml: %s\n' "${_COVERAGE_DIR}/report.xml"
  printf 'html: %s\n' "${_COVERAGE_DIR}/html"
  printf 'src: %s\n' "${_SOURCES_DIR}"
  printf '##############################################\n'

  if [[ -f "${_JACOCO_EXEC_PATH}" ]]; then
    jacoco_report
  else
    printf 'Jacoco execution file not found.\n'
    printf 'Jacoco report ABORTED\n'
  fi

)

function jacoco_report {
  printf '# Jacoco report\n'
  java -jar "${_JACOCO_CLI_PATH}" \
    report "${_JACOCO_EXEC_PATH}" \
    --classfiles "${_LIB_BACKUP_DIR}" \
    --csv "${_COVERAGE_DIR}/report.csv" \
    --xml "${_COVERAGE_DIR}/report.xml" \
    --html "${_COVERAGE_DIR}/html" \
    --name "TCK API test coverage" \
    --sourcefiles "${_SOURCES_DIR}"
    # not used yet --quiet
	printf '##############################################\n'
}

main "$@"
