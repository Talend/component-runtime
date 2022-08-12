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

TALEND_REPO="https://artifacts-zl.talend.com/nexus/service/local/repositories"
COMPONENT_SE_REPO="${TALEND_REPO}/TalendOpenSourceRelease/content/org/talend/components"
COMPONENT_LINK="${COMPONENT_SE_REPO}/azure-dls-gen2/VERSION/COMPONENT-VERSION-component.car"

INSTALL_DIR="/tmp/webtester/install"
DOWNLOAD_DIR="/tmp/webtester/download"
DISTRIBUTION_DIR=${INSTALL_DIR}/component-server-distribution
M2_DIR=${DISTRIBUTION_DIR}/m2

command -v wget || usage "'wget' command"

# Check parameters
[ -z ${1+x} ] && usage "Parameter 'connectors_version'"
[ -z ${2+x} ] && usage "Parameter 'connector'"

CONN_VERSION="$1"
CONNECTOR="$2"

main() (
  echo "##############################################"
  echo "Download components"
  echo "##############################################"

  download_connector
)

function usage(){
  # TODO: check it
  echo "Start TCK Web tester using registry"
  echo "Usage : $0 <tck_version> <connectors_version> <install_dir> <connectors_list_file> <port>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

function download_connector {
  printf "# Download connector: %s\n" "${CONNECTOR}"
  echo Downloaded connectors:

  # Replace "VERSION" by var $CONN_VERSION in $COMPONENT_LINK
  connector_final_link=${COMPONENT_LINK//VERSION/$CONN_VERSION}
  # Replace "COMPONENT" by var $connector
  connector_final_link=${connector_final_link//COMPONENT/$connector}

  echo "From following link: ${connector_final_link}"

  # Download
  wget -N -P "${DOWNLOAD_DIR}" "${connector_final_link}"
  component_path="${DOWNLOAD_DIR}/${CONNECTOR}-${CONN_VERSION}-component.car"

  # Deploy
  echo "Deploy the car: ${component_path}"
  java -jar "${component_path}" maven-deploy --location "${M2_DIR}"

	echo "##############################"
}

main "$@"
