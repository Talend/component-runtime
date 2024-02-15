#!/usr/bin/env bash
#
#  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

# Parameters:
# $1: github username
# $2: github password
main() {
  local username="${1?Missing github username}"
  local password="${2?Missing github password}"

  git config --global credential.username ${username}
  git config --global credential.helper '!echo password=${password}; echo'
  git config --global credential.name "jenkins-build"

  {
    printf "machine github.com\n"
    printf "login %s\n" "${username}"
    printf "password %s\n" "${password}"

    printf "machine api.github.com\n"
    printf "login %s\n" "${username}"
    printf "password %s\n" "${password}"
  } > ~/.netrc

  chmod 600 ~/.netrc
}

main "$@"
