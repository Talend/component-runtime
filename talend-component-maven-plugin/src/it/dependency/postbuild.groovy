/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.nio.file.Path
import java.nio.file.Paths

import static groovy.io.FileType.FILES
import static java.nio.file.Files.exists

/**
 * Checks that the IT on mojo prepare-repository ran smoothly
 */
static def checkPrepareRepositoryMojo(final File basedir, final String componentVersion, final String componentArtifactId,
                                      componentSnapshotVersion, componentSnapshotArtifactId) {
    final Path builtM2 = Paths.get(basedir.toString())
            .resolve('target/talend-component-kit')

    if (!exists(builtM2)) {
        throw new FileNotFoundException("Could not find built m2 ${builtM2}")
    }
    // check artifact using release versioning
    final Path componentJar = builtM2.resolve(
            "maven/org/talend/components/${componentArtifactId}/${componentVersion}/${componentArtifactId}-${componentVersion}.jar"
    )
    if (!exists(componentJar)) {
        throw new FileNotFoundException("Could not find component JAR ${componentJar}")
    }
    // check artifact using snapshot versioning
    final Path componentSnapshotJar = builtM2.resolve(
            "maven/org/talend/components/${componentSnapshotArtifactId}/${componentSnapshotVersion}/${componentSnapshotArtifactId}-${componentSnapshotVersion}.jar"
    )
    if (!exists(componentSnapshotJar)) {
        throw new FileNotFoundException("Could not find component JAR ${componentSnapshotJar}")
    }

    final List<String> allJars = []
    new File(builtM2.toAbsolutePath().toString())
            .traverse(type: FILES, nameFilter: ~/.*\.jar$/) { it ->
                allJars.add(it.getPath())
            }

    if (allJars.size() <= 1) {
        throw new IllegalStateException('Only the component jar was found with no dependency')
    }

    // check component-registry.properties generated file
    Properties properties = new Properties()
    File registry = builtM2.resolve("maven/component-registry.properties").toFile()
    registry.withInputStream {
        properties.load(it)
    }
    assert properties."${componentArtifactId}" == "org.talend.components:${componentArtifactId}:${componentVersion}"
    assert properties."${componentSnapshotArtifactId}" == "org.talend.components:${componentSnapshotArtifactId}:${componentSnapshotVersion}"
}

checkPrepareRepositoryMojo(basedir, componentVersion, componentArtifactId, componentSnapshotVersion,
        componentSnapshotArtifactId)
