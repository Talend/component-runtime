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
  printf 'Usage : %s [install_dir] [runtime_version] [server_port]\n' "${0}"
  printf '\n'
  exit 1
}

# Parameters:
_LOCAL_SERVER_TEST_PATH=${1:"/tmp/tck_server"}
_RUNTIME_VERSION=${2:"1.54.0-SNAPSHOT"}
_SERVER_PORT=${3:-"8081"}

_LOCAL_SERVER_TEST_PATH=/home/acatoire
_DOWNLOAD_DIR="${_LOCAL_SERVER_TEST_PATH}/test_demo/download"
_INSTALL_DIR="${_LOCAL_SERVER_TEST_PATH}/test_demo/install"
_COVERAGE_DIR="${_LOCAL_SERVER_TEST_PATH}/test_demo/coverage"

./server-registry-stop.sh "${_INSTALL_DIR}"

./server-registry-init.sh "${_DOWNLOAD_DIR}" \
                          "${_INSTALL_DIR}" \
                          "${_COVERAGE_DIR}" \
                          "${_RUNTIME_VERSION}" \
                          '1.41.0' \
                          '/home/acatoire/.m2/repository'\
                          "${_SERVER_PORT}"

./server-registry-start.sh "${_INSTALL_DIR}" "${_COVERAGE_DIR}" "${_SERVER_PORT}"

