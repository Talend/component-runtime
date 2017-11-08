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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.talend.core.model.components.EComponentType;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.designer.core.model.components.AbstractBasicComponent;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.studio.service.ComponentService;

// TODO: finish the impl
public class ComponentModel extends AbstractBasicComponent {

    private final ComponentService service;

    private final ComponentIndex index;

    private final ImageDescriptor image;

    private final ImageDescriptor image24;

    private final ImageDescriptor image16;
    
    /**
     * All palette entries for component joined by "|"
     * Component palette entry is computed as category + "/" + familyName
     * E.g. "Business/Salesforce|Cloud/Salesforce", where "Business", "Cloud" are categories,
     * "Salesforce" - is familyName
     */
    private final String familyName;

    public ComponentModel(final ComponentIndex component, final ComponentService service) {
        this.service = service;
        this.index = component;
        this.image = service.toEclipseIcon(component.getIcon());
        this.image24 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(24, 24));
        this.image16 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(16, 16));
        this.familyName = computeFamilyName();
    }
    
    ComponentModel(final ComponentIndex component) {
        this.service = null;
        this.index = component;
        this.image = null;
        this.image24 = null;
        this.image16 = null;
        this.familyName = computeFamilyName();
    }
    
    private String computeFamilyName() {
        return index.getCategories().stream()
                .map(category -> category + "/" + index.getId().getFamily())
                .collect(Collectors.joining("|"));
    }

    /**
     * @return component name (e.g. "tSalesforceInput")
     */
    @Override
    public String getName() {
        return index.getId().getName();
    }

    /**
     * Looks the same as {@link #getName()}
     */
    @Override
    public String getOriginalName() {
        return getName();
    }

    /**
     * Returns long component name, aka title (e.g. "Salesforce Input"). It is i18n title.
     * In v0 component it is specified by "component.{compName}.title" message key
     * 
     * @return long component name, aka title (e.g. "") or translated
     */
    @Override
    public String getLongName() {
        return getName();
    }

    /**
     * Returns string which is concatenation of all component palette entries
     * Component palette entry is computed as category + "/" + familyName
     * E.g. "Business/Salesforce|Cloud/Salesforce"
     * 
     * @return all palette entries for this component
     */
    @Override
    public String getOriginalFamilyName() {
        return familyName;
    }

    /**
     * Returns i18n family names
     */
    @Override
    public String getTranslatedFamilyName() {
        return getOriginalFamilyName();
    }

    /**
     * Returns short component name, which is obtained in following way
     * All capital letters are picked and converted to lower case
     * E.g. the short name for "tSalesforceInput" is "si"
     * 
     * @return short component name
     */
    @Override
    public String getShortName() {
        return getName();
    }

    /**
     * @return original component icon
     */
    @Override
    public ImageDescriptor getIcon32() {
        return image;
    }

    /**
     * @return component icon scaled to 24x24
     */
    @Override
    public ImageDescriptor getIcon24() {
        return image24;
    }

    /**
     * @return component icon scaled to 16x16
     */
    @Override
    public ImageDescriptor getIcon16() {
        return image16;
    }

    /**
     * Creates component parameters aka Properties/Configuration
     */
    @Override // TODO
    public List<? extends IElementParameter> createElementParameters(final INode iNode) {
        return emptyList();
    }

    /**
     * Creates component return properties
     */
    @Override // TODO
    public List<? extends INodeReturn> createReturns(final INode iNode) {
        return emptyList();
    }

    /**
     * Creates component connectors including "ITERATE", "ON_COMPONENT_OK" etc 
     */
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

    /**
     * Get the default modules needed for the component.
     * @return common v1 components Job dependencies
     */
    @Override // TODO
    public List<ModuleNeeded> getModulesNeeded() {
        return emptyList();
    }

    /**
     * Get the modules needed according component configuration
     * This method should no have sense for v1 as Job classpath should contain only common API dependencies
     * All component specific dependencies will be resolved by ComponentManager class
     * 
     * @return
     */
    @Override // TODO
    public List<ModuleNeeded> getModulesNeeded(final INode iNode) {
        return emptyList();
    }

    /**
     * Returns code parts (.javajet templates) for this component.
     * All v1 component should have the same set of common templates
     */
    @Override // TODO
    public List<ECodePart> getAvailableCodeParts() {
        return emptyList();
    }

    /**
     * Returns component type, which is EComponentType.GENERIC
     */
    @Override // TODO
    public EComponentType getComponentType() {
        return EComponentType.GENERIC;
    }
}
