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


function usage(){
  printf 'Manual server starter, like ci would do.\n'
  printf 'For manual testing.\n'
  printf 'Usage : %s [install_dir] [runtime_version] [server_port] [connectors_version]\n' "${0}"
  printf '\n'
  exit 1
}

# To debug the component server java app on port 5005, you can uncomment the following line
# _JAVA_DEBUG="YES"

# Parameters:
_USER_PATH=~
_LOCAL_SERVER_TEST_PATH=${1:-"/tmp"}
_RUNTIME_VERSION=${2:-"1.56.0-SNAPSHOT"}
_SERVER_PORT=${3:-"8081"}
_CONNECTORS_VERSION=${4:-"1.41.0"}

_DOWNLOAD_DIR="${_LOCAL_SERVER_TEST_PATH}/test_tck_server/download"
_INSTALL_DIR="${_LOCAL_SERVER_TEST_PATH}/test_tck_server/install"
_COVERAGE_DIR="${_LOCAL_SERVER_TEST_PATH}/test_tck_server/coverage"

_SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

printf '##############################################\n'
printf 'Init parameters\n'
printf '##############################################\n'
printf "USER_PATH = %s\n" "${_USER_PATH}"
printf "LOCAL_SERVER_TEST_PATH = %s\n" "${_LOCAL_SERVER_TEST_PATH}"
printf "RUNTIME_VERSION = %s\n" "${_RUNTIME_VERSION}"
printf "SERVER_PORT = %s\n" "${_SERVER_PORT}"
printf "CONNECTORS_VERSION = %s\n" "${_CONNECTORS_VERSION}"
printf "DOWNLOAD_DIR = %s\n" "${_DOWNLOAD_DIR}"
printf "INSTALL_DIR = %s\n" "${_INSTALL_DIR}"
printf "COVERAGE_DIR = %s\n" "${_COVERAGE_DIR}"
printf "SCRIPT_PATH = %s\n" "${_SCRIPT_PATH}"

"${_SCRIPT_PATH}"/server-registry-stop.sh "${_INSTALL_DIR}"

"${_SCRIPT_PATH}"/server-registry-init.sh "${_DOWNLOAD_DIR}" \
                                          "${_INSTALL_DIR}" \
                                          "${_COVERAGE_DIR}" \
                                          "${_RUNTIME_VERSION}" \
                                          "${_CONNECTORS_VERSION}" \
                                          "${_USER_PATH}/.m2/repository" \
                                          "${_SERVER_PORT}" \
                                          "${_JAVA_DEBUG}"

"${_SCRIPT_PATH}"/server-registry-start.sh "${_INSTALL_DIR}" "${_COVERAGE_DIR}" "${_SERVER_PORT}"
