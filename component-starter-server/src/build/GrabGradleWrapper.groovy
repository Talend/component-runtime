/**
 *  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
        case "gradle/wrapper/gradle-wrapper.properties":
            if(!from.text.contains('distributionUrl=https')){
                throw new IllegalStateException("Invalid file: " + file);
            }
            break
        case "gradlew":
            if(!from.text.contains('org.gradle.wrapper.GradleWrapperMain')){
                throw new IllegalStateException("Invalid file: " + file);
            }
            break
        case "gradlew.bat":
            if(!from.text.contains('"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*')){
                throw new IllegalStateException("Invalid file: " + file);
            }
            break
    }
}

def gradleHomeFile = new File(gradleHome)
def fakeProject = new File(project.build.directory, 'gradle-fake-project-wrapper')
fakeProject.mkdirs()

final int exit = new ProcessBuilder().inheritIO()
        .directory(fakeProject)
        .command(
        new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(),
        "-cp", new File(gradleHomeFile, "lib").listFiles(new FilenameFilter() {
            @Override
            boolean accept(final File dir, final String name) {
                return name.startsWith("gradle-launcher-") && name.endsWith(".jar")
            }
        })[0].getAbsolutePath(),
        "org.gradle.launcher.GradleMain", "--no-daemon",
        "wrapper")
        .start().waitFor()
if (exit != 0) {
    throw new IllegalStateException("bad exit status generating gradle wrapper: " + exit)
}

[
    'gradle/wrapper/gradle-wrapper.jar',
    'gradle/wrapper/gradle-wrapper.properties',
    'gradlew',
    'gradlew.bat'
].each {
    validateWrapperFile(fakeProject, it)
    def from = new File(fakeProject, it)
    def slash = it.lastIndexOf('/')
    def to = new File(project.build.outputDirectory, 'generator/gradle/' + (slash < 0 ? it : it.substring(slash + 1)))
    def fromIs = from.newInputStream()
    def toOs = to.newOutputStream()
    toOs << fromIs
    fromIs.close()
    toOs.close()
}

