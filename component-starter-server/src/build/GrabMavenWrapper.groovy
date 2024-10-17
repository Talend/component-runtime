/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
def validateWrapperFile(File baseDir, String file){
    def from = new File(baseDir, file)
    if(!from.exists()){
        throw new IllegalStateException("wrapper file doesn't exist: " + file);
    }
    switch (file){
        case ".mvn/wrapper/maven-wrapper.properties":
            if(!from.text.contains('distributionUrl=https://')){
                throw new IllegalStateException("Invalid file: " + file);
            }
            break
        case "mvnw":
            if(!from.text.contains('exec "$JAVACMD" \\\n' +
                    '  $MAVEN_OPTS \\\n' +
                    '  $MAVEN_DEBUG_OPTS \\\n' +
                    '  -classpath "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" \\\n' +
                    '  "-Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR}" \\\n' +
                    '  ${WRAPPER_LAUNCHER} $MAVEN_CONFIG "$@"')){
                throw new IllegalStateException("Invalid file: " + file);
            }
            break
        case "mvnw.cmd":
            if(!from.text.contains('%MAVEN_JAVA_EXE% ^')){
                throw new IllegalStateException("Invalid file: " + file);
            }
            break
    }
}

def fakeProject = new File(project.build.directory, 'maven-fake-project-wrapper')
fakeProject.mkdirs()

def mvnCommand = "mvn"
if (System.properties['os.name'].toLowerCase(Locale.ENGLISH).contains('windows')) {
    mvnCommand += ".cmd"
}

final int exit = new ProcessBuilder().inheritIO()
        .directory(fakeProject)
        .command(
        new File(System.getProperty('maven.home'), "bin/" + mvnCommand).getAbsolutePath(),
        "-N", "-Dtype=bin", "wrapper:wrapper")
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
    validateWrapperFile(fakeProject, it)
    def from = new File(fakeProject, it)
    def slash = it.lastIndexOf('/')
    def to = new File(project.build.outputDirectory, 'generator/maven/' + (slash < 0 ? it : it.substring(slash + 1)))
    def fromIs = from.newInputStream()
    def toOs = to.newOutputStream()
    toOs << fromIs
    fromIs.close()
    toOs.close()
}

