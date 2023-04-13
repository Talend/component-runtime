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
  printf 'Configure the environment to start a local server\n'
  printf 'Usage : %s <download_dir> <install_dir> <coverage_dir> <tck_version> <connectors_version> <local_m2_dir> [server_port] [java_debug]\n' "${0}"
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
[ -z ${6+x} ] && usage 'Parameter "local_m2_dir" is needed.'
[ -z ${7+x} ] && printf 'Parameter "server_port" use the default value: 8080\n'
[ -z ${8+x} ] && printf 'Parameter "java_debug" not given use default value: off\n'

_DOWNLOAD_DIR=${1}
_INSTALL_DIR=${2}
_COVERAGE_DIR=${3}
_TCK_VERSION=${4}
_CONNECTOR_VERSION=${5}
_LOCAL_M2_DIR=${6}
_SERVER_PORT=${7:-"8080"}
_JAVA_DEBUG_PORT=${8:-''}

# Check command possibilities
which wget || { usage 'wget is not present'; }
which unzip || { usage 'unzip is not present'; }

# Constants
_EXTRA_INSTRUMENTED='vault-client'
_JACOCO_VERSION='0.8.1'
_JAVAX_VERSION='1.1.1'
_MVN_CENTRAL='https://repo.maven.apache.org/maven2'
_TALEND_REPO='https://artifacts-zl.talend.com/nexus/service/local/repositories'
_TALEND_SE_REPO="${_TALEND_REPO}/TalendOpenSourceRelease/content/org/talend"
_COMPONENT_SE_REPO="${_TALEND_SE_REPO}/components"
_COMPONENT_LINK="${_COMPONENT_SE_REPO}/NAME/VERSION/NAME-VERSION-component.car"

_DISTRIBUTION_DIR="${_INSTALL_DIR}/component-server-distribution"
_LIB_DIR="${_DISTRIBUTION_DIR}/lib"
_LIB_BACKUP_DIR="${_COVERAGE_DIR}/lib_backup"
_LIB_INSTRUMENTED_DIR="${_COVERAGE_DIR}/lib_instrumented"
_SOURCES_DIR="${_COVERAGE_DIR}/src"
_M2_DIR="${_DISTRIBUTION_DIR}/m2"

_SAMPLE_CONNECTOR_PATH="${_LOCAL_M2_DIR}/org/talend/sdk/component/sample-connector/VERSION/sample-connector-VERSION-component.car"

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
  printf 'Server init parameters\n'
  printf '##############################################\n'
  printf "DOWNLOAD_DIR = %s\n" "${_DOWNLOAD_DIR}"
  printf "INSTALL_DIR = %s\n" "${_INSTALL_DIR}"
  printf "COVERAGE_DIR = %s\n" "${_COVERAGE_DIR}"
  printf "TCK_VERSION = %s\n" "${_TCK_VERSION}"
  printf "CONNECTOR_VERSION = %s\n" "${_CONNECTOR_VERSION}"
  printf "LOCAL_M2_DIR = %s\n" "${_LOCAL_M2_DIR}"
  printf "SERVER_PORT = %s\n" "${_SERVER_PORT}"

  printf '##############################################\n'
  printf 'Server download\n'
  printf '##############################################\n'

  init
  download_all

  printf '##############################################\n'
  printf 'Server configuration\n'
  printf '##############################################\n'
  create_setenv_script
  generate_registry
)

