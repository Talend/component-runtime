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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

// super light maven resolver, actually just a coordinate file converter
@Data
@Slf4j
public class MvnCoordinateToFileConverter {

    private static final Set<String> SCOPES =
            new HashSet<>(Arrays.asList("compile", "provided", "runtime", "test", "system"));

    public Artifact toArtifact(final String coordinates) {
        String trim = coordinates
                .trim()
                // maven with Java Module System (>= 9) support
                .replaceAll(" -- module.*", "");
        if (trim.isEmpty()) {
            return null;
        }

        final int endOfTreePrefix = Math.max(trim.indexOf("─ "/* sbt */), trim.indexOf("- "/* mvn */));
        if (endOfTreePrefix > 0) {
            trim = trim.substring(endOfTreePrefix + "─ ".length());
        }

        String[] segments = trim.split(":");
        if (segments.length < 3) {
            throw new IllegalArgumentException("Invalid coordinate: " + trim);
        }

        switch (segments.length) {
        case 3:
            // g:a:v
            segments = new String[] { segments[0], segments[1], "jar", null, segments[2], "compile" };
            break;
        case 4:
            // g:a:t:v
            segments = new String[] { segments[0], segments[1], segments[2], null, segments[3], "compile" };
            break;
        case 5:
            if (SCOPES.contains(segments[4])) {
                // g:a:t:v:s
                segments = new String[] { segments[0], segments[1], segments[2], null, segments[3], segments[4] };
            } else {
                // g:a:t:c:v
                segments = new String[] { segments[0], segments[1], segments[2], segments[3], segments[4], "compile" };
            }
            break;
        default:
        }

        // group:artifact:type[:classifier]:version:scope
        return new Artifact(segments[0], segments[1], segments[2], segments[3], segments[4], segments[5]);
    }
}
