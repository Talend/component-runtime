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
package org.talend.sdk.component.studio;

import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.model.components.EComponentType;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.designer.core.model.components.AbstractBasicComponent;
import org.talend.sdk.component.server.front.model.ComponentIndex;

// TODO: finish the impl
public class ComponentModel extends AbstractBasicComponent {

    private static final ImageDescriptor DEFAULT_IMAGE = ImageProvider.getImageDesc(EImage.COMPONENT_MISSING);

    private final ComponentIndex index;

    private final ImageDescriptor image;

    private final ImageDescriptor image24;

    private final ImageDescriptor image16;

    public ComponentModel(final ComponentIndex component) {
        index = component;
        if (component.getCustomIcon() != null) {
            try (final InputStream in = new ByteArrayInputStream(component.getCustomIcon())) {
                image = ImageDescriptor.createFromImageData(new ImageData(in));
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            if (component.getIcon() != null) {
                final ClassLoader loader = ComponentModel.class.getClassLoader();
                final String icon = component.getIcon();
                image = Stream.of(icon + "_icon32.png", "icons/" + icon + "_icon32.png")
                        .map(pattern -> String.format(pattern, icon)).map(loader::getResourceAsStream).filter(Objects::nonNull)
                        .findFirst().map(in -> ImageDescriptor.createFromImageData(new ImageData(in))).orElse(DEFAULT_IMAGE);
            } else {
                image = DEFAULT_IMAGE;
            }
        }
        image24 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(24, 24));
        image16 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(16, 16));
    }

    @Override
    public String getName() {
        return index.getId().getName();
    }

    @Override
    public String getOriginalName() {
        return getName();
    }

    @Override
    public String getLongName() {
        return getName();
    }

    @Override
    public String getOriginalFamilyName() {
        return index.getId().getFamily();
    }

    @Override
    public String getTranslatedFamilyName() {
        return getOriginalFamilyName();
    }

    @Override
    public String getShortName() {
        return getName();
    }

    @Override
    public ImageDescriptor getIcon32() {
        return image;
    }

    @Override
    public ImageDescriptor getIcon24() {
        return image24;
    }

    @Override
    public ImageDescriptor getIcon16() {
        return image16;
    }

    @Override // TODO
    public List<? extends IElementParameter> createElementParameters(final INode iNode) {
        return emptyList();
    }

    @Override // TODO
    public List<? extends INodeReturn> createReturns(final INode iNode) {
        return emptyList();
    }

    @Override // TODO
    public List<? extends INodeConnector> createConnectors(final INode iNode) {
        return emptyList();
    }

    @Override // TODO
    public boolean isSchemaAutoPropagated() {
        return false;
    }

    @Override // TODO
    public boolean isDataAutoPropagated() {
        return false;
    }

    @Override // TODO
    public List<ModuleNeeded> getModulesNeeded() {
        return emptyList();
    }

    @Override // TODO
    public List<ModuleNeeded> getModulesNeeded(final INode iNode) {
        return emptyList();
    }

    @Override // TODO
    public List<ECodePart> getAvailableCodeParts() {
        return emptyList();
    }

    @Override // TODO
    public EComponentType getComponentType() {
        return EComponentType.GENERIC;
    }
}
