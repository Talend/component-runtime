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

# Start a server in registry mode
# $1: install_dir
# $2: coverage_dir
# $3: port (default "8080")

# set -xe

[ -z ${1+x} ] && usage "Parameter 'install_dir'"
[ -z ${1+x} ] && usage "Parameter 'coverage_dir'"
INSTALL_DIR=${1}
COVERAGE_DIR=${2}
PORT=${3:-"8080"}

EXTRA_INSTRUMENTED="vault-client"
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR="${COVERAGE_DIR}/src"
JACOCO_CLI_PATH="${LIB_DIR}/jacococli.jar"

MEECROWAVE_LOG_PATH="${DISTRIBUTION_DIR}/logs/meecrowave.out"


main() (
  echo "##############################################"
  echo "Start web tester"
  echo "##############################################"

  jacoco_instrument
  start_server
)

function usage(){
  echo "Start TCK Web tester using registry"
  echo "Usage : $0 <install_dir> <port>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

function jacoco_instrument {
  printf "\n# Jacoco instrument\n"
  printf "\n## Backup original jar files\n"
  cp --verbose "${LIB_DIR}/component-"*.jar "${LIB_BACKUP_DIR}"
  cp --verbose "${LIB_DIR}/${EXTRA_INSTRUMENTED}"*.jar "${LIB_BACKUP_DIR}"

  printf "\n## Backup original sources files\n"
  cp --verbose ./**/src/main/java/**/*.* "${SOURCES_DIR}"
  ls -l "${SOURCES_DIR}"

  printf "\n## Instrument classes in jar files\n"
  java -jar "${JACOCO_CLI_PATH}" \
    instrument "${LIB_DIR}/component-"*".jar"\
               "${LIB_DIR}/${EXTRA_INSTRUMENTED}"*".jar" \
    --dest "${LIB_INSTRUMENTED_DIR}"

  printf "\n## Copy instrumented jar to the lib folder\n"
  cp --verbose --recursive "${LIB_INSTRUMENTED_DIR}"/*".jar" "${LIB_DIR}"
	echo "##############################################"
}

function start_server {
  printf "# Start server\n"
  # Go in the distribution directory
  cd "${DISTRIBUTION_DIR}" || exit 1
  # Start the server
  ./bin/meecrowave.sh start

  if grep -q "Exception in thread" "${MEECROWAVE_LOG_PATH}"; then
      echo ""
      echo "Error happened on meecrowave start:"
      cat "${MEECROWAVE_LOG_PATH}"
      exit 1
    	echo "##############################################"
  fi

  LOCAL_IP=$(hostname -I | sed 's/ *$//g')

  echo ""
  echo "You can now connect on http://${LOCAL_IP}:${PORT}"
  echo ""
	echo "##############################################"
}

main "$@"
