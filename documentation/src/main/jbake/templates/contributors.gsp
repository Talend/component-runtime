<%/*
  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
   Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/%>
<%include "header.gsp"%>
	<%include "menu.gsp"%>

    <div class="container section-padded">
        <div class="row title">
            <div class='page-header'>
              <h2>${content.title}</h2>
            </div>
        </div>
        <div class="row contributors">
            <%
                new groovy.json.JsonSlurper().parseText(new File('target/generated-adoc/contributors.json').text).each {contributor ->
            %>
            <div class="contributor">
                <div>
                    <img alt="${contributor.name}" src="${contributor.gravatar}" style="width:140px">
                </div>
                <div>
                    <h3 class="contributor-name" style="font-size:1.4em;">${contributor.name}</h3>
                    <h3 class="contributor-commits" style="font-size:1.4em;">#${contributor.commits} commits</h3>
                    <p>${contributor.description}</p>
                </div>
            </div>
            <% } %>
        </div>
    </div>

<%include "footer.gsp"%>
