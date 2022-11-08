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
package org.talend.sdk.component.maven;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.talend.sdk.component.maven.api.Audience;

@Audience(TALEND_INTERNAL)
public abstract class CarConsumer extends ComponentDependenciesBase {

    @Parameter(defaultValue = "component", property = "talend.repository.classifier")
    protected String classifier;

    protected Set<Artifact> getComponentsCar(final Set<Artifact> artifacts) {
        return artifacts.stream().map(art -> resolve(art, classifier, "car")).collect(toSet());
    }

    protected Set<Artifact> getComponentArtifacts() {
        return getArtifacts(it -> {
            try (final JarFile file = new JarFile(it.getFile())) { // filter components with this marker
                return ofNullable(file.getEntry("TALEND-INF/dependencies.txt")).map(ok -> it).orElse(null);
            } catch (final IOException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(toSet());
    }
}
