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

_DOWNLOAD_DIR="${1}"
_INSTALL_DIR="${2}"
_COVERAGE_DIR="${3}"
_TCK_VERSION="${4}"
_CONNECTOR_VERSION="${5}"
_CONNECTOR_LIST="${6}"
_SERVER_PORT="${7:-'8080'}"

# Check command possibilities
which wget || { usage 'wget is not present'; }
which unzip || { usage 'unzip is not present'; }

# Constants
_EXTRA_INSTRUMENTED='vault-client'
_JACOCO_VERSION='0.8.1'
_JAVAX_VERSION='1.1.1'
_MVN_CENTRAL='https://repo.maven.apache.org/maven2'
_TALEND_REPO='https://artifacts-zl.talend.com/nexus/service/local/repositories'
_COMPONENT_SE_REPO="${_TALEND_REPO}/TalendOpenSourceRelease/content/org/talend/components"
_COMPONENT_LINK="${_COMPONENT_SE_REPO}/NAME/VERSION/NAME-VERSION-component.car"

_DISTRIBUTION_DIR="${_INSTALL_DIR}/component-server-distribution"
_LIB_DIR="${_DISTRIBUTION_DIR}/lib"
_LIB_BACKUP_DIR="${_COVERAGE_DIR}/lib_backup"
_LIB_INSTRUMENTED_DIR="${_COVERAGE_DIR}/lib_instrumented"
_SOURCES_DIR="${_COVERAGE_DIR}/src"
_M2_DIR="${_DISTRIBUTION_DIR}/m2"
_LOCAL_M2_DIR='/root/.m2/repository'

_SETENV_PATH="${_DISTRIBUTION_DIR}/bin/setenv.sh"
_REGISTRY_PATH="${_DISTRIBUTION_DIR}/conf/components-registry.properties"

if [[ ${_TCK_VERSION} != *"-SNAPSHOT" ]]; then
  printf 'Use maven central repository: %s\n' "${_MVN_CENTRAL}"
  _MVN_SOURCE=${_MVN_CENTRAL}
else
  USE_LOCAL_M2=true
  printf 'Use maven local m2 repository: %s\n' "${_LOCAL_M2_DIR}"
  _MVN_SOURCE="${_LOCAL_M2_DIR}"
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
  printf 'Install dir       : %s\n' "${_INSTALL_DIR}"
  printf 'Server version    : %s\n' "${_TCK_VERSION}"
  printf 'Server version    : %s\n' "${_CONNECTOR_VERSION}"
  printf 'Server version    : %s\n' "${_CONNECTOR_LIST}"

  printf 'Delete the install dir\n'
  rm --recursive --force "${_INSTALL_DIR}"

  printf 'Create needed directories:\n'
  mkdir --verbose --parents "${_INSTALL_DIR}"
  mkdir --verbose --parents "${_DOWNLOAD_DIR}"
  mkdir --verbose --parents "${_DISTRIBUTION_DIR}"
  mkdir --verbose --parents "${_COVERAGE_DIR}"
  mkdir --verbose --parents "${_LIB_BACKUP_DIR}"
  mkdir --verbose --parents "${_LIB_INSTRUMENTED_DIR}"
  mkdir --verbose --parents "${_SOURCES_DIR}"
  mkdir --verbose --parents "${_M2_DIR}"
}

function download_component_lib {

  lib_name="$1"
  printf '\n## Download component element: %s\n' "${lib_name}"
  file_name="${lib_name}-${_TCK_VERSION}.jar"
  printf 'File Name: %s\n' "${lib_name}"
  file_path="${_MVN_SOURCE}/org/talend/sdk/component/${lib_name}/${_TCK_VERSION}/${file_name}"
  printf 'File path: %s\n' "${file_path}"

  # Download
  if [[ -z ${USE_LOCAL_M2}  ]]; then
    wget --timestamping --directory-prefix "${_DOWNLOAD_DIR}" "${file_path}"
  else
    cp -v "${file_path}" "${_DOWNLOAD_DIR}"
  fi

  printf 'Copy the file in lib folder\n'
  cp -v "${_DOWNLOAD_DIR}/${file_name}" "${_LIB_DIR}"
}

