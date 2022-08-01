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

set -xe

main() (

  serverPort="${1:?Missing server port}"
  timeout="${2:-30}"

  echo "Waiting server on ${serverPort}..."

  i=0

  while ! curl --output /dev/null --silent --head --fail http://localhost:${serverPort}; do
    sleep 1
    ((i = i + 1))
    echo "."

    if test "${i}" -gt "${timeout}"; then
      echo "Timeout, stop waiting"
      exit 1
    fi
  done

  echo "Server launched"
)

main "$@"
