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
package org.talend.sdk.component.runtime.beam.customizer;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.manager.ComponentManager;

public class BeamCustomizer implements ComponentManager.Customizer {

    private Collection<ComponentManager.Customizer> all;

    @Override
    public void setCustomizers(final Collection<ComponentManager.Customizer> customizers) {
        all = customizers;
    }

    @Override
    public Stream<String> containerClassesAndPackages() {
        return all.stream().anyMatch(ComponentManager.Customizer::ignoreBeamClassLoaderExclusions) ? Stream.empty()
                : loadIndex().stream();
    }

    private Collection<String> loadIndex() {
        return Stream.of(Indices.values()).filter(Indices::isAvailable).flatMap(Indices::getClasses).collect(toSet());
    }
}
