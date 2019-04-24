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

def sanitizePom(path) {
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
      </extension>""")
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
    Files.walkFileTree(module, new SimpleFileVisitor<Path>() {
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
    })
    jar.close()
    projectHelper.attachArtifact(project, 'zip', "${it}-distribution", zip)
}
