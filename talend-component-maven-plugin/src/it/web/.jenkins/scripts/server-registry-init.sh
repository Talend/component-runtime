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

# set -xe

function usage(){
  printf 'Configure the environment to start a local server\n'
  printf 'Usage : %s <download_dir> <install_dir> <coverage_dir> <tck_version> <connector_version> <connector_list> [server_port]\n' "${0}"
  printf '\n'
  printf '%s\n' "${1}"
  printf '\n'
  exit 1
}

# Parameters:
[ -z ${1+x} ] && usage 'Parameter "download_dir" is needed.'
[ -z ${2+x} ] && usage 'Parameter "install_dir" is needed.'
[ -z ${3+x} ] && usage 'Parameter "coverage_dir" is needed.'
[ -z ${4+x} ] && usage 'Parameter "tck_version" is needed.'
[ -z ${5+x} ] && usage 'Parameter "connectors_version" is needed.'
[ -z ${6+x} ] && usage 'Parameter "connector" is needed.'
[ -z ${7+x} ] && printf 'Parameter "server_port" use the default value: 8080\n'

DOWNLOAD_DIR="${1}"
INSTALL_DIR="${2}"
COVERAGE_DIR="${3}"
TCK_VERSION="${4}"
CONNECTOR_VERSION="${5}"
CONNECTOR_LIST="${6}"
SERVER_PORT="${7:-'8080'}"

# Check command possibilities
which wget || { usage 'wget is not present'; }
which unzip || { usage 'unzip is not present'; }

# Constants
EXTRA_INSTRUMENTED='vault-client'
JACOCO_VERSION='0.8.1'
JAVAX_VERSION='1.1.1'
MVN_CENTRAL='https://repo.maven.apache.org/maven2'
TALEND_REPO='https://artifacts-zl.talend.com/nexus/service/local/repositories'
COMPONENT_SE_REPO="${TALEND_REPO}/TalendOpenSourceRelease/content/org/talend/components"
COMPONENT_LINK="${COMPONENT_SE_REPO}/NAME/VERSION/NAME-VERSION-component.car"

DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
LIB_INSTRUMENTED_DIR="${COVERAGE_DIR}/lib_instrumented"
SOURCES_DIR="${COVERAGE_DIR}/src"
M2_DIR="${DISTRIBUTION_DIR}/m2"
LOCAL_M2_DIR='/root/.m2/repository'

SETENV_PATH="${DISTRIBUTION_DIR}/bin/setenv.sh"
REGISTRY_PATH="${DISTRIBUTION_DIR}/conf/components-registry.properties"

if [[ ${TCK_VERSION} != *"-SNAPSHOT" ]]; then
  printf 'Use maven central repository: %s\n' "${MVN_CENTRAL}"
  MVN_SOURCE=${MVN_CENTRAL}
else
  USE_LOCAL_M2=true
  printf 'Use maven local m2 repository: %s\n' "${LOCAL_M2_DIR}"
  MVN_SOURCE="${LOCAL_M2_DIR}"
fi

main() (
  printf '##############################################\n'
  printf 'Server download\n'
  printf '##############################################\n'

  init
  download_all
  create_setenv_script
  generate_registry
)

function init {

  printf '# Init the environment\n'
  printf '##############################################\n'
  printf 'Install dir       : %s\n' "${INSTALL_DIR}"
  printf 'Server version    : %s\n' "${TCK_VERSION}"
  printf 'Server version    : %s\n' "${CONNECTOR_VERSION}"
  printf 'Server version    : %s\n' "${CONNECTOR_LIST}"

  printf 'Delete the install dir\n'
  rm --recursive --force "${INSTALL_DIR}"

  printf 'Create needed directories:\n'
  mkdir --verbose --parents "${INSTALL_DIR}"
  mkdir --verbose --parents "${DOWNLOAD_DIR}"
  mkdir --verbose --parents "${DISTRIBUTION_DIR}"
  mkdir --verbose --parents "${COVERAGE_DIR}"
  mkdir --verbose --parents "${LIB_BACKUP_DIR}"
  mkdir --verbose --parents "${LIB_INSTRUMENTED_DIR}"
  mkdir --verbose --parents "${SOURCES_DIR}"
  mkdir --verbose --parents "${M2_DIR}"
}

