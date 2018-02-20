<%/*
  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
        <div class="row">
          <div id="popcorn" class="col-sm-8"></div>
          <div id="popcorn-comment" class="col-sm-4"></div>
        </div>
    </div>

<%
    // not the most sexy way to parse it but html -> xml works well in groovy but xml -> html is broken
    def sections = []
    def token = '<div class="sect1'
    def index = 0
    do {
        index = content.body.indexOf(token, index)
        if (index < 0) {
            break
        }
        def endDiv = content.body.indexOf('>', index)
        if (endDiv < 0) {
            break
        }
        def endBlock = content.body.indexOf(token, index + 1)
        if (endBlock < 0) {
            endBlock = content.body.length()
        }
        def startClasses = content.body.indexOf('class="', index) + 'class="'.length() + 1
        def endClasses = content.body.indexOf('"', startClasses)
        def classes = content.body.substring(startClasses, endClasses).split(' ')
        def bound = classes.findAll { it.startsWith('time=') }.collect { it.substring('time='.length()) }[0].split(',')
        def realClasses = classes.findAll { !it.startsWith('time=') }.join(' ')
        sections << [
            text: "<div class=\"${realClasses}\">${content.body.substring(endDiv + 1, endBlock)}",
            start: Integer.parseInt(bound[0]),
            end: Integer.parseInt(bound[1])
        ]
        index = endBlock
    } while (true);
%>
<script>
window.slideDefinition = {
    video: "${content.get('popcorn-video')}",
    slides: ${groovy.json.JsonOutput.toJson(sections)}
};
</script>
<script src="js/popcorn.js?version=1.5.11"></script>
<script src="js/popcorn.footnote.js?version=1.5.11"></script>
<script src="js/popcorn-init.js?version=1.0.0"></script>


<%include "footer.gsp"%>