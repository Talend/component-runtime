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

# Configure the environment to start a local server
# $1: INSTALL_DIR
# $2: DOWNLOAD_DIR
# $3: TCK_VERSION
# $4: CONNECTOR_VERSION
# $5: CONNECTOR_LIST
# $5: SERVER_PORT (default "8080")

# set -xe

EXTRA_INSTRUMENTED="vault-client"
JACOCO_VERSION="0.8.1"
JAVAX_VERSION="1.1.1"
MVN_CENTRAL="https://repo.maven.apache.org/maven2"
TALEND_REPO="https://artifacts-zl.talend.com/nexus/service/local/repositories"
COMPONENT_SE_REPO="${TALEND_REPO}/TalendOpenSourceRelease/content/org/talend/components"
COMPONENT_LINK="${COMPONENT_SE_REPO}/NAME/VERSION/NAME-VERSION-component.car"


# check command possibilities
command -v wget || usage "'wget' command"
command -v unzip || usage "'unzip' command"

# check parameters
[ -z ${1+x} ] && usage "Parameter 'download_dir'"
[ -z ${2+x} ] && usage "Parameter 'install_dir'"
[ -z ${3+x} ] && usage "Parameter 'coverage_dir'"
[ -z ${4+x} ] && usage "Parameter 'tck_version'"
[ -z ${5+x} ] && usage "Parameter 'connectors_version'"
[ -z ${6+x} ] && usage "Parameter 'connector'"

DOWNLOAD_DIR=${1}
INSTALL_DIR=${2}
COVERAGE_DIR=${3}
TCK_VERSION=${4}
CONNECTOR_VERSION="${5}"
CONNECTOR_LIST="${6}"
SERVER_PORT=${7:-"8080"}

DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR="${COVERAGE_DIR}/src"
M2_DIR=${DISTRIBUTION_DIR}/m2
LOCAL_M2_DIR="/root/.m2/repository"

SETENV_PATH="${DISTRIBUTION_DIR}/bin/setenv.sh"
REGISTRY_PATH="${DISTRIBUTION_DIR}/conf/components-registry.properties"

if [[ ${TCK_VERSION} != *"-SNAPSHOT" ]]; then
  echo "Use maven central repository: ${MVN_CENTRAL}"
  MVN_SOURCE=${MVN_CENTRAL}
else
  USE_LOCAL_M2=true
  echo "Use maven local m2 repository: ${LOCAL_M2_DIR}"
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
  echo "Start TCK Web tester using registry"
  echo "Usage : $0 <install_dir> <download_dir> <tck_version> <connector_version> <connector_list>"
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
  echo "Server version    : ${CONNECTOR_VERSION}"
  echo "Server version    : ${CONNECTOR_LIST}"
  echo "Delete the install dir" && rm -rf "${INSTALL_DIR}"
  echo "Create needed directories:"
  mkdir -vp "${INSTALL_DIR}"
  mkdir -vp "${DOWNLOAD_DIR}"
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

  # Download
  if [[ -z ${USE_LOCAL_M2}  ]]; then
    wget -N -P "${DOWNLOAD_DIR}" "${file_path}"
  else
    cp -v "${file_path}" "${DOWNLOAD_DIR}"
  fi

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

  # Deploy
  component_path="${DOWNLOAD_DIR}/${CONNECTOR_LIST}-${CONNECTOR_VERSION}-component.car"
  echo "Deploy the car: ${component_path}"
  java -jar "${component_path}" maven-deploy --location "${M2_DIR}"

	echo "##############################################"
}

function download_all {
  printf "\n# Download ALL\n"

  printf "\n## Download and unzip component-server\n"
  if [[ -z ${USE_LOCAL_M2}  ]]; then
    wget -N -P "${DOWNLOAD_DIR}" "${MVN_SOURCE}/org/talend/sdk/component/component-server/${TCK_VERSION}/component-server-${TCK_VERSION}.zip"
  else
    cp -v "${MVN_SOURCE}/org/talend/sdk/component/component-server/${TCK_VERSION}/component-server-${TCK_VERSION}.zip" "${DOWNLOAD_DIR}"
  fi
  unzip -d "${INSTALL_DIR}" "${DOWNLOAD_DIR}/component-server-${TCK_VERSION}.zip"

  printf "\n## Download and unzip jacoco\n"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/org/jacoco/jacoco/0.8.1/jacoco-0.8.1.zip"
  unzip "${DOWNLOAD_DIR}/jacoco-${JACOCO_VERSION}.zip" "lib/*" -d "${DISTRIBUTION_DIR}"

  printf "\n## Download javax\n"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/javax/activation/activation/${JAVAX_VERSION}/activation-${JAVAX_VERSION}.jar"
  cp -v "${DOWNLOAD_DIR}/activation-${JAVAX_VERSION}.jar" "${LIB_DIR}"

  download_component_lib "component-tools"
  download_component_lib "component-tools-webapp"
  download_component_lib "component-form-core"
  download_component_lib "component-form-model"
  download_component_lib "component-runtime-beam"
  download_component_lib "${EXTRA_INSTRUMENTED}"

  download_connector

  echo "##############################################"
}

function create_setenv_script {
  printf "\n# Create the setenv.sh script\n"
	{
		echo 	"""
    export JAVA_HOME=\"${JAVA_HOME}\"
    export ENDORSED_PROP=\"ignored.endorsed.dir\"
    export MEECROWAVE_OPTS=\"-Dhttp=${SERVER_PORT}\"
    export MEECROWAVE_OPTS=\"-Dtalend.component.manager.m2.repository=m2 \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-D_talend.studio.version=7.4.1 \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-Dtalend.vault.cache.vault.url=none \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-Dtalend.component.server.component.registry=conf/components-registry.properties \${MEECROWAVE_OPTS}\"
    """
	} > "${SETENV_PATH}"
	chmod +x "${SETENV_PATH}"
	echo "##############################################"
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
