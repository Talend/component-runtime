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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ArtifactTest {

    static Iterable<Artifact> samples() {
        return asList(new Artifact("g", "a", "jar", null, "1", "compile"),
                new Artifact("g", "a", "jar", "c", "1", "compile"),
                new Artifact("g", "a", "jar", "c", "1-TCOMP-2285", "compile"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    void toCoordinateFrom(final Artifact artifact) {
        assertEquals(artifact, Artifact.from(artifact.toCoordinate()));
    }
}
