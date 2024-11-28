#!/usr/bin/env bash
#
#  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

main() {
  echo "Scan completed, now uploading to DefectDojo"

  apk add curl

  curl -s -X 'POST' -H "Authorization: Token ${DEFECTDOJO_API_TOKEN}" \
  'https://defectdojo.dagali.talendinc.com/api/v2/reimport-scan/' \
      -H 'accept: application/json' \
      -H 'Content-Type: multipart/form-data' \
      -F 'close_old_findings=true' \
      -F 'engagement_name=trivy-sca_component-runtime' \
      -F 'auto_create_context=true' \
      -F 'push_to_jira=false' \
      -F 'scan_date=' \
      -F 'do_not_reactivate=true' \
      -F 'minimum_severity=Low' \
      -F 'product_name=component-runtime' \
      -F 'verified=true' \
      -F 'product_type_name=Talend' \
      -F 'scan_type=Trivy Scan' \
      -F 'file=@output/trivy-results.json'
}

main "$@"
