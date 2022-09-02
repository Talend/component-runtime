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
  printf 'Check if the server is running on the giving port\n'
  printf 'Usage : %s [server_port] [server_address] [timeout]\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}

# Parameters:
[ -z ${1+x} ] && printf 'Parameter "server_port" use the default value: 8080\n'
[ -z ${2+x} ] && printf 'Parameter "server_address" use the default value: http://localhost\n'
[ -z ${3+x} ] && printf 'Parameter "timeout" use the default value: 30s\n'

SERVER_PORT="${1:-'8080'}"
SERVER_ADDRESS="${2:-'http://localhost'}"
TIMEOUT="${3:-30}"

# Check command possibilities
which curl || { usage 'curl is not present'; }

main() (

  printf 'Waiting server maximum %ss for %s\n' "${TIMEOUT}" "${SERVER_ADDRESS}:${SERVER_PORT}"

  i=0

  while ! curl --output /dev/null --silent --head --fail "${SERVER_ADDRESS}":"${SERVER_PORT}"; do
    sleep 1
    ((i = i + 1))
    printf '.'

    if test "${i}" -gt "${TIMEOUT}"; then
      printf 'Timeout, stop waiting\n'
      exit 1
    fi
  done

  printf '\n'
  printf 'Server launched\n'
)

main "$@"
