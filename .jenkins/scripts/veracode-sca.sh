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

function usage(){
  printf 'Run a Veracode SCA (Source Clear Analysis)\n'
  printf 'Usage : %s <srcclr_api_token>\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}

# Parameters:
[ -z ${1+x} ] && usage 'Parameter "srcclr_api_token" is needed.'

_SRCCLR_API_TOKEN=${1}

main() (
  printf '##############################################\n'
  printf 'Veracode SCA (Source Clear Analysis)\n'
  printf '##############################################\n'

  veracode_sca
)

function veracode_sca {

  cp .jenkins/settings.xml ~/.m2/
  curl -sSL https://download.sourceclear.com/ci.sh | SRCCLR_API_TOKEN=${_SRCCLR_API_TOKEN} DEBUG=1 sh -s -- scan --allow-dirty --recursive --skip-collectors npm;

}

main "$@"
