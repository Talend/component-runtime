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
  printf 'Manual server stop, like ci would do.\n'
  printf 'For manual testing.\n'
  printf 'Usage : %s [server_dir]\n' "${0}"
  printf '\n'
  exit 1
}

[ -z ${1+x} ] && printf 'Parameter "server_dir" not given use the default value: "/tmp/test_tck_server"\n'

# Parameters:
_USER_PATH=~
_LOCAL_SERVER_TEST_PATH=${1:-"/tmp/test_tck_server"}

_INSTALL_DIR="${_LOCAL_SERVER_TEST_PATH}/install"
_SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

printf '##############################################\n'
printf 'Init parameters\n'
printf '##############################################\n'
printf "USER_PATH = %s\n" "${_USER_PATH}"
printf "LOCAL_SERVER_TEST_PATH = %s\n" "${_LOCAL_SERVER_TEST_PATH}"
printf "SCRIPT_PATH = %s\n" "${_SCRIPT_PATH}"

bash "${_SCRIPT_PATH}"/server-registry-stop.sh "${_INSTALL_DIR}"
