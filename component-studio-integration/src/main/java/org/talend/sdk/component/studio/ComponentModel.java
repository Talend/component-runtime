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

import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.studio.model.ReturnVariables.AFTER;
import static org.talend.sdk.component.studio.model.ReturnVariables.RETURN_ERROR_MESSAGE;
import static org.talend.sdk.component.studio.model.ReturnVariables.RETURN_TOTAL_RECORD_COUNT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.talend.core.model.components.EComponentType;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.metadata.types.JavaTypesManager;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.core.runtime.util.ComponentReturnVariableUtils;
import org.talend.designer.core.model.components.AbstractBasicComponent;
import org.talend.designer.core.model.components.NodeReturn;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.studio.model.connector.ConnectorCreatorFactory;
import org.talend.sdk.component.studio.model.parameter.ElementParameterCreator;

// TODO: finish the impl
public class ComponentModel extends AbstractBasicComponent {

    /**
     * Separator between family and component name
     */
    private static final String COMPONENT_SEPARATOR = "";

    private final ComponentIndex index;

    private final ComponentDetail detail;

    private final ImageDescriptor image;

    private final ImageDescriptor image24;

    private final ImageDescriptor image16;

    /**
     * All palette entries for component joined by "|" Component palette entry is
     * computed as category + "/" + familyName E.g.
     * "Business/Salesforce|Cloud/Salesforce", where "Business", "Cloud" are
     * categories, "Salesforce" - is familyName
     */
    private final String familyName;

    private volatile List<ModuleNeeded> modulesNeeded;

