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

INSTALL_DIR="${1}"
COVERAGE_DIR="${2}"

# Constants
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
JACOCO_EXEC_PATH="${DISTRIBUTION_DIR}/jacoco.exec"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
SOURCES_DIR="${COVERAGE_DIR}/src"
JACOCO_CLI_PATH="${LIB_DIR}/jacococli.jar"

main() (
  printf '##############################################\n'
  printf 'Jacoco report creation with:'
  printf '%s\n' "${JACOCO_CLI_PATH}"
  printf 'JACOCO_EXEC_PATH: %s\n' "${JACOCO_EXEC_PATH}"
  printf 'LIB_BACKUP_DIR: %s\n' "${LIB_BACKUP_DIR}"
  printf 'csv: %s\n' "${COVERAGE_DIR}/report.csv"
  printf 'xml: %s\n' "${COVERAGE_DIR}/report.xml"
  printf 'html: %s\n' "${COVERAGE_DIR}/html"
  printf 'src: %s\n' "${SOURCES_DIR}"
  printf '##############################################\n'

  jacoco_report
)

function jacoco_report {
  printf '# Jacoco report\n'
  java -jar "${JACOCO_CLI_PATH}" \
    report "${JACOCO_EXEC_PATH}" \
    --classfiles "${LIB_BACKUP_DIR}" \
    --csv "${COVERAGE_DIR}/report.csv" \
    --xml "${COVERAGE_DIR}/report.xml" \
    --html "${COVERAGE_DIR}/html" \
    --name "TCK API test coverage" \
    --sourcefiles "${SOURCES_DIR}"
    # not used yet --quiet
	printf '##############################################\n'
}

main "$@"
