package org.talend.components.dependencies.maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MvnDependenciesTxtArtifactConverterTest {

    private final MvnDependencyListLocalRepositoryResolver.MvnDependenciesTxtArtifactConverter converter = new MvnDependencyListLocalRepositoryResolver.MvnDependenciesTxtArtifactConverter(
            new MvnCoordinateToFileConverter());

    @Test
    public void mvnDependencyOutput() {
        final Artifact[] artifacts = converter
                .withContent(" \n The following files have been resolved:\norg.apache.tomee:ziplock:jar:7.0.3:runtime\n").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "7.0.3", "jar", "runtime", null);
    }

    @Test
    public void simpleList() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:jar:7.0.3:runtime").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "7.0.3", "jar", "runtime", null);
    }

    @Test
    public void shortArtifact() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:7.0.3").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "7.0.3", "jar", "compile", null);
    }

    @Test
    public void noScopeArtifact() {
        final Artifact[] artifacts = converter.withContent("org.apache.tomee:ziplock:jar:7.0.3").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "7.0.3", "jar", "compile", null);
    }

    @Test
    public void tree() {
        final Artifact[] artifacts = converter.withContent("└─ org.apache.tomee:ziplock:7.0.3").build();
        assertEquals(1, artifacts.length);
        assertArtifact(artifacts[0], "org.apache.tomee", "ziplock", "7.0.3", "jar", "compile", null);
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
