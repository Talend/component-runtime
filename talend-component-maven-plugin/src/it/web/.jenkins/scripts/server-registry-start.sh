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
  printf 'Start TCK Web tester using registry\n'
  printf 'Usage : %s <install_dir> <coverage_dir> [server_port]\n' "${0}"
  printf '\n'
  printf "%s\n" "${1}"
  printf '\n'
  exit 1
}

# Start a server in registry mode
# Parameters:
[ -z ${1+x} ] && usage 'Parameter "install_dir" is needed.'
[ -z ${2+x} ] && usage 'Parameter "coverage_dir" is needed.'
[ -z ${3+x} ] && printf 'Parameter "server_port" use the default value: 8080\n'

INSTALL_DIR=${1}
COVERAGE_DIR=${2}
PORT=${3:-'8080'}

# Constants
EXTRA_INSTRUMENTED='vault-client'
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR="${COVERAGE_DIR}/src"
JACOCO_CLI_PATH="${LIB_DIR}/jacococli.jar"
MEECROWAVE_LOG_PATH="${DISTRIBUTION_DIR}/logs/meecrowave.out"

main() (
  printf '##############################################\n'
  printf 'Start web tester\n'
  printf '##############################################\n'

  jacoco_instrument
  start_server
)

function jacoco_instrument {
  printf '\n# Jacoco instrument\n'
  printf '\n## Backup original jar files\n'
  cp --verbose "${LIB_DIR}/component-"*.jar "${LIB_BACKUP_DIR}"
  cp --verbose "${LIB_DIR}/${EXTRA_INSTRUMENTED}"*.jar "${LIB_BACKUP_DIR}"

  printf '\n## Backup original sources files\n'
  # TODO: TCOMP-2245 some sources seem not to be correctly linked eg: org.talend.sdk.component.server.configuration
  cp --verbose --recursive ./*/src/main/java/* "${SOURCES_DIR}"
  ls -l "${SOURCES_DIR}"

  printf '\n## Instrument classes in jar files\n'
  java -jar "${JACOCO_CLI_PATH}" \
    instrument "${LIB_DIR}/component-"*".jar"\
               "${LIB_DIR}/${EXTRA_INSTRUMENTED}"*".jar" \
    --dest "${LIB_INSTRUMENTED_DIR}"

  printf '\n## Copy instrumented jar to the lib folder\n'
  cp --verbose "${LIB_INSTRUMENTED_DIR}"/*".jar" "${LIB_DIR}"
	printf '##############################################\n'
}

function start_server {
  printf '# Start server\n'
  # Go in the distribution directory
  cd "${DISTRIBUTION_DIR}" || exit 1
  # Start the server
  ./bin/meecrowave.sh start

  if grep -q 'Exception in thread' "${MEECROWAVE_LOG_PATH}"; then
      printf '\n'
      printf 'Error happened on meecrowave start:\n'
      cat "${MEECROWAVE_LOG_PATH}"
      exit 1
    	printf '##############################################\n'
  fi

  LOCAL_IP=$(hostname -I | sed 's/ *$//g')

  printf '\n'
  printf 'You can now connect on http://%s:%s\n' "${LOCAL_IP}" "${PORT}"
  printf '\n'
	printf '##############################################\n'
}

main "$@"
