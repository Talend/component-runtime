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
package org.talend.sdk.component.dependencies.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MvnDependenciesTxtArtifactConverterTest {

    private final MvnDependencyListLocalRepositoryResolver.MvnDependenciesTxtArtifactConverter converter =
            new MvnDependencyListLocalRepositoryResolver.MvnDependenciesTxtArtifactConverter(
                    new MvnCoordinateToFileConverter());

    @Test
    void mvnDependencyOutput() {
        final Artifact[] artifacts = converter
                .withContent(
                        " \n The following files have been resolved:\norg.apache.tomee:ziplock:jar:8.0.14:runtime\n")
                .build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "8.0.14", "jar", "runtime", null);
    }

    @Test
    void simpleList() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:jar:8.0.14:runtime").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "8.0.14", "jar", "runtime", null);
    }

    @Test
    void shortArtifact() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:8.0.14").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "8.0.14", "jar", "compile", null);
    }

    @Test
    void noScopeArtifact() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:jar:8.0.14").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "8.0.14", "jar", "compile", null);
    }

    @Test
    void tree() {
        final Artifact[] artifacts = converter.withContent("└─ org.apache.tomee:ziplock:8.0.14").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "8.0.14", "jar", "compile", null);
    }

    @Test
    void shortArtifactJira() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:8.0.14-TCOMP-2285").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "8.0.14-TCOMP-2285", "jar", "compile", null);
    }

    private void assertArtifact(final Artifact art, final String group, final String artifact, final String version,
            final String type, final String scope, final String classifier) {
        assertEquals(group, art.getGroup());
        assertEquals(artifact, art.getArtifact());
        assertEquals(version, art.getVersion());
        assertEquals(classifier, art.getClassifier());
        assertEquals(type, art.getType());
        assertEquals(scope, art.getScope());
    }
}
