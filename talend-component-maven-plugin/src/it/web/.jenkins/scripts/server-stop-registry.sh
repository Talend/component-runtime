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

# Enter in a connector path and start a component test server in bash mode
# This script will fail if the branch already exists.
# $1: Connector name
# $2: (Optional) Output log file path for background execution
# $3: (Optional) Server port (default is 8080)

INSTALL_DIR="/tmp/webtester/install"
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"

main() (
  echo "##############################################"
  echo "Start web tester"
  echo "##############################################"

  stop_server
)

function stop_server {
  printf "\n# Stop server ###############################"
  cd "${DISTRIBUTION_DIR}" || exit
  ./bin/meecrowave.sh stop
	echo "##############################################"
}

main "$@"
