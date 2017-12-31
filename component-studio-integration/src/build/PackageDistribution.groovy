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
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver

import java.nio.file.Files
import java.nio.file.StandardCopyOption

// quick sanity check - workaround for gmavenplus bug
def plugin = new File(project.build.directory, "${project.build.finalName}.jar")
if (!plugin.exists()) {
    return
}

def workDir = new File(project.build.directory, "distribution_workdir/${project.artifactId}-${project.version}-distribution")
workDir.parentFile.mkdirs()

// 1. configuration/.m2/ from dependencies.txt of server
def localRepository = session.settings.localRepository
new MvnDependencyListLocalRepositoryResolver.MvnDependenciesTxtArtifactConverter(new MvnCoordinateToFileConverter())
        .withContent(new File(project.basedir, '../component-server/target/classes/TALEND-INF/dependencies.txt').text)
        .build()
        .each {
            def relativePath = it.toPath()
            def target = new File(workDir, "configuration/.m2/repository/${relativePath}")
            def source = new File(localRepository, relativePath)

            target.parentFile.mkdirs()
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)

            // if we have the pom, attach it as well
            def pomName = source.name.substring(0, source.name.length() - it.type.length() - (it.classifier == null ? 0 : (it.classifier.length() + 1))) + 'pom'
            def pomSource = new File(source.parentFile, pomName)
            if (pomSource.exists()) {
                Files.copy(pomSource.toPath(), new File(target.parentFile, pomName).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

// 2. plugins/studio.jar
def pluginFile = new File(workDir, "plugins/${plugin.name}")
pluginFile.parentFile.mkdirs()
Files.copy(plugin.toPath(), pluginFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

// 3. add a readme
new File(workDir, 'README.adoc').text = """
= Talend Component Kit Studio Plugin

This zip contains all the files needed to setup Talend Component Kit in a standalone Talend Studio.

Its layout follows the studio layout and you should be able to just merge this zip with a freshly unpacked studio.

You can then have a look to the configuration you can add in configuration/config.init at https://talend.github.io/component-runtime/latest/documentation-rest.html#_server_configuration
"""

// todo: add a script or jar to do the merge with `java -jar installer.jar`?

// 4. zip them all
def distribution = new File(project.build.directory, "${project.artifactId}-${project.version}-distribution.zip")
new AntBuilder().zip(destFile: distribution.absolutePath) {
    // include the root folder - avoids to mess up the folder where the user extracts it
    fileset(dir: workDir.parentFile.absolutePath)
}

// 5. attach
projectHelper.attachArtifact(project, 'zip', 'distribution', distribution)
