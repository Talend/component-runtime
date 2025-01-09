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
#

set -xe

# Enter in a connector path and start a component test server in bash mode
# This script will fail if the branch already exists.
# $1: Connector path
# $2: Output log file path for background execution
# $3: (Optional) Server port (default is 8080)
# $4: (Optional) Timeout value in minute for the server (default 2mn)
main() (

  _CONNECTOR_PATH=${1:?Missing connector path}
  _LOG_FILE=${2:?Missing log file path}
  _SERVER_PORT=${3:-"8081"}
  _TIMEOUT=${4:-"120"}

  printf '# Go into given path: %s\n' "${_CONNECTOR_PATH}"
  cd "${_CONNECTOR_PATH}" || exit
  pwd

  printf '# Create the command\n'
  if [ -z "${_SERVER_PORT}" ]; then
    port_cmd=
  else
    port_cmd="--define talend.web.port=${_SERVER_PORT}"
  fi
  if [ -z "${_TIMEOUT}" ]; then
    timeout_cmd=
  else
    timeout_cmd="--define talend.web.batch.timeout=${_TIMEOUT}"
  fi

  printf '# Execute the command\n'
  cmdOption=('--define talend.web.batch=true' "${port_cmd}" "${timeout_cmd}")
  # printf '%s\n' "${cmdOption[*]}"

  # execute command
  mvn talend-component:web "${cmdOption[*]}" >"${_LOG_FILE}" 2>&1
)

main "$@"
