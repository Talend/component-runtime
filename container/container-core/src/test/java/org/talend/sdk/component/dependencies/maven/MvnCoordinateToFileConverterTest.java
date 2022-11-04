/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Note. Not all cases are covered!
 */
class MvnCoordinateToFileConverterTest {

    public static final String GROUP_ID = "org.talend.sdk.component";

    public static final String ARTIFACT_ID = "container-core";

    public static final String TYPE = "jar";

    public static final String VERSION = "1.42.0";

    public static final String VERSION_JIRA = "1.42.0-TCOMP-2285";

    public static final String VERSION_JIRA_SNAPSHOT = "1.42.0-TCOMP-2285-SNAPSHOT";

    public static final String CLASSIFIER = "osx-aarch_64";

    public static final String SCOPE = "compile";

    @Test
    void coordinateGATV() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter.toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + TYPE + ":" + VERSION);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertNull(artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateGAV() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter.toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertNull(artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateGATCV() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter
                .toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + TYPE + ":" + CLASSIFIER + ":" + VERSION);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertEquals(CLASSIFIER, artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateGATVS() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter
                .toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + TYPE + ":" + VERSION + ":" + SCOPE);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertNull(artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateGATCVS() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter
                .toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + TYPE + ":" + CLASSIFIER + ":" + VERSION + ":" + SCOPE);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertEquals(CLASSIFIER, artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateLessThen3() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> converter
                        .toArtifact(GROUP_ID + ":" + ARTIFACT_ID));
    }

    @Test
    void coordinateEmpty() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        Assertions.assertNull(converter.toArtifact(""));
    }

    @Test
    void coordinateGAVMvnPrefix() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        // I hope was meant dependency:tree output (-OZ)
        final Artifact artifact = converter.toArtifact(" +- " + GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertNull(artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateGATCVSWithJira() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter
                .toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + TYPE + ":" + CLASSIFIER + ":" + VERSION_JIRA + ":"
                        + SCOPE);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertEquals(CLASSIFIER, artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION_JIRA, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

    @Test
    void coordinateGATCVSWithJiraSnapshot() {
        final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
        final Artifact artifact = converter
                .toArtifact(GROUP_ID + ":" + ARTIFACT_ID + ":" + TYPE + ":" + CLASSIFIER + ":" + VERSION_JIRA_SNAPSHOT
                        + ":" + SCOPE);

        Assertions.assertNotNull(artifact);
        Assertions.assertEquals(GROUP_ID, artifact.getGroup());
        Assertions.assertEquals(ARTIFACT_ID, artifact.getArtifact());
        Assertions.assertEquals(CLASSIFIER, artifact.getClassifier());
        Assertions.assertEquals(TYPE, artifact.getType());
        Assertions.assertEquals(VERSION_JIRA_SNAPSHOT, artifact.getVersion());
        Assertions.assertEquals(SCOPE, artifact.getScope());
    }

}
