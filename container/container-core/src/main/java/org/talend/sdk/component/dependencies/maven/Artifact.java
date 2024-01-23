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

import static java.util.Optional.ofNullable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(exclude = "pathCache")
public class Artifact {

    private final String group;

    private final String artifact;

    private final String type;

    private final String classifier;

    private final String version;

    private final String scope;

    private volatile String pathCache;

    public Artifact(final String group, final String artifact, final String type, final String classifier,
            final String version, final String scope) {
        this.group = group;
        this.artifact = artifact;
        this.type = type;
        this.classifier = classifier != null && classifier.isEmpty() ? null : classifier;
        this.version = version;
        this.scope = scope;
    }

    public String toPath() {
        if (pathCache == null) {
            synchronized (this) {
                if (pathCache == null) {
                    pathCache = String
                            .format("%s/%s/%s/%s-%s%s.%s", group.replace(".", "/"), artifact, version, artifact,
                                    version,
                                    ofNullable(classifier).filter(it -> !it.isEmpty()).map(c -> '-' + c).orElse(""),
                                    type);
                }
            }
        }
        return pathCache;
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