function download_component_lib {

  lib_name="$1"
  printf '\n## Download component element: %s\n' "${lib_name}"
  file_name="${lib_name}-${TCK_VERSION}.jar"
  printf 'File Name: %s\n' "${lib_name}"
  file_path="${MVN_SOURCE}/org/talend/sdk/component/${lib_name}/${TCK_VERSION}/${file_name}"
  printf 'File path: %s\n' "${file_path}"

  # Download
  if [[ -z ${USE_LOCAL_M2}  ]]; then
    wget --timestamping --directory-prefix "${DOWNLOAD_DIR}" "${file_path}"
  else
    cp -v "${file_path}" "${DOWNLOAD_DIR}"
  fi

  printf 'Copy the file in lib folder\n'
  cp -v "${DOWNLOAD_DIR}/${file_name}" "${LIB_DIR}"
}

function download_connector {

  printf '##############################################\n'
  printf '# Download connector: %s\n' "${CONNECTOR_LIST}"
  printf '##############################################\n'
  printf 'Downloaded connectors:\n'

  # Replace "VERSION" by var $CONNECTOR_VERSION in $COMPONENT_LINK
  connector_final_link=${COMPONENT_LINK//VERSION/$CONNECTOR_VERSION}
  # Replace "COMPONENT" by var $connector
  connector_final_link=${connector_final_link//NAME/$CONNECTOR_LIST}

  printf 'From following link: %s\n' "${connector_final_link}"

  # Download
  wget --timestamping --directory-prefix "${DOWNLOAD_DIR}" "${connector_final_link}"

  # Deploy
  component_path="${DOWNLOAD_DIR}/${CONNECTOR_LIST}-${CONNECTOR_VERSION}-component.car"
  printf 'Deploy the car: %s\n' "${component_path}"
  java -jar "${component_path}" maven-deploy --location "${M2_DIR}"

}

function download_all {
  printf '\n# Download ALL\n'

  printf '\n## Download and unzip component-server\n'
  if [[ -z ${USE_LOCAL_M2}  ]]; then
    wget --timestamping \
         --directory-prefix "${DOWNLOAD_DIR}" \
         "${MVN_SOURCE}/org/talend/sdk/component/component-server/${TCK_VERSION}/component-server-${TCK_VERSION}.zip"
  else
    cp --verbose \
       "${MVN_SOURCE}/org/talend/sdk/component/component-server/${TCK_VERSION}/component-server-${TCK_VERSION}.zip" \
       "${DOWNLOAD_DIR}"
  fi
  unzip -d "${INSTALL_DIR}" "${DOWNLOAD_DIR}/component-server-${TCK_VERSION}.zip"

  printf '\n## Download and unzip jacoco\n'
  wget --timestamping \
       --directory-prefix "${DOWNLOAD_DIR}" \
       "${MVN_CENTRAL}/org/jacoco/jacoco/0.8.1/jacoco-0.8.1.zip"
  unzip "${DOWNLOAD_DIR}/jacoco-${JACOCO_VERSION}.zip" "lib/*" -d "${DISTRIBUTION_DIR}"

  printf '\n## Download javax\n'
  wget --timestamping \
       --directory-prefix "${DOWNLOAD_DIR}" \
       "${MVN_CENTRAL}/javax/activation/activation/${JAVAX_VERSION}/activation-${JAVAX_VERSION}.jar"

  cp --verbose "${DOWNLOAD_DIR}/activation-${JAVAX_VERSION}.jar" "${LIB_DIR}"

  download_component_lib 'component-tools'
  download_component_lib 'component-tools-webapp'
  download_component_lib 'component-form-core'
  download_component_lib 'component-form-model'
  download_component_lib 'component-runtime-beam'
  download_component_lib "${EXTRA_INSTRUMENTED}"

  download_connector
}

function create_setenv_script {
  printf '# Create the setenv.sh script\n'
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
}

function generate_registry {
  printf '\n# Generate components registry\n'
  # Create the file
	echo "" > "${REGISTRY_PATH}"
	# Add connectors FIXME: TCOMP-2246 make really compatible with a list
  echo "conn_1=org.talend.components\\:${CONNECTOR_LIST}\\:${CONNECTOR_VERSION}" >> "${REGISTRY_PATH}"
}


main "$@"
