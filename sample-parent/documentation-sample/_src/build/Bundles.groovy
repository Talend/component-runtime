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
package org.talend.sdk.component.build.documentation.sample

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class Copier extends SimpleFileVisitor<Path> {
    private def project
    private Path module
    private String rootFolder
    private JarOutputStream jar

    @Override
    FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        toRelative(dir).map({ pathStr ->
            jar.putNextEntry(new JarEntry(pathStr + '/'))
            jar.closeEntry()
            FileVisitResult.CONTINUE
        }).orElse(FileVisitResult.SKIP_SUBTREE)
    }

    @Override
    FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        toRelative(file).ifPresent({ pathStr ->
            jar.putNextEntry(new JarEntry(pathStr))
            if ('pom.xml' == file.fileName.toString()) {
                jar.write(sanitizePom(file).getBytes(StandardCharsets.UTF_8))
            } else {
                Files.copy(file, jar)
            }
            jar.closeEntry()
        })
        super.visitFile(file, attrs)
    }

    private Optional<String> toRelative(Path file) {
        def pathStr = module.relativize(file).toString().replace(File.separator, '/')
        if (pathStr.isEmpty() || pathStr.startsWith('src') || pathStr == 'pom.xml' || pathStr == 'README.adoc') {
            Optional.of(rootFolder + (!pathStr.isEmpty() ? '/' + pathStr : ''))
        } else {
            Optional.empty()
        }
    }

    private sanitizePom(path) {
        path.toFile().text.replaceAll("""    <dependency>
      <groupId>org.talend.sdk.component</groupId>
      <artifactId>component-api</artifactId>
      <version>.*</version>
      <scope>provided</scope>
    </dependency>""", """    <dependency>
      <groupId>org.talend.sdk.component</groupId>
      <artifactId>component-api</artifactId>
      <version>${project.version}</version>
      <scope>provided</scope>
    </dependency>""").replaceAll("""      <extension>
        <groupId>org.talend.sdk.component</groupId>
        <artifactId>talend-component-maven-plugin</artifactId>
        <version>.*</version>
      </extension>""", """      <extension>
        <groupId>org.talend.sdk.component</groupId>
        <artifactId>talend-component-maven-plugin</artifactId>
        <version>${project.version}</version>
      </extension>""").replaceAll("""

  <parent>
    <groupId>org.talend.sdk.component</groupId>
    <artifactId>documentation-sample</artifactId>
    <version>${project.version}</version>
  </parent>""", '').replace('  <packaging>',
                "  <groupId>${project.groupId}</groupId>\n  <version>${project.version}</version>\n  <packaging>")
                .replace("""

    <spotless.check.skip>false</spotless.check.skip>
    <spotless.apply.skip>false</spotless.apply.skip>""", '')
    }
}

project.modules.each {
    def base = project.basedir.toPath()
    def workdir = new File(project.build.directory).toPath().resolve('bundles_workdir')
    if (!Files.isDirectory(workdir)) {
        Files.createDirectories(workdir)
    }
    def module = base.resolve(it)
    def rootFolder = module.fileName.toString()

    def zip = workdir.resolve("${it}-${project.version}-distibution.zip").toFile()
    def jar = new JarOutputStream(zip.newOutputStream())
    Files.walkFileTree(module, new Copier(project: project, module: module, rootFolder: rootFolder, jar: jar))
    jar.close()
    projectHelper.attachArtifact(project, 'zip', "${it}-distribution", zip)
}


def doc = project.modules.collect {
    def pom = new File(project.basedir, "${it}/pom.xml").text
    def nameStart = pom.indexOf('<name>') + '<name>'.length()
    def nameEnd = pom.indexOf('</name>')
    def descriptionStart = pom.indexOf('<description>') + '<description>'.length()
    def descriptionEnd = pom.indexOf('</description>')
    def name = pom.substring(nameStart, nameEnd).replace('Component Runtime :: Sample Parent :: Documentation Samples :: ', '')
    def description = pom.substring(descriptionStart, descriptionEnd)
    def snapshotLink = "https://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=org.talend.sdk.component&a=documentation-sample&v=${project.version}&e=zip&c=${it}-distribution"
    def releaseLink = "http://repo.maven.apache.org/maven2/org/talend/sdk/component/documentation-sample/${project.version.replace('-SNAPSHOT', '')}/documentation-sample-${project.version.replace('-SNAPSHOT', '')}-${it}-distribution.zip"

    """
    |ifeval::["{page-origin-refname}" == "master"]
    |- link:${snapshotLink}[$name]: ${description}
    |endif::[]
    |ifeval::["{page-origin-refname}" != "master"]
    |- link:${releaseLink}[$name]: ${description}
    |endif::[]
    """.stripMargin().stripIndent().trim()
}.toSorted().join('\n')

new File(project.basedir, '../../documentation/src/main/antora/modules/ROOT/pages/_partials/generated_sample-index.adoc')
    .text = doc
