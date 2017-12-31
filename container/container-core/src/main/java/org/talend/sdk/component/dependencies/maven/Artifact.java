/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;

import lombok.Data;

@Data
public class Artifact {

    private final String group;

    private final String artifact;

    private final String type;

    private final String classifier;

    private final String version;

    private final String scope;

    public String toPath() {
        return String.format("%s/%s/%s/%s-%s%s.%s", group.replace(".", "/"), artifact, version, artifact, version,
                ofNullable(classifier).map(c -> '-' + c).orElse(""), type);
    }

    public String toCoordinate() {
        return group + ':' + artifact + ':' + type
                + (classifier != null && !classifier.isEmpty() ? ':' + classifier : "") + ':' + version;
    }

    // symmetric from toCoordinate()
    public static Artifact from(final String id) {
        final String[] split = id.split(":");
        return new Artifact(split[0], split.length >= 2 ? split[1] : null, split.length >= 3 ? split[2] : "jar",
                split.length == 5 ? split[3] : null,
                split.length == 4 ? split[3] : (split.length == 5 ? split[4] : null), "compile");
    }
}