function download_connector {

  printf '##############################################\n'
  printf '# Download connector: %s\n' "${_CONNECTOR_LIST}"
  printf '##############################################\n'
  printf 'Downloaded connectors:\n'

  # Replace "VERSION" by var $CONNECTOR_VERSION in $COMPONENT_LINK
  connector_final_link=${_COMPONENT_LINK//VERSION/$_CONNECTOR_VERSION}
  # Replace "COMPONENT" by var $connector
  connector_final_link=${connector_final_link//NAME/$_CONNECTOR_LIST}

  printf 'From following link: %s\n' "${connector_final_link}"

  # Download
  wget --timestamping --directory-prefix "${_DOWNLOAD_DIR}" "${connector_final_link}"

  # Deploy
  component_path="${_DOWNLOAD_DIR}/${_CONNECTOR_LIST}-${_CONNECTOR_VERSION}-component.car"
  printf 'Deploy the car: %s\n' "${component_path}"
  java -jar "${component_path}" maven-deploy --location "${_M2_DIR}"

}

function download_all {
  printf '\n# Download ALL\n'

  printf '\n## Download and unzip component-server\n'
  if [[ -z ${USE_LOCAL_M2}  ]]; then
    wget --timestamping \
         --directory-prefix "${_DOWNLOAD_DIR}" \
         "${_MVN_SOURCE}/org/talend/sdk/component/component-server/${_TCK_VERSION}/component-server-${_TCK_VERSION}.zip"
  else
    cp --verbose \
       "${_MVN_SOURCE}/org/talend/sdk/component/component-server/${_TCK_VERSION}/component-server-${_TCK_VERSION}.zip" \
       "${_DOWNLOAD_DIR}"
  fi
  unzip -d "${_INSTALL_DIR}" "${_DOWNLOAD_DIR}/component-server-${_TCK_VERSION}.zip"

  printf '\n## Download and unzip jacoco\n'
  wget --timestamping \
       --directory-prefix "${_DOWNLOAD_DIR}" \
       "${_MVN_CENTRAL}/org/jacoco/jacoco/0.8.1/jacoco-0.8.1.zip"
  unzip "${_DOWNLOAD_DIR}/jacoco-${_JACOCO_VERSION}.zip" "lib/*" -d "${_DISTRIBUTION_DIR}"

  printf '\n## Download javax\n'
  wget --timestamping \
       --directory-prefix "${_DOWNLOAD_DIR}" \
       "${_MVN_CENTRAL}/javax/activation/activation/${_JAVAX_VERSION}/activation-${_JAVAX_VERSION}.jar"

  cp --verbose "${_DOWNLOAD_DIR}/activation-${_JAVAX_VERSION}.jar" "${_LIB_DIR}"

  download_component_lib 'component-tools'
  download_component_lib 'component-tools-webapp'
  download_component_lib 'component-form-core'
  download_component_lib 'component-form-model'
  download_component_lib 'component-runtime-beam'
  download_component_lib "${_EXTRA_INSTRUMENTED}"

  download_connector
}

function create_setenv_script {
  printf '# Create the setenv.sh script\n'
	{
		echo 	"""
    export JAVA_HOME=\"${JAVA_HOME}\"
    export ENDORSED_PROP=\"ignored.endorsed.dir\"
    export MEECROWAVE_OPTS=\"-Dhttp=${_SERVER_PORT}\"
    export MEECROWAVE_OPTS=\"-Dtalend.component.manager.m2.repository=m2 \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-D_talend.studio.version=7.4.1 \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-Dtalend.vault.cache.vault.url=none \${MEECROWAVE_OPTS}\"
    export MEECROWAVE_OPTS=\"-Dtalend.component.server.component.registry=conf/components-registry.properties \${MEECROWAVE_OPTS}\"
    """
	} > "${_SETENV_PATH}"
	chmod +x "${_SETENV_PATH}"
}

function generate_registry {
  printf '\n# Generate components registry\n'
  # Create the file
	printf '\n' > "${_REGISTRY_PATH}"
	# Add connectors FIXME: TCOMP-2246 make really compatible with a list
  printf 'conn_1=org.talend.components\\:%s\\:%s' "${_CONNECTOR_LIST}" "${_CONNECTOR_VERSION}" >> "${_REGISTRY_PATH}"
}


main "$@"
