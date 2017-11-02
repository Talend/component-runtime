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

import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.model.Dependency
import org.apache.maven.model.Repository

import java.util.jar.JarInputStream

// note: we can extract that logic into a small lib instead of inlining it here
// technical note: we avoid to use tycho which is too impacting on the build process

log.info 'Preparing project dependencies from p2 repository'
log.info 'Don\'t forget to activate the "ide" profile to be able to develop it'

def dependencies = [
        'org.talend.osgi.lib.loader',
        'org.talend.model',
        'org.talend.core.ui',
        'org.talend.core.runtime',
        'org.talend.designer.core',
        'org.talend.commons.runtime',
        'org.talend.common.ui.runtime',
        'org.talend.maven.resolver',
        'org.eclipse.m2e.core'
]

def studioVersion = project.properties['studio.version'].replace('-', '.');
def studioRepo = "https://artifacts-oss.talend.com/nexus/content/unzip/TalendP2UnzipOpenSourceRelease/org/talend/studio/talend-tos-p2-repo/${studioVersion}/talend-tos-p2-repo-${studioVersion}.zip-unzip/"
def p2LocalRepository = new File(project.basedir, '.p2localrepository')

def doIndex= { base ->
    // find artifacts
    def artifacts = new URL("${base}/artifacts.jar").openConnection();
    if (artifacts.getResponseCode() != 200) {
        throw new IllegalStateException("Bad request to find p2 artifacts: HTTP ${artifacts.getResponseCode()}")
    }
    def artifactsJar = new JarInputStream(artifacts.getInputStream())
    try {
        def artifactsXml;
        while ((artifactsXml = artifactsJar.getNextEntry()) != null && !artifactsXml.getName().equals("artifacts.xml")) {
            // next
        }
        if (artifactsXml == null) {
            artifactsJar.close()
            throw new IllegalStateException('No artifacts.xml found')
        }

        def parsedArtifacts = new XmlParser().parseText(artifactsJar.text).artifacts.artifact
        def artifactIndex = parsedArtifacts.findAll {
            it.version != null && it.id != null &&
                    it.properties != null && it.properties.property != null &&
                    it.properties.property.findIndexOf { p -> p.@name == 'maven-groupId' } >= 0
        }
        .collectEntries {
            def propertiesIndex = it.properties.property.collectEntries { p ->
                [(p.@name): p.@value]
            }

            def key = "${propertiesIndex['maven-groupId']}:${propertiesIndex['maven-artifactId']}:${propertiesIndex['maven-version']}"
            def value = "${it.@id}_${it.@version}"

            [(key): value]
        }

        parsedArtifacts.findAll {
            it.version != null && it.id != null && it.properties != null &&
                    it.properties.@size.size() == 1 && it.properties.@size.iterator().next() == '1'
        }
        .each {
            artifactIndex.put("${it.@id}", "${it.@id}_${it.@version}")
        }

        artifactIndex
    } finally {
        artifactsJar.close()
    }
}

def addDependency = { base, localRepo, gav, index ->
    def gavSplit = gav.split(':')
    def localPathJar = new File(localRepo, "${gavSplit[0].replace('.', '/')}/${gavSplit[1]}/${gavSplit[2]}/${gavSplit[1]}-${gavSplit[2]}.jar")
    if (!localPathJar.exists()) {
        if (index.isEmpty()) { // not needed after first download
            index.putAll(doIndex(studioRepo))
        }

        def jarName = index.get(gav)
        if (jarName == null) {
            jarName = index.get("${gavSplit[1]}")
        }
        if (jarName == null) {
            throw new IllegalArgumentException("Didn't find ${gav} nor ${gavSplit[1]}, available: ${index.keySet().toString()}")
        }

        localPathJar.parentFile.mkdirs()
        def os = localPathJar.newOutputStream()
        try {
            os << new URL("${base}/plugins/${jarName}.jar").openStream()
        } finally { // todo: be resilient if download fails instead of storing a corrupted jar and have to delete localPathJar
            os.close()
        }

        // create a fake pom
        def localPom = new File(localPathJar.parentFile, localPathJar.name.substring(0, localPathJar.name.length() - "jar".length()) + 'pom')
        def pomOs = localPom.newOutputStream()
        try {
            pomOs << """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>${gavSplit[0]}</groupId>
  <artifactId>${gavSplit[1]}</artifactId>
  <version>${gavSplit[2]}</version>
  <description>Generated pom at build time without dependencies</description>
</project>"""
        } finally {
            pomOs.close()
        }
    }

    localPathJar
}

// add the local repo
final Repository repository = new Repository();
repository.id = "build-p2-local-repository";
repository.url = new File(project.getBasedir(), ".p2localrepository").toURI().toURL();
project.getRepositories().add(repository);

println("""
  <repositories>
    <repository>
      <id>p2-local</id>
      <url>file:${project.basedir}/.p2localrepository</url>
    </repository>
  </repositories>
""")
println('    <!-- Generated dependencies -->')

def addArtifact = { pj, art ->
    def f = pj.class.getDeclaredField('resolvedArtifacts')
    if (!f.accessible) {
        f.accessible = true
    }
    f.get(pj).add(art)
}

// add the deps
def artifacts = [:]
dependencies.each {
    def dep = new Dependency()
    dep.groupId = 'org.talend.studio'
    dep.artifactId = it
    dep.version = "${project.properties['studio.version']}"

    def gav = "${dep.groupId}:${it}:${dep.version}"
    def jar = addDependency(studioRepo, p2LocalRepository, gav, artifacts)

    project.dependencies.add(dep)

    def artHandler = new DefaultArtifactHandler()
    artHandler.addedToClasspath = true // maven-compiler-plugin uses that flag to determine the javac cp
    def art = new DefaultArtifact(dep.groupId, dep.artifactId, dep.version, 'provided', 'jar', null, artHandler)
    art.file = jar
    // project.resolvedArtifacts.add(art)
    addArtifact(project, art)

    // log it to ensure it is easy to "dev"
    println("    <dependency>\n      <groupId>${dep.groupId}</groupId>\n      <artifactId>${dep.artifactId}</artifactId>\n      <version>\${studio.version}</version>\n      <scope>provided</scope>\n    </dependency>")
}
