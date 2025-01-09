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

function usage(){
  printf 'Edit version in pom\n'
  printf 'Usage : %s <new_version> <connector_path>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}
# Parameters:
[ -z ${1+x} ] && usage "Parameter 'new_version' is needed."
[ -z ${2+x} ] && usage "Parameter 'connector_path' is needed."

_ELEMENT='component-runtime.version'
_NEW_VERSION=${1:?Missing version}
_CONNECTOR_PATH=${2:?Missing connector path}


setMavenProperty() (
  PROPERTY_NAME=${1}
  PROPERTY_VALUE=${2}
  mvn 'versions:set-property' \
    --batch-mode \
    --define property="${PROPERTY_NAME}" \
    --define newVersion="${PROPERTY_VALUE}"
)

# Change pom versions
main() (

  if [ "default" = "${_NEW_VERSION}" ]; then
    printf 'No version change in the pom, keep the default one\n'
  else
    printf 'Change version in the pom %s to %s in %s\n' "${_ELEMENT}" "${_NEW_VERSION}" "${_CONNECTOR_PATH}"
    cd "${_CONNECTOR_PATH}" || exit
    pwd
    setMavenProperty "${_ELEMENT}" "${_NEW_VERSION}"
  fi
)

main "$@"
