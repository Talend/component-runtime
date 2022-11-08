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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

// super light maven resolver, actually just a coordinate file converter
@Data
@Slf4j
public class MvnCoordinateToFileConverter {

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

        switch (segments.length) { // support some optional values 3: g:a:v, 4: g:a:t:v
        case 3:
            segments = new String[] { segments[0], segments[1], "jar", segments[2], "compile" };
            break;
        case 4:
            segments = (trim + ":compile").split(":");
            break;
        default:
        }

        // group:artifact:type[:classifier]:version:scope
        final int classifierOffset = segments.length == 5 ? 0 : 1;
        return new Artifact(segments[0], segments[1], segments[2], classifierOffset == 0 ? null : segments[3],
                segments[3 + classifierOffset], segments[4 + classifierOffset]);
    }
}
