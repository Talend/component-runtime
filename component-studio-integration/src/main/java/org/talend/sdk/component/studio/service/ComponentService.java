/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.sdk.component.server.front.model.Icon;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.ComponentModel;

public class ComponentService {

    private static final ImageDescriptor DEFAULT_IMAGE = ImageProvider.getImageDesc(EImage.COMPONENT_MISSING);

    private volatile Boolean linux;

    // a @ConfigurationType is directly stored into the metadata without any prefix.
    // for now whitelist the support types and ensure it works all the way along
    // before just checking it doesn't contain "::"
    public boolean isConfiguration(final SimplePropertyDefinition prop) {
        return prop.getMetadata().containsKey("dataset") || prop.getMetadata().containsKey("datastore");
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

    public boolean isLinux() {
        if (linux == null) {
            synchronized (this) {
                if (linux == null) {
                    linux = Platform.getOS().equals(Platform.OS_LINUX);
                }
            }
        }
        return linux;
    }
}
