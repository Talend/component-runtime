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
# $1: Connector path
# $2: Output log file path for background execution
# $3: (Optional) Server port (default is 8080)
# $4: (Optional) Timeout value in minute for the server (default 2mn)
main() (

  connectorPath="${1:?Missing connector path}"
  logFile="${2:?Missing log file path}"
  serverPort="${3:-"8081"}"
  timeout="${4:-"120"}"

  echo "# Go into given path: ${connectorPath}"
  cd "${connectorPath}" || exit
  pwd

  echo "# Create the command"
  if [ -z "${serverPort}" ]; then
    portCmd=
  else
    portCmd="-Dtalend.web.port=${serverPort}"
  fi
  if [ -z "${timeout}" ]; then
    timeoutCmd=
  else
    timeoutCmd="-Dtalend.web.batch.timeout=${timeout}"
  fi

  echo "# Execute the command"
  cmdOption=(-Dtalend.web.batch=true "${portCmd}" "${timeoutCmd}" "${outputCmd}")
  # echo ${cmdOption[*]}

  # execute command template
  # mvn talend-component:web -Dtalend.web.batch=true -Dtalend.web.batch.timeout=1 -Dtalend.web.port=8081 > output.log 2>&1 &

  # execute command
  mvn talend-component:web "${cmdOption[*]}" >"${logFile}" 2>&1
)

main "$@"
