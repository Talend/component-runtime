/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
def fakeproject = new File(project.build.directory, 'maven-fake-project-wrapper')
fakeproject.mkdirs()

def mvnCommand = "mvn"
if (System.properties['os.name'].toLowerCase().contains('windows')) {
    mvnCommand += ".cmd"
}

final int exit = new ProcessBuilder().inheritIO()
        .directory(fakeproject)
        .command(
        new File(mvnHome, "bin/" + mvnCommand).getAbsolutePath(),
        "-N", "io.takari:maven:wrapper", "-Dmaven=" + mvnVersion)
        .start().waitFor()
if (exit != 0) {
    throw new IllegalStateException("bad exit status generating maven wrapper: " + exit)
}

[
        '.mvn/wrapper/maven-wrapper.jar',
        '.mvn/wrapper/maven-wrapper.properties',
        'mvnw',
        'mvnw.cmd'
].each {
    def from = new File(fakeproject, it)
    def slash = it.lastIndexOf('/')
    def to = new File(project.build.outputDirectory, 'generator/maven/' + (slash < 0 ? it : it.substring(slash + 1)))
    def fromIs = from.newInputStream()
    def toOs = to.newOutputStream()
    toOs << fromIs
    fromIs.close()
    toOs.close()
}

