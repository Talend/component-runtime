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

# Manual server starter like ci would do.

_DOWNLOAD_DIR="/tmp/test_demo/download"
_INSTALL_DIR="/tmp/test_demo/install"
_COVERAGE_DIR="/tmp/test_demo/coverage"
_RUNTIME_VERSION="1.53.0-SNAPSHOT"
_SERVER_PORT="8080"

./server-registry-init.sh "${_DOWNLOAD_DIR}" \
                          "${_INSTALL_DIR}" \
                          "${_COVERAGE_DIR}" \
                          "${_RUNTIME_VERSION}" \
                          "1.36.0" \
                          "azure-dls-gen2" \
                          "${_SERVER_PORT}"

./server-registry-start.sh "${_INSTALL_DIR}" "${_COVERAGE_DIR}" "${_SERVER_PORT}"