    public ComponentModel(final ComponentIndex component, final ComponentDetail detail, final ImageDescriptor image32) {
        setPaletteType("DI");
        this.index = component;
        this.detail = detail;
        this.familyName = computeFamilyName();
        this.codePartListX = createCodePartList();
        this.image = image32;
        this.image24 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(24, 24));
        this.image16 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(16, 16));
    }

    ComponentModel(final ComponentIndex component, final ComponentDetail detail) {
        setPaletteType("DI");
        this.index = component;
        this.detail = detail;
        this.familyName = computeFamilyName();
        this.codePartListX = createCodePartList();
        this.image = null;
        this.image24 = null;
        this.image16 = null;
        createCodePartList();
    }

    /**
     * TODO change to StringBuilder impl? Seems, here StringBuilder instance is
     * created per category
     */
    private String computeFamilyName() {
        return index.getCategories().stream().map(category -> category + "/" + index.getId().getFamily()).collect(
                Collectors.joining("|"));
    }

    /**
     * Creates unmodifiable list of code part templates (.javajet) All Tacokit
     * component have following 4 parts by default:
     * <ul>
     * <li>BEGIN</li>
     * <li>MAIN</li>
     * <li>END</li>
     * <li>FINALLYS</li>
     * </ul>
     * 
     * @return
     */
    private List<ECodePart> createCodePartList() {
        return (detail.getType().equalsIgnoreCase("input"))
                ? Collections.unmodifiableList(Arrays.asList(ECodePart.BEGIN, ECodePart.END, ECodePart.FINALLY))
                : Collections.unmodifiableList(Arrays.asList(ECodePart.BEGIN, ECodePart.MAIN, ECodePart.FINALLY));
    }

    /**
     * @return component name (e.g. "tSalesforceInput")
     */
    @Override
    public String getName() {
        return index.getId().getFamily() + COMPONENT_SEPARATOR + index.getId().getName();
    }

    /**
     * Looks the same as {@link #getName()}
     */
    @Override
    public String getOriginalName() {
        return getName();
    }

    /**
     * Returns long component name, aka title (e.g. "Salesforce Input"). It is i18n
     * title. In v0 component it is specified by "component.{compName}.title"
     * message key
     * 
     * @return long component name, aka title (e.g. "") or translated
     */
    @Override
    public String getLongName() {
        return index.getDisplayName();
    }

    /**
     * Returns string which is concatenation of all component palette entries
     * Component palette entry is computed as category + "/" + familyName E.g.
     * "Business/Salesforce|Cloud/Salesforce"
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
     * Returns short component name, which is obtained in following way All capital
     * letters are picked and converted to lower case E.g. the short name for
     * "tSalesforceInput" is "si"
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
     * Returns Component configuration version
     * When Configuration is changed its version should be incremented and corresponding MigrationHandler implemented to
     * migrate from
     * older versions to newer during deserialization
     */
    @Override
    public String getVersion() {
        return Integer.toString(detail.getVersion());
    }

    /**
     * Creates component parameters aka Properties/Configuration
     */
    @Override // TODO This is dummy implementation. Correct impl should be added soon
    public List<? extends IElementParameter> createElementParameters(final INode node) {
        ElementParameterCreator creator = new ElementParameterCreator(this, detail, node);
        List<IElementParameter> parameters = (List<IElementParameter>) creator.createParameters();
        return parameters;
    }

    /**
     * Creates component return variables For the moment it returns only
     * ERROR_MESSAGE and NB_LINE after variables
     * 
     * @return list of component return variables
     */
    @Override
    public List<? extends INodeReturn> createReturns(final INode node) {
        List<NodeReturn> returnVariables = new ArrayList<>();

        NodeReturn errorMessage = new NodeReturn();
        errorMessage.setType(JavaTypesManager.STRING.getLabel());
        errorMessage.setDisplayName(
                ComponentReturnVariableUtils.getTranslationForVariable(RETURN_ERROR_MESSAGE, RETURN_ERROR_MESSAGE));
        errorMessage.setName(ComponentReturnVariableUtils.getStudioNameFromVariable(RETURN_ERROR_MESSAGE));
        errorMessage.setAvailability(AFTER);
        returnVariables.add(errorMessage);

        NodeReturn numberLinesMessage = new NodeReturn();
        numberLinesMessage.setType(JavaTypesManager.INTEGER.getLabel());
        numberLinesMessage.setDisplayName(ComponentReturnVariableUtils
                .getTranslationForVariable(RETURN_TOTAL_RECORD_COUNT, RETURN_TOTAL_RECORD_COUNT));
        numberLinesMessage.setName(ComponentReturnVariableUtils.getStudioNameFromVariable(RETURN_TOTAL_RECORD_COUNT));
        numberLinesMessage.setAvailability(AFTER);
        returnVariables.add(numberLinesMessage);

        return returnVariables;
    }

    /**
     * Creates component connectors. It creates all possible connector even if some
     * of them are not applicable for component. In such cases not applicable
     * connector has 0 outgoing and incoming links.
     * 
     * @param node
     * component node - object representing component instance on design
     * canvas
     */
    @Override
    public List<? extends INodeConnector> createConnectors(final INode node) {
        return ConnectorCreatorFactory.create(detail, node).createConnectors();
    }

    /**
     * TODO decide about API for this feature
     */
    @Override
    public boolean isSchemaAutoPropagated() {
        return true;
    }

    /**
     * TODO decide about API for this feature
     */
    @Override
    public boolean isDataAutoPropagated() {
        return true;
    }

    /**
     * Get the default modules needed for the component.
     * 
     * @return common v1 components Job dependencies
     */
    @Override
    public List<ModuleNeeded> getModulesNeeded() {
        return getModulesNeeded(null);
    }

    /**
     * Get the modules needed according component configuration This method should
     * no have sense for v1 as Job classpath should contain only common API
     * dependencies All component specific dependencies will be resolved by
     * ComponentManager class
     * 
     * @return the needed dependencies for the framework,
     * component dependencies are loaded later through ComponentManager.
     */
    @Override
    public List<ModuleNeeded> getModulesNeeded(final INode iNode) {
        if (modulesNeeded == null) {
            synchronized (this) {
                if (modulesNeeded == null) {
                    modulesNeeded = new ArrayList<>();
                    try (final BufferedReader reader = new BufferedReader(
                            new InputStreamReader(ComponentModel.class.getClassLoader().getResourceAsStream(
                                    "TALEND-INF/tacokit.dependencies")))) {
                        modulesNeeded.addAll(reader
                                .lines()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(s -> new ModuleNeeded(getName(), "", true, s))
                                .collect(toList()));
                    } catch (final IOException e) {
                        throw new IllegalStateException("No TALEND-INF/tacokit.dependencies found");
                    }
                    modulesNeeded.add(new ModuleNeeded(getName(), "", true,
                            "mvn:org.talend.sdk.component/component-runtime-di/" + GAV.VERSION));
                    modulesNeeded.add(new ModuleNeeded(getName(), "", true, "mvn:org.slf4j/slf4j-api/1.7.12"));
                    modulesNeeded.add(new ModuleNeeded(getName(), "", true,
                            "mvn:org.talend.libraries/slf4j-log4j12-1.7.2/6.0.0"));
                }
            }
        }
        return modulesNeeded;
    }

    /**
     * Returns code parts (.javajet templates) for this component. All Tacokit
     * componenta have same set of common templates:
     * <ul>
     * <li>BEGIN</li>
     * <li>MAIN</li>
     * <li>END</li>
     * <li>FINALLYS</li>
     * </ul>
     */
    @Override
    public List<ECodePart> getAvailableCodeParts() {
        return this.codePartListX;
    }

    /**
     * Returns component type, which is EComponentType.GENERIC
     */
    @Override
    public EComponentType getComponentType() {
        return EComponentType.GENERIC;
    }

    public String getTemplateFolder() {
        return "tacokit/jet_stub/generic/" + detail.getType().toLowerCase();
    }

    public String getTemplateNamePrefix() {
        return detail.getType().toLowerCase();
    }

}
