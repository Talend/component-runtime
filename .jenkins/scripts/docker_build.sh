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

set -xe

# Parameters:
# $1: docker tag version
main() {
  local tag="${1?Missing tag}"

  echo ">> Building and pushing component-server:${tag}"
  cd images/component-server-image
  mvn verify dockerfile:build -P ci-tsbi
  docker tag "talend/common/tacokit/component-server:${tag}" "artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server:${tag}"
  docker push "artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server:${tag}"
  echo ">> Building and pushing component-server-vault-proxy:${tag}"
  cd ../component-server-vault-proxy-image
  mvn verify dockerfile:build -P ci-tsbi
  docker tag "talend/common/tacokit/component-server-vault-proxy:${tag}" "artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server-vault-proxy:${tag}"
  docker push "artifactory.datapwn.com/tlnd-docker-dev/talend/common/tacokit/component-server-vault-proxy:${tag}"
  #TODO starter and remote-engine-customizer
  cd ../..
}

main "$@"
