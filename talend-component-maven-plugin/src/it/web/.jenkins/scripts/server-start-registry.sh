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
# $2: 'connectors_version'"nd execution
# $3: 'port' default 8080"

JACOCO_VERSION="0.8.1"
JAVAX_VERSION="1.1.1"
MVN_CENTRAL="https://repo.maven.apache.org/maven2"
LOCAL_IP=$(hostname -I | sed 's/ *$//g')

EXTRA_INSTRUMENTED="vault-client*"

INSTALL_DIR="/tmp/webtester/install"
DOWNLOAD_DIR="/tmp/webtester/download"
COVERAGE_DIR="${INSTALL_DIR}/coverage"
DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR=${COVERAGE_DIR}/src
JACOCO_CLI_PATH="${LIB_DIR}/jacococli.jar"
SETENV_PATH="${DISTRIBUTION_DIR}/bin/setenv.sh"
REGISTRY_PATH="${DISTRIBUTION_DIR}/conf/components-registry.properties"


# check command possibilities
command -v wget || usage "'wget' command"
# check parameters
[ -z ${1+x} ] && usage "Parameter 'tck_version'"
[ -z ${2+x} ] && usage "Parameter 'connectors_version'"

TCK_VERSION=${1}
CONN_VERSION=${2}
PORT=${3:-"8080"}

main() (
  echo "##############################################"
  echo "Start web tester"
  echo "##############################################"

  init
  download_all
  jacoco_instrument
  create_setenv_script
  generate_registry

  jacoco_report
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
  echo "Connectors list   : ${CONNECTORS_LIST}"
  echo "Server version    : ${TCK_VERSION}"
  echo "Connector version : ${CONN_VERSION}"
  echo "Delete the install dir" && rm -r "${INSTALL_DIR}"
  echo "##############################################"
  echo "## Create needed directories"
  mkdir "${COVERAGE_DIR}"
  mkdir "${SOURCES_DIR}"
  mkdir "${LIB_BACKUP_DIR}"
  mkdir "${LIB_INSTRUMENTED_DIR}"
  mkdir "${INSTALL_DIR}"
}

function download_component_lib {
  printf "\n## Download %s" "${LIB_NAME}"
  LIB_NAME="$1"
  new_file_name="${LIB_NAME}-${TCK_VERSION}.jar"
  wget -N -P "${DOWNLOAD_DIR}" "${MVN_CENTRAL}/org/talend/sdk/component/${LIB_NAME}/${TCK_VERSION}/${new_file_name}"
  echo "copy the file in lib folder"
  cp -v "${DOWNLOAD_DIR}/${new_file_name}" "${LIB_DIR}"
}

function download_all {
  printf "\n# Download ALL"

  printf "\n## Check connector presence in .m2"
  # TODO: Check connector presence in .m2

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

  download_lib "component-tools"
  download_lib "component-tools-webapp"
  download_lib "component-form-core"
  download_lib "component-form-model"
  download_lib "component-runtime-beam"

  echo "##############################################"
}

function create_setenv_script {
  printf "\n# Create setenv file: %s" "${SETENV_PATH}"
	{
		printf 'export JAVA_HOME="%s"\n' "${JAVA_HOME}"
		printf 'export ENDORSED_PROP="ignored.endorsed.dir"\n'
		printf 'export MEECROWAVE_OPTS="-Dhttp=%s"\n' "${PORT}"
		printf 'export MEECROWAVE_OPTS="-D_talend.studio.version=7.4.1 -Dtalend.vault.cache.vault.url=none -Dtalend.component.server.component.registry=conf/components-registry.properties ${MEECROWAVE_OPTS}"\n'
	} > "${SETENV}"
	chmod +x "${SETENV}"

	echo "##############################################"
}

function generate_registry {
  printf "\n# Generate components registry"
  # Create the file
	echo "" > "${REGISTRY_PATH}"
	# Add connectors TODO: make it more dynamic when needed

  connector="azure-dls-gen2"
  echo "conn_1=org.talend.components\\:${connector}\\:${CONN_VERSION}" >> "${REGISTRY_PATH}"

	echo "##############################################"
}

function jacoco_instrument {
  printf "\n# Jacoco instrument ###############################"
  printf "\n\n## Backup original files\n"
  cp -v "${LIB_DIR}"/component-*".jar"  "${LIB_BACKUP_DIR}"
  cp -v "${LIB_DIR}/${EXTRA_INSTRUMENTED}"*".jar " "${LIB_BACKUP_DIR}"
  cp -v ./*"org.talend.sdk.component."* "${SOURCES_DIR}" # TODO check it

  printf "\n\n## Instrument classes in jar files\n"
  java -jar "${JACOCO_CLI_PATH}" \
    instrument "${LIB_DIR}/component-"*".jar "\
               "${LIB_DIR}/${EXTRA_INSTRUMENTED}"*".jar" \
    --dest "${LIB_INSTRUMENTED_DIR}"

  printf "\n## Copy instrumented jar to the lib folder \n"
  cp -v "${LIB_INSTRUMENTED_DIR}"/*".jar" "${LIB_DIR}"
	echo "##############################################"
}

function jacoco_report {
  printf "\n# Jacoco report ###############################"
  java -jar "${JACOCO_CLI_PATH}" \
    report "${DISTRIBUTION_DIR}/jacoco.exec" \
    --classfiles "${LIB_BACKUP_DIR}" \
    --csv "${COVERAGE_DIR}/report.csv" \
    --xml "${COVERAGE_DIR}/report.xml" \
    --html "${COVERAGE_DIR}/html" \
    --name "TCK API test coverage" \
    --sourcefiles "${SOURCES_DIR}"
    # not used yet --sourcefiles <path> --quiet
	echo "##############################################"
}

function start_server {
  printf "\n# Start server"
  # Go in the distribution directory
  cd "${DISTRIBUTION_DIR}" || exit
  # Start the server
  ./bin/meecrowave.sh start

  echo ""
  echo "You can now connect on http://${LOCAL_IP}:${PORT}"
  echo ""
	echo "##############################################"
}

function stop_server {
  printf "\n# Stop server ###############################"
  cd "${DISTRIBUTION_DIR}" || exit
  ./bin/meecrowave.sh stop
	echo "##############################################"
}

main "$@"
