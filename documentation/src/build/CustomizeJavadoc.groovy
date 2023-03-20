/**
 *  Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
def out = new File(project.build.directory, "${project.build.finalName}/apidocs/${project.version.replace('-SNAPSHOT', '')}")
if (!out.exists()) {
    log.warn("No ${out.absolutePath} directory")
    return
}

// replace all stylesheets
def talendStyleSheet = new File(project.basedir, 'src/build/javadoc/stylesheet.css').text
['api', 'junit', 'junit-http'].each {
    def original = new File(out, "${it}/stylesheet.css")
    log.info("Replacing ${original.absolutePath} by Talend javadoc stylesheet")

    def os = original.newOutputStream()
    try {
        os << talendStyleSheet
    } finally {
        os.close()
    }
}
