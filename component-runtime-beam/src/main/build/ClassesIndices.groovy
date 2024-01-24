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
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject

import java.util.jar.JarFile

import static java.util.Collections.list
import static java.util.stream.Collectors.joining

String findResourceName(artifactId) {
    underscore = artifactId.indexOf('_')
    "TALEND-INF/classloader/indices/${underscore > 0 ? artifactId.substring(0, underscore) : artifactId}"
}

excludedPackages = [
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
    'org.apache.zookeeper.',
    'org.apache.hadoop.',
    'org.apache.directory.',
    'com.codahale.metrics.'
]

simplifiedPackages = [
    'org.apache.beam.repackaged.',
    'org.apache.beam.sdk.coders.',
    'org.apache.beam.sdk.metrics.',
    'org.apache.beam.sdk.schemas.',
    'org.apache.beam.sdk.state.',
    'org.apache.beam.sdk.testing.',
    'org.apache.beam.sdk.transforms.reflect.',
    'org.apache.beam.sdk.transforms.windowing.',
    'org.apache.beam.sdk.values.',
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

Collection<String> doIndex(dependency, excludes, simplifiedPackages, excludedPackages) {
    dependenciesResolver = session.container.lookup(LifecycleDependencyResolver)

    resolutionProject = new MavenProject(artifactId: 'temp', groupId: 'temp', version: 'temp', packaging: 'pom');
    resolutionProject.artifact = new DefaultArtifact(project.groupId, project.artifactId, project.version, 'compile',
            'pom', null, new DefaultArtifactHandler())
    resolutionProject.dependencies = [dependency] as List
    resolutionProject.remoteArtifactRepositories = project.remoteArtifactRepositories

    scopes = ['compile', 'runtime']
    dependenciesResolver.resolveProjectDependencies(resolutionProject, scopes, scopes, session, false, [project.artifact] as Set)

    classes = []
    resolutionProject.resolvedArtifacts.each { it ->
        jar = new JarFile(it.file)
        classes = (classes << list(jar.entries())
            .collect { it.name.replace('/', '.') }
            .findAll {
                it.endsWith('.class') && // is a class
                !it.startsWith('META-INF') && // is not a java 9 mjar thing - aliases another class so useless
                !it.endsWith('module-info.class') &&
                !it.endsWith('package-info.class') &&
                !it.contains('$') // if nested the prefix test on the parent is enough, try to limit the number of classes we keep
            }
            .findAll { excludedPackages.stream().noneMatch { pref -> it.startsWith(pref) } }
            .collect { it.substring(0, it.length() - '.class'.length()) }
            .collect { // to limit the number of classes we replace some 100% sure classes by their package only
                simplifiedPackages.stream()
                    .filter { pck -> it.startsWith(pck) }
                    .findFirst()
                    .map { pck ->
                        marker = it.indexOf('.', pck.length())
                        marker > 0 ? it.substring(0, marker) : pck.substring(0, pck.length() - 1)
                    }
                    .orElse(it)
            })
            .flatten()
        jar.close()
    }
    classes = classes - excludes

    output = new File(project.build.outputDirectory, findResourceName(dependency.artifactId))
    output.parentFile.mkdirs()
    output.text = "# ${classes.size()} classes\n${classes.stream().distinct().sorted().collect(joining('\n'))}"
    log.info("Wrote $output for ${dependency.artifactId} (${classes.size()} classes)")

    classes
}

Dependency beamDep(artifactId) {
    new Dependency(
        groupId: 'org.apache.beam', artifactId: artifactId, version: project.properties['beam.version'], scope: 'compile')
}

sdkClasses = doIndex(beamDep('beam-sdks-java-core'), [], simplifiedPackages, excludedPackages)
doIndex(beamDep('beam-runners-direct-java'), sdkClasses, simplifiedPackages, excludedPackages)

def sparkCore = new Dependency(groupId: 'org.apache.spark', artifactId: "spark-core_${project.properties['spark-scala.version']}", version: "${project.properties['spark.version']}", scope: 'compile')
def sparkStreaming = new Dependency(groupId: 'org.apache.spark', artifactId: "spark-streaming_${project.properties['spark-scala.version']}", version: "${project.properties['spark.version']}", scope: 'compile')
sparkCoreClasses = doIndex(sparkCore, sdkClasses, simplifiedPackages, excludedPackages)
sparkStreamingClasses = doIndex(sparkStreaming, sdkClasses, simplifiedPackages, excludedPackages)
