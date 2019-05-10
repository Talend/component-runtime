/**
 *  Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject

import java.util.jar.JarFile

import static java.util.Collections.list
import static java.util.stream.Collectors.joining
import static java.util.stream.Collectors.toSet

def replaceClassesFor = { name, classes ->
    def enumLocation = new File(project.basedir, 'src/main/java/org/talend/sdk/component/runtime/beam/customizer/Indices.java')
    def content = enumLocation.text
    def startMarker = name.replace('-', '_').toUpperCase(Locale.ROOT) + '('
    def start = content.indexOf(startMarker)
    if (start < 0) {
        throw new IllegalArgumentException("No enum ${name}")
    }
    start += name.length() + 1
    def end = content.indexOf(')', start)
    if (end < 0) {
        throw new IllegalArgumentException("Invalid enum ${name}")
    }
    if (content.substring(start, end) != classes) {
        enumLocation.text = content.substring(0, start) + classes + content.substring(end)
        log.info("Updated ${enumLocation} for ${name}")
    } else {
        log.debug("Enum ${name} already up to date")
    }
}

def doIndex = { dependency, excludes ->
    def dependenciesResolver = session.container.lookup(LifecycleDependencyResolver)

    def resolutionProject = new MavenProject(artifactId: 'temp', groupId: 'temp', version: 'temp', packaging: 'pom');
    resolutionProject.artifact = new DefaultArtifact(project.groupId, project.artifactId, project.version, 'compile',
            'pom', null, new DefaultArtifactHandler())
    resolutionProject.dependencies = [dependency] as List
    resolutionProject.remoteArtifactRepositories = project.remoteArtifactRepositories

    def scopes = ['compile', 'runtime']
    dependenciesResolver.resolveProjectDependencies(resolutionProject, scopes, scopes, session, false, [project.artifact] as Set)

    def classes = [] // TBD: what to do of org.apache.beam.sdk.io, should move in Apache Beam anyway
    resolutionProject.resolvedArtifacts.each { it ->
        def jar = new JarFile(it.file)
        classes = (classes << list(jar.entries())
                .findAll {
                    it.name.endsWith('.class') && // is a class
                    !it.name.startsWith('META-INF') && // is not a java 9 mjar thing - aliases another class so useless
                    !it.name.startsWith('org/slf4j/') && // already handled
                    !it.name.startsWith('javax/json/') && // already handled
                    !it.name.startsWith('scala/') && // unlikely?
                    !it.name.endsWith('package-info.class') &&
                    !it.name.contains('$') // if nested the prefix test on the parent is enough, try to limit the number of classes we keep
                }
                .collect { it.name.substring(0, it.name.length() - '.class'.length()).replace('/', '.')}
                .collect { // to limit the number of classes we replace some 100% sure classes by their package only
                    if (it.startsWith('org.apache.beam.repackaged.')) {
                        def marker = it.indexOf('.', 'org.apache.beam.repackaged.'.length())
                        if (marker > 0) {
                            return it.substring(0, marker)
                        }
                    }
                    if (it.startsWith('org.apache.beam.vendor.')) {
                        def marker = it.indexOf('.', 'org.apache.beam.vendor.'.length())
                        if (marker > 0) {
                            return it.substring(0, marker)
                        }
                    }
                    if (it.startsWith('org.apache.beam.runners.')) {
                        def marker = it.indexOf('.', 'org.apache.beam.runners.'.length())
                        if (marker > 0) {
                            return it.substring(0, marker)
                        }
                    }
                    if (it.startsWith('com.fasterxml.jackson.module.')) {
                        def marker = it.indexOf('.', 'com.fasterxml.jackson.module.'.length())
                        if (marker > 0) {
                            return it.substring(0, marker)
                        }
                    }
                    if (it.startsWith('org.apache.commons.compress.')) {
                        def marker = it.indexOf('.', 'org.apache.commons.compress.'.length())
                        if (marker > 0) {
                            return it.substring(0, marker)
                        }
                    }
                    if (it.startsWith('avro.shaded.com.google.common.')) {
                        def marker = it.indexOf('.', 'avro.shaded.com.google.common.'.length())
                        if (marker > 0) {
                            return it.substring(0, marker)
                        }
                    }
                    return it
                }
                .collect { "\"$it\"" })
                .flatten()
    }
    classes = classes - excludes
    replaceClassesFor(dependency.artifactId,
            "new String[] { // #${classes.size()}\n            ${classes.stream().distinct().sorted().collect(joining(',\n            '))}\n        }")
    classes
}

def depFor = { artifactId ->
    new Dependency(
        groupId: 'org.apache.beam', artifactId: artifactId, version: project.properties['beam.version'], scope: 'compile')
}

def sdkClasses = doIndex(depFor('beam-sdks-java-core'), [])
['beam-runners-direct-java', 'beam-runners-spark']
        .each { doIndex(depFor(it), sdkClasses) }


