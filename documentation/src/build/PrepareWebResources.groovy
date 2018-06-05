/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.build

def copyJsResource = { source, output ->
    def target = new File(project.build.directory, "${project.build.finalName}/_/${output}")
    target.parentFile.mkdirs()
    target.text = source.text
    log.info("Imported ${target.name}")
}

// add web resources
copyJsResource(
    new File(project.basedir, 'src/main/frontend/node_modules/highlight.js/styles/idea.css').toURI().toURL(),
    'css/idea.css')
copyJsResource(
    new File(project.basedir, 'src/main/frontend/node_modules/instantsearch.js/dist/instantsearch-theme-algolia.min.css').toURI().toURL(),
    'css/instantsearch-theme-algolia.min.css')
copyJsResource(
    new File(project.basedir, 'src/main/frontend/node_modules/instantsearch.js/dist/instantsearch.min.css').toURI().toURL(),
    'css/instantsearch.min.css')
copyJsResource(
    new File(project.basedir, 'src/main/frontend/node_modules/instantsearch.js/dist/instantsearch.min.js').toURI().toURL(),
    'js/instantsearch.min.js')

// otherwise gh-pages ignore the _ folders
new File(project.build.directory, "${project.build.finalName}/.nojekyll").text = ''
