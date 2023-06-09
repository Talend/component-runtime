#!/usr/bin/env bash
#
#  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
  printf 'Run an apitester campaign\n'
  printf 'Usage : %s <tests_path> <maven_settings> <instance> <account_id> <environment>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}

# Parameters:
[ -z ${1+x} ] && usage 'Parameter "tests_path" is needed.'
[ -z ${2+x} ] && usage 'Parameter "maven_settings" is needed.'
[ -z ${3+x} ] && usage 'Parameter "instance" is needed.'
[ -z ${4+x} ] && usage 'Parameter "account_id" is needed.'
[ -z ${5+x} ] && usage 'Parameter "environment" is needed.'

_TESTS_PATH=${1}
_MAVEN_SETTINGS=${2}
_INSTANCE=${3}
_ACCOUNT_ID=${4}
_ENVIRONMENT=${5}

main() (
  printf '##############################################\n'
  printf 'Api Tester run\n'
  printf '##############################################\n'

  test_run
)

function test_run {

  cd "${_TESTS_PATH}"
  mvn clean test --settings="${_MAVEN_SETTINGS}" \
                 --define instance="${_INSTANCE}" \
                 --define accountId="${_ACCOUNT_ID}" \
                 --define selectedEnvironment="${_ENVIRONMENT}" \
                 --define stopOnFailure=false \
                 --fail-at-end
}

main "$@"
