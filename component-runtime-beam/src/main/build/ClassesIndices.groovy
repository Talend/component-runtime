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
    def startMarker = name.replace('-', '_').replace('.', '_').toUpperCase(Locale.ROOT) + '('
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

def excludedPackages = [
    'org.w3c',
    'org.xml.sax',
    'org.jboss.netty.',
    'org.mortbay.util.',
    'org.objenesis.',
    'org.json4s.',
    'org.htrace.',
    'org.iq80.',
    'org.glassfish.',
    'org.apache.xml.',
    'org.apache.xerces.',
    'org.apache.xbean.',
    'org.apache.wml.',
    'org.apache.oro.',
    'org.apache.log4j.',
    'org.apache.jute.',
    'org.apache.ivy.',
    'org.apache.http.',
    'scala.',
    'org.slf4j.',
    'org.apache.commons.',
    'org.apache.curator.',
    'jersey.',
    'javax.',
    'javassist.',
    'io.netty.',
    'com.sun.',
    'com.ning.',
    'com.google.protobuf.',
    'com.google.gson.',
    'com.github.luben.',
    'com.esotericsoftware.',
    'com.codehale.',
    'com.clearspring.',
    'org.jvnet.',
    'net.razorvine.',
    'net.jpountz.',
    'com.twitter.chill.',
    'com.google.errorprone.annotations.',
    'org.apache.html.',
    'org.aopalliance.',
    'org.fusesource.',
    'org.mortbay.',
    'org.kohsuke.',
    // todo: check
    'org.apache.avro.hadoop.',
    'org.apache.avro.ipc.',
    'org.apache.avro.mapred.',
    'org.apache.avro.mapreduce.',
    'org.apache.zookeeper.',
    'org.apache.hadoop.',
    'org.apache.directory.',
    'com.codahale.metrics.'
]

def simplifiedPackages = [
    'org.apache.beam.repackaged', // we want everything inside repackaged
    'org.apache.beam.repackaged.',
    'org.apache.beam.vendor.',
    'org.apache.beam.runners.',
    'com.fasterxml.jackson.module.',
    'org.apache.commons.compress.',
    'avro.shaded.com.google.common.',
    'com.fasterxml.jackson.',
    'org.codehaus.jackson.',
    'org.tukaani.xz.',
    'com.google.common.',
    'org.kohsuke.args4j.',
    'org.joda.time.',
    'org.apache.avro.',
    'py4j.',
    'org.znerd.',
    'org.spark_project.',
    'org.roaringbitmap.',
    'org.fusesource.hawtjni.',
    'org.apache.spark.'
]

def doIndex = { dependency, excludes ->
    def dependenciesResolver = session.container.lookup(LifecycleDependencyResolver)

    def resolutionProject = new MavenProject(artifactId: 'temp', groupId: 'temp', version: 'temp', packaging: 'pom');
    resolutionProject.artifact = new DefaultArtifact(project.groupId, project.artifactId, project.version, 'compile',
            'pom', null, new DefaultArtifactHandler())
    resolutionProject.dependencies = [dependency] as List
    resolutionProject.remoteArtifactRepositories = project.remoteArtifactRepositories

    def scopes = ['compile', 'runtime']
    dependenciesResolver.resolveProjectDependencies(resolutionProject, scopes, scopes, session, false, [project.artifact] as Set)

    def classes = []
    resolutionProject.resolvedArtifacts.each { it ->
        def jar = new JarFile(it.file)
        classes = (classes << list(jar.entries())
                .findAll {
                    !it.isDirectory() &&
                    it.name.endsWith('.class') && // is a class
                    !it.name.startsWith('META-INF') && // is not a java 9 mjar thing - aliases another class so useless
                    !it.name.endsWith('package-info.class') &&
                    !it.name.contains('$') // if nested the prefix test on the parent is enough, try to limit the number of classes we keep
                }
                .collect { it.name.substring(0, it.name.length() - '.class'.length()).replace('/', '.') }
                .findAll { excludedPackages.stream().noneMatch { pref -> it.startsWith(pref) } }
                .collect { // to limit the number of classes we replace some 100% sure classes by their package only
                    simplifiedPackages.stream()
                        .filter { pck -> it.startsWith(pck) }
                        .findFirst()
                        .map { pck ->
                            def marker = it.indexOf('.', pck.length())
                            if (marker > 0) {
                                return it.substring(0, marker)
                            }
                            it
                        }
                        .orElse(it)
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
[
    depFor('beam-runners-direct-java'),
    depFor('beam-runners-spark'),
    new Dependency(groupId: 'org.apache.spark', artifactId:  "spark-core_${project.properties['spark-scala.version']}", version:  "${project.properties['spark.version']}", scope: 'compile')
].each { doIndex(it, sdkClasses) }
