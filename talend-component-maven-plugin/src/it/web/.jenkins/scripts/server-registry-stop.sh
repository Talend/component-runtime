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

# Stop a running server in registry mode
# $1: install_dir

set -xe

[ -z ${1+x} ] && usage "Parameter 'install_dir'"

INSTALL_DIR="${1}"
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"

main() (
  echo "##############################################"
  echo "Stop web tester"
  echo "##############################################"

  stop_server
)

function usage(){
  echo "Stop TCK Web tester using registry"
  echo "Usage : $0 <install_dir>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

function stop_server {
  printf "# Stop server\n"
  cd "${DISTRIBUTION_DIR}" || exit
  sudo ./bin/meecrowave.sh stop
	echo "##############################################"
}

main "$@"