function init {

  printf '# Init the environment\n'
  printf '##############################################\n'
  printf 'Install dir        : %s\n' "${_INSTALL_DIR}"
  printf 'TCK server version : %s\n' "${_TCK_VERSION}"
  printf 'Connectors version : %s\n' "${_CONNECTOR_VERSION}"

  printf 'Delete existing dir if needed\n'
  rm --recursive --force "${_INSTALL_DIR}"
  rm --recursive --force "${_COVERAGE_DIR}"

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

  lib_name=${1}
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

function download_connector (){
  connector_name=$1
  printf '##############################################\n'
  printf '# Download connector: %s\n' "${connector_name}"
  printf '##############################################\n'
  printf 'Downloaded connectors:\n'

  # Replace "VERSION" by var $CONNECTOR_VERSION in $COMPONENT_LINK
  connector_final_link=${_COMPONENT_LINK//VERSION/$_CONNECTOR_VERSION}
  # Replace "COMPONENT" by var $connector
  connector_final_link=${connector_final_link//NAME/$connector_name}

  printf 'From following link: %s\n' "${connector_final_link}"

  # Download
  wget --timestamping --directory-prefix "${_DOWNLOAD_DIR}" "${connector_final_link}"

  # Deploy
  component_path="${_DOWNLOAD_DIR}/${connector_name}-${_CONNECTOR_VERSION}-component.car"
  printf 'Deploy the car: %s\n' "${component_path}"
  java -jar "${component_path}" maven-deploy --location "${_M2_DIR}"

}

function copy_sample_connector {

  printf '##############################################\n'
  printf '# Download sample connector\n'
  printf '##############################################\n'

  # Replace "VERSION" by var $_TCK_VERSION in _SAMPLE_CONNECTOR_PATH
  sample_connector_path=${_SAMPLE_CONNECTOR_PATH//VERSION/$_TCK_VERSION}

  printf 'From : %s\n' "${sample_connector_path}"

  # Download
  cp -v "${sample_connector_path}" "${_DOWNLOAD_DIR}"

  # Deploy
  printf 'Deploy the car: %s\n' "${sample_connector_path}"
  java -jar "${sample_connector_path}" maven-deploy --location "${_M2_DIR}"

}

function download_all {
  printf '\n# Download ALL\n'

  printf '\n## Download and unzip component-server (meecrowave)\n'
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

  download_connector azureblob
  download_connector azure-dls-gen2
  copy_sample_connector
}

function create_setenv_script {
  printf '# Create the setenv.sh script: %s\n' "${_SETENV_PATH}"
  {
    # Force JAVA_HOME, this is a fix to correct meecrowave that doesn't detect correctly java with asdf
    echo "export JAVA_HOME=\"\$(dirname \"\$(dirname \"\$(asdf which java)\")\")\""
    # ENDORSED_PROP
    echo "export ENDORSED_PROP=\"ignored.endorsed.dir\""
    # MEECROWAVE_OPTS
    echo "export MEECROWAVE_OPTS=\"-Dhttp=${_SERVER_PORT} \${MEECROWAVE_OPTS}\""
    echo "export MEECROWAVE_OPTS=\"-Dtalend.component.manager.m2.repository=m2 \${MEECROWAVE_OPTS}\""
    echo "export MEECROWAVE_OPTS=\"-D_talend.studio.version=7.4.1 \${MEECROWAVE_OPTS}\""
    echo "export MEECROWAVE_OPTS=\"-Dtalend.vault.cache.vault.url=none \${MEECROWAVE_OPTS}\""
    echo "export MEECROWAVE_OPTS=\"-Dtalend.component.server.component.registry=conf/components-registry.properties \${MEECROWAVE_OPTS}\""
    # JDK not recognized automatically on our system
    echo "export MEECROWAVE_OPTS=\"--activate-profiles java9 \${MEECROWAVE_OPTS}\""

    # TODO change default locale.mapping https://jira.talendforge.org/browse/TCOMP-2378
    # Default is en*=en\nfr*=fr\nzh*=zh_CN\nja*=ja\nde*=de
    # It has to be edited to add more languages
    # echo "export MEECROWAVE_OPTS=\"-Dtalend.component.server.locale.mapping=en*=en\\nfr*=fr\\nzh*=zh_CN\\nja*=ja\\nde*=de\\nuk*=uk \${MEECROWAVE_OPTS}\"" >> "${_SETENV_PATH}"
  } >> "${_SETENV_PATH}"

  if [[ ${_JAVA_DEBUG_PORT} ]]; then
    printf '\n\n\n\n\n\n\n\nJava debug activated\n on port 5005\n\n\n\n\n\n\n\n\n'
    {
      echo "export MEECROWAVE_OPTS=\"-agentlib:jdwp=server=y,transport=dt_socket,suspend=y,address=*:${_JAVA_DEBUG_PORT} \${MEECROWAVE_OPTS}\""
    } >> "${_SETENV_PATH}"
  fi

  chmod +x "${_SETENV_PATH}"
}

function generate_registry {
  printf '# Generate components registry: %s\n' "${_REGISTRY_PATH}"
  # Create the file
  printf '\n' > "${_REGISTRY_PATH}"

  # Add connectors FIXME: TCOMP-2246 make really compatible with a list
  printf 'conn_1=org.talend.components\\:%s\\:%s\n' 'azure-dls-gen2' "${_CONNECTOR_VERSION}" >> "${_REGISTRY_PATH}"
  printf 'conn_2=org.talend.components\\:%s\\:%s\n' 'azureblob' "${_CONNECTOR_VERSION}" >> "${_REGISTRY_PATH}"
  # Add the sample connectors
  printf 'conn_3=org.talend.sdk.component\\:sample-connector\\:%s' "${_TCK_VERSION}" >> "${_REGISTRY_PATH}"
}


main "$@"
    