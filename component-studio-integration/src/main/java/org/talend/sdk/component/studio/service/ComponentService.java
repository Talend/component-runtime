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
package org.talend.sdk.component.studio.service;

import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.sdk.component.server.front.model.Icon;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.ComponentModel;
import org.talend.sdk.component.studio.model.parameter.Metadatas;

import lombok.AllArgsConstructor;
import lombok.Data;

public class ComponentService {

    private static final ImageDescriptor DEFAULT_IMAGE = ImageProvider.getImageDesc(EImage.COMPONENT_MISSING);

    private volatile Dependencies dependencies;

    // a @ConfigurationType is directly stored into the metadata without any prefix.
    // for now whitelist the support types and ensure it works all the way along
    // before just checking it doesn't contain "::"
    public boolean isConfiguration(final SimplePropertyDefinition prop) {
        return prop.getMetadata().containsKey(Metadatas.CONFIG_TYPE);
    }

    public ImageDescriptor toEclipseIcon(final Icon componentIcon) {
        if (componentIcon == null) {
            return DEFAULT_IMAGE;
        }

        // component-server return byte[] for both: custom icon and preinstalled
        if (componentIcon.getCustomIcon() != null) {
            try (final InputStream in = new ByteArrayInputStream(componentIcon.getCustomIcon())) {
                return ImageDescriptor.createFromImageData(new ImageData(in));
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
            // TODO deadcode. Remove it
        } else {

            final ClassLoader loader = ComponentModel.class.getClassLoader();
            final String icon = componentIcon.getIcon();
            return Stream
                    .of(icon + "_icon32.png", "icons/" + icon + "_icon32.png")
                    .map(pattern -> String.format(pattern, icon))
                    .map(loader::getResourceAsStream)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(in -> ImageDescriptor.createFromImageData(new ImageData(in)))
                    .orElse(DEFAULT_IMAGE);
        }
    }

    public Dependencies getDependencies() {
        if (dependencies == null) {
            synchronized (this) {
                if (dependencies == null) {
                    dependencies = new Dependencies(readDependencies("tacokit"), readDependencies("beam"));
                }
            }
        }
        return dependencies;
    }

    private Set<String> readDependencies(final String name) {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                ComponentModel.class.getClassLoader().getResourceAsStream("TALEND-INF/" + name + ".dependencies")))) {
            return reader.lines().map(String::trim).filter(s -> !s.isEmpty()).collect(toSet());
        } catch (final IOException e) {
            throw new IllegalStateException("No TALEND-INF/tacokit.dependencies found");
        }
    }

    @Data
    @AllArgsConstructor(access = PRIVATE)
    public static class Dependencies {

        private final Collection<String> common;

        private final Collection<String> beam;
    }
}
