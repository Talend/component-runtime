#!/usr/bin/env bash
#
#  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
  printf 'Stop a running server in registry mode\n'
  printf 'Usage : %s <install_dir>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}

# Parameters:
[ -z ${1+x} ] && usage 'Parameter "install_dir" is needed.'

_INSTALL_DIR=${1}

# Constants
_DISTRIBUTION_DIR="${_INSTALL_DIR}/component-server-distribution"

main() (
  printf '##############################################\n'
  printf 'Stop web tester\n'
  printf '##############################################\n'

  stop_server
)

function stop_server {
  printf '# Stop server\n'
  cd "${_DISTRIBUTION_DIR}" || exit
  ./bin/meecrowave.sh stop --force
	printf '##############################################\n'
}

main "$@"
