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
# $1: 'tck_version'"

JACOCO_VERSION="0.8.1"
JAVAX_VERSION="1.1.1"
MVN_CENTRAL="https://repo.maven.apache.org/maven2"
COMPONENT_SE_REPO="${TALEND_REPO}/TalendOpenSourceRelease/content/org/talend/components"
COMPONENT_LINK="${COMPONENT_SE_REPO}/azure-dls-gen2/VERSION/NAME-VERSION-component.car"

INSTALL_DIR="/tmp/webtester/install"
DOWNLOAD_DIR="/tmp/webtester/download"
COVERAGE_DIR="${INSTALL_DIR}/coverage"
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR=${COVERAGE_DIR}/src
M2_DIR=${DISTRIBUTION_DIR}/m2
LOCAL_M2_DIR="/root/.m2/"

SETENV_PATH="${DISTRIBUTION_DIR}/bin/setenv.sh"
REGISTRY_PATH="${DISTRIBUTION_DIR}/conf/components-registry.properties"


# check command possibilities
command -v wget || usage "'wget' command"
command -v unzip || usage "'unzip' command"

# check parameters
[ -z ${1+x} ] && usage "Parameter 'tck_version'"
[ -z ${1+x} ] && usage "Parameter 'connectors_version'"
[ -z ${2+x} ] && usage "Parameter 'connector'"

TCK_VERSION=${1}
CONNECTOR_VERSION="${2}"
CONNECTOR_LIST="${3}"


if [[ ${TCK_VERSION} != *"-SNAPSHOT" ]]; then
  echo "Use maven central repository: ${MVN_CENTRAL}"
  MVN_SOURCE=${MVN_CENTRAL}
else
  echo "Use maven local m2: ${LOCAL_M2_DIR}"
  MVN_SOURCE="${LOCAL_M2_DIR}"
fi

main() (
  echo "##############################################"
  echo "Server download"
  echo "##############################################"

  init
  download_all
  create_setenv_script
  generate_registry
)

function usage(){
  # TODO: check it
  echo "Start TCK Web tester using registry"
  echo "Usage : $0 <tck_version> <conn_version> <connector_list>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

function init {

  printf "\n# Init the environment\n"
  echo "##############################################"
  echo "Install dir       : ${INSTALL_DIR}"
  echo "Server version    : ${TCK_VERSION}"
  echo "Delete the install dir" && rm -rf "${INSTALL_DIR}"
  echo "Create needed directories:"
  mkdir -vp "${INSTALL_DIR}"
  mkdir -vp "${DISTRIBUTION_DIR}"
  mkdir -vp "${COVERAGE_DIR}"
  mkdir -vp "${LIB_BACKUP_DIR}"
  mkdir -vp "${LIB_INSTRUMENTED_DIR}"
  mkdir -vp "${SOURCES_DIR}"
  mkdir -vp "${M2_DIR}"
  echo "##############################################"
}

function download_component_lib {

  LIB_NAME="$1"
  printf "\n## Download component element: %s\n" "${LIB_NAME}"
  file_name="${LIB_NAME}-${TCK_VERSION}.jar"
  printf "File Name: %s\n" "${LIB_NAME}"
  file_path="${MVN_SOURCE}/org/talend/sdk/component/${LIB_NAME}/${TCK_VERSION}/${file_name}"
  printf "File path: %s\n" "${file_path}"

  wget -N -P "${DOWNLOAD_DIR}" "${file_path}"
  echo "copy the file in lib folder"
  cp -v "${DOWNLOAD_DIR}/${file_name}" "${LIB_DIR}"
}

function download_connector {

  echo "##############################################"
  printf "# Download connector: %s\n" "${CONNECTOR_LIST}\n"
  echo "##############################################"
  echo Downloaded connectors:

  # Replace "VERSION" by var $CONNECTOR_VERSION in $COMPONENT_LINK
  connector_final_link=${COMPONENT_LINK//VERSION/$CONNECTOR_VERSION}
  # Replace "COMPONENT" by var $connector
  connector_final_link=${connector_final_link//NAME/$CONNECTOR_LIST}

  echo "From following link: ${connector_final_link}"

  # Download
  wget -N -P "${DOWNLOAD_DIR}" "${connector_final_link}"
  component_path="${DOWNLOAD_DIR}/${CONNECTOR_LIST}-${CONNECTOR_VERSION}-component.car"

  # Deploy
  echo "Deploy the car: ${component_path}"
  java -jar "${component_path}" maven-deploy --location "${M2_DIR}"

	echo "##############################"
}

function download_all {
  printf "\n# Download ALL\n"

  printf "\n## Download and unzip component-server\n"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_SOURCE}/org/talend/sdk/component/component-server/${TCK_VERSION}/component-server-${TCK_VERSION}.zip"
  unzip -d "${INSTALL_DIR}" "${DOWNLOAD_DIR}/component-server-${TCK_VERSION}.zip"

  printf "\n## Download and unzip jacoco\n"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_SOURCE}/org/jacoco/jacoco/0.8.1/jacoco-0.8.1.zip"
  unzip "${DOWNLOAD_DIR}/jacoco-${JACOCO_VERSION}.zip" "lib/*" -d "${DISTRIBUTION_DIR}"

  printf "\n## Download javax\n"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_SOURCE}/javax/activation/activation/${JAVAX_VERSION}/activation-${JAVAX_VERSION}.jar"
  echo copy:
  cp -v "${DOWNLOAD_DIR}/activation-${JAVAX_VERSION}.jar" "${LIB_DIR}"

  download_component_lib "component-tools"
  download_component_lib "component-tools-webapp"
  download_component_lib "component-form-core"
  download_component_lib "component-form-model"
  download_component_lib "component-runtime-beam"

  download_connector

  echo "##############################################"
}

function create_setenv_script {
  printf "\n# Create the setenv.sh script\n"
	{
		echo 	"""
    export JAVA_HOME=\"${JAVA_HOME}\"
    export ENDORSED_PROP=\"ignored.endorsed.dir\"
    export MEECROWAVE_OPTS=\"-Dhttp=${PORT}\"
    export MEECROWAVE_OPTS=\"-Dtalend.component.manager.m2.repository=m2 \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-D_talend.studio.version=7.4.1 \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-Dtalend.vault.cache.vault.url=none \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-Dtalend.component.server.component.registry=conf/components-registry.properties \${MEECROWAVE_OPTS}\"
    """
	} > "${SETENV_PATH}"
	chmod +x "${SETENV_PATH}"
	echo "##############################"
}

function generate_registry {
  printf "\n# Generate components registry\n"
  # Create the file
	echo "" > "${REGISTRY_PATH}"
	# Add connectors TODO: make it more dynamic when needed

  echo "conn_1=org.talend.components\\:${CONNECTOR_LIST}\\:${CONNECTOR_VERSION}" >> "${REGISTRY_PATH}"

	echo "##############################################"
}


main "$@"
