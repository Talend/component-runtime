/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Dependency {

    private static final Dependency JUNIT = new Dependency("junit", "junit", "4.13.1", "test");

    private final String group;

    private final String artifact;

    private final String version;

    private final String scope;

    private final String type;

    private final String classifier;

    public Dependency(final String group, final String artifact, final String version, final String scope) {
        this(group, artifact, version, scope, null, null);
    }

    public Dependency(final Dependency source, final String newScope) {
        this(source.getGroup(), source.getArtifact(), source.getVersion(), newScope, source.getType(),
                source.getClassifier());
    }

    public static Dependency componentApi(final String version) {
        return new Dependency("org.talend.sdk.component", "component-api", version, "provided");
    }

    public static Dependency junit() {
        return JUNIT;
    }
}
