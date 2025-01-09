#!/usr/bin/env bash
#
#  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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


set -xe


function usage(){
  printf 'Manual server starter, like ci would do.\n'
  printf 'For manual testing.\n'
  printf 'Usage : %s [connectors_version] [server_port] [server_dir] \n' "${0}"
  printf '\n'
  exit 1
}

DEFAULT_CONNECTORS_VERSION="1.41.0"

# To debug the component server java, you can uncomment the following line
# _JAVA_DEBUG_PORT="5005"

[ -z ${1+x} ] && printf 'Parameter "connectors_version" not given use default value: %s\n' $DEFAULT_CONNECTORS_VERSION
[ -z ${2+x} ] && printf 'Parameter "server_port" not given use default value: 8081\n'
[ -z ${3+x} ] && printf 'Parameter "server_dir" not given use the default value: "/tmp/test_tck_server"\n'

# Parameters:
USER_PATH=~
CONNECTORS_VERSION=${1:-"${DEFAULT_CONNECTORS_VERSION}"}
SERVER_PORT=${2:-"8081"}
LOCAL_SERVER_TEST_PATH=${3:-"/tmp/test_tck_server"}

DOWNLOAD_DIR="${LOCAL_SERVER_TEST_PATH}/download"
INSTALL_DIR="${LOCAL_SERVER_TEST_PATH}/install"
COVERAGE_DIR="${LOCAL_SERVER_TEST_PATH}/coverage"

SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

COMPONENT_RUNTIME_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
                                --define expression=project.version \
                                --quiet \
                                --define forceStdout)

printf '##############################################\n'
printf 'Init parameters\n'
printf '##############################################\n'
printf "USER_PATH = %s\n" "${USER_PATH}"
printf "LOCAL_SERVER_TEST_PATH = %s\n" "${LOCAL_SERVER_TEST_PATH}"
printf "COMPONENT_RUNTIME_VERSION = %s\n" "${COMPONENT_RUNTIME_VERSION}"
printf "SERVER_PORT = %s\n" "${SERVER_PORT}"
printf "CONNECTORS_VERSION = %s\n" "${CONNECTORS_VERSION}"
printf "DOWNLOAD_DIR = %s\n" "${DOWNLOAD_DIR}"
printf "INSTALL_DIR = %s\n" "${INSTALL_DIR}"
printf "COVERAGE_DIR = %s\n" "${COVERAGE_DIR}"
printf "SCRIPT_PATH = %s\n" "${SCRIPT_PATH}"


bash "${SCRIPT_PATH}"/server-registry-stop.sh "${INSTALL_DIR}" || : #  "|| :" Avoid error if no server is running

bash "${SCRIPT_PATH}"/server-registry-init.sh "${DOWNLOAD_DIR}" \
                                          "${INSTALL_DIR}" \
                                          "${COVERAGE_DIR}" \
                                          "${COMPONENT_RUNTIME_VERSION}" \
                                          "${CONNECTORS_VERSION}" \
                                          "${USER_PATH}/.m2/repository" \
                                          "${SERVER_PORT}" \
                                          "${JAVA_DEBUG_PORT}"

bash "${SCRIPT_PATH}"/server-registry-start.sh "${INSTALL_DIR}" "no_coverage" "${SERVER_PORT}"
