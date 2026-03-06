#!/usr/bin/env bash
#
#  Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
#
# Correct npm behavior https://docs.npmjs.com/cli/v7/using-npm/scripts#user
# > When npm is run as root, scripts are always run with the effective uid and gid of the working directory owner.
#
# As build is run under root, target folder is created and owned by root.
# But, as specified above, the npm process takes the uid/guid of cwd ie as talend (1000), so when generating
# documentation via antora the process fails w/ :
# FATAL (antora): EACCES: permission denied, mkdir '.../documentation/target/documentation-1.43.0-SNAPSHOT/main'
#
# TODO: check for other modules using npm
# - component-tools
# - component-tools-webapp
# - component-starter-server
#
chown -R $(id -u):$(id -g) documentation