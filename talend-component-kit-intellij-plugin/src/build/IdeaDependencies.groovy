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
import org.apache.maven.model.Dependency
import org.apache.maven.model.Repository

if (project.properties[project.artifactId + '.idea.dependencies.setup'] != null) {
    return
}

// note: we can extract that logic into a small lib instead of inlining it here

log.info 'Preparing project dependencies from idea distribution'
log.info 'Don\'t forget to activate the "ide" profile to be able to develop it'

def dependencies = [
        'lib/openapi',
        'lib/idea',
        'lib/util',
        'lib/extensions',
        'lib/gson-2.8.5',
        'lib/jdom',
        'lib/jsr305',
        'lib/platform-api',
        'lib/platform-impl',
        'lib/platform-util-ui',
        'lib/swingx-core-1.6.2-2',
        'lib/slf4j-api-1.7.25',
        'plugins/java/lib/java-api',
        'plugins/java/lib/java-impl',
        'plugins/properties/lib/properties',
        'plugins/maven/lib/maven',
        'plugins/gradle-java/lib/gradle-java'
]

def ideaBase = project.properties['idea.unpacked.folder']
def localRepository = new File(project.basedir, '.cache/m2/localrepository')
def extractedZip = new File(ideaBase)
if (!localRepository.exists()) { // in case of a release just reuse the existing cache if it exists
    def mainCache = new File(project.basedir, "../../../${project.basedir.name}/.cache")
    if (mainCache.exists()) {
        def ideaCache = new File(mainCache, 'idea')
        def mainLocalRepository = new File(mainCache, 'm2/localrepository')
        if (ideaCache.exists() && mainLocalRepository.exists()) {
            ideaCache = mainCache
            extractedZip = ideaCache
            localRepository = mainLocalRepository
        }
    }
}

if (!extractedZip.exists()) {
    // ensure idea is downloaded - we dont use download plugin since it computes md5+sha1+sha512 each time and it is slowwwww
    def ideaRemoteZip = new URL(project.properties['idea.source'])
    def ideaLocalZip = new File(project.basedir, '.cache/download/idea.zip')
    def ideaLocalVersion = new File(project.basedir, '.cache/download/idea.version')

    ideaLocalZip.parentFile.mkdirs()

    if (!ideaLocalVersion.exists() || ideaLocalVersion.text.trim() != project.properties['idea.build.version']) {
        ideaLocalZip.parentFile.mkdirs()
        def os = ideaLocalZip.newOutputStream()
        try {
            os << new BufferedInputStream(ideaRemoteZip.openStream())
        } finally {
            os.close()
        }

        ideaLocalVersion.text = project.properties['idea.build.version']

        // extract now
        new AntBuilder().unzip(src: ideaLocalZip.absolutePath, dest: ideaBase, overwrite: 'true')
    }
}

def addDependency = { base, localRepo, name ->
    def artifactId = name.replace('/', '_')
    def localPathJar = new File(localRepo, "com/intellij/idea/${artifactId}/${project.properties['idea.build.version']}/${artifactId}-${project.properties['idea.build.version']}.jar")
    if (!localPathJar.exists() ||
            Boolean.parseBoolean(System.getProperty('talend.component.kit.build.idea.m2.forceupdate', project.properties['talend.component.kit.build.studio.m2.forceupdate']))) {

        def fileSrc = new File(base, "${name}.jar")
        if (!fileSrc.exists()) {
            throw new IllegalArgumentException("No jar ${fileSrc}")
        }

        localPathJar.parentFile.mkdirs()
        def os = localPathJar.newOutputStream()
        try {
            os << fileSrc.newInputStream()
        } finally {
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
  <groupId>com.intellij.idea</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${project.properties['idea.build.version']}</version>
  <description>Generated pom at build time without dependencies</description>
</project>"""
        } finally {
            pomOs.close()
        }
    }

    localPathJar
}

// add the local repo
final Repository repository = new Repository()
repository.id = "build-idea-local-repository"
repository.url = new File(project.getBasedir(), '.cache/m2/localrepository').toURI().toURL()
project.getRepositories().add(repository)

println("""
  <repositories>
    <repository>
      <id>idea-local</id>
      <url>file:${project.basedir}/.cache/m2/localrepository</url>
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
dependencies.each {
    def dep = new Dependency()
    dep.groupId = 'com.intellij.idea'
    dep.artifactId = it.replace('/', '_')
    dep.version = "${project.properties['idea.build.version']}"

    def jar = addDependency(ideaBase, localRepository, it)

    project.dependencies.add(dep)

    def artHandler = new DefaultArtifactHandler()
    artHandler.addedToClasspath = true // maven-compiler-plugin uses that flag to determine the javac cp
    def art = new DefaultArtifact(dep.groupId, dep.artifactId, dep.version, 'provided', 'jar', null, artHandler)
    art.file = jar
    // project.resolvedArtifacts.add(art)
    addArtifact(project, art)

    // log it to ensure it is easy to "dev"
    println("    <dependency>\n      <groupId>${dep.groupId}</groupId>\n      <artifactId>${dep.artifactId}</artifactId>\n      <version>\${idea.build.version}</version>\n      <scope>provided</scope>\n    </dependency>")
}

project.properties[project.artifactId + '.idea.dependencies.setup'] = 'true'
