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

INSTALL_DIR="/tmp/webtester/install"
DOWNLOAD_DIR="/tmp/webtester/download"
COVERAGE_DIR="${INSTALL_DIR}/coverage"
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR=${COVERAGE_DIR}/src
M2_DIR=${DISTRIBUTION_DIR}/m2


# check command possibilities
command -v wget || usage "'wget' command"
command -v unzip || usage "'unzip' command"

# check parameters
[ -z ${1+x} ] && usage "Parameter 'tck_version'"

TCK_VERSION=${1}

main() (
  echo "##############################################"
  echo "Start web tester"
  echo "##############################################"

  init
  download_all
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

function init {

  echo "##############################################"
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
  printf "\n## Download component element: %s" "${LIB_NAME}"
  LIB_NAME="$1"
  new_file_name="${LIB_NAME}-${TCK_VERSION}.jar"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/org/talend/sdk/component/${LIB_NAME}/${TCK_VERSION}/${new_file_name}"
  echo "copy the file in lib folder"
  cp -v "${DOWNLOAD_DIR}/${new_file_name}" "${LIB_DIR}"
}

function download_all {
  printf "\n# Download ALL"

  printf "\n## Download and unzip component-server"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/org/talend/sdk/component/component-server/${TCK_VERSION}/component-server-${TCK_VERSION}.zip"
  unzip -d "${INSTALL_DIR}" "${DOWNLOAD_DIR}/component-server-${TCK_VERSION}.zip"

  printf "\n## Download and unzip jacoco"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/org/jacoco/jacoco/0.8.1/jacoco-0.8.1.zip"
  unzip "${DOWNLOAD_DIR}/jacoco-${JACOCO_VERSION}.zip" "lib/*" -d "${DISTRIBUTION_DIR}"

  printf "\n## Download javax"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/javax/activation/activation/${JAVAX_VERSION}/activation-${JAVAX_VERSION}.jar"
  echo copy:
  cp -v "${DOWNLOAD_DIR}/activation-${JAVAX_VERSION}.jar" "${LIB_DIR}"

  download_component_lib "component-tools"
  download_component_lib "component-tools-webapp"
  download_component_lib "component-form-core"
  download_component_lib "component-form-model"
  download_component_lib "component-runtime-beam"

  echo "##############################################"
}

main "$@"
