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
import static org.talend.core.model.process.EConnectionType.FLOW_MAIN;
import static org.talend.core.model.process.EConnectionType.FLOW_MERGE;
import static org.talend.core.model.process.EConnectionType.FLOW_REF;
import static org.talend.core.model.process.EConnectionType.ON_COMPONENT_ERROR;
import static org.talend.core.model.process.EConnectionType.ON_COMPONENT_OK;
import static org.talend.core.model.process.EConnectionType.ON_SUBJOB_ERROR;
import static org.talend.core.model.process.EConnectionType.ON_SUBJOB_OK;
import static org.talend.core.model.process.EConnectionType.REJECT;
import static org.talend.core.model.process.EConnectionType.RUN_IF;
import static org.talend.sdk.component.studio.model.ReturnVariables.AFTER;
import static org.talend.sdk.component.studio.model.ReturnVariables.RETURN_ERROR_MESSAGE;
import static org.talend.sdk.component.studio.model.ReturnVariables.RETURN_TOTAL_RECORD_COUNT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.talend.core.model.components.EComponentType;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.metadata.types.JavaTypesManager;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.core.runtime.util.ComponentReturnVariableUtils;
import org.talend.designer.core.model.components.AbstractBasicComponent;
import org.talend.designer.core.model.components.NodeConnector;
import org.talend.designer.core.model.components.NodeReturn;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.studio.model.ElementParameterCreator;

// TODO: finish the impl
public class ComponentModel extends AbstractBasicComponent {

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

    public ComponentModel(final ComponentIndex component, final ImageDescriptor image32) {
        setPaletteType("DI");
        this.index = component;
        this.familyName = computeFamilyName();
        this.codePartListX = createCodePartList();
        this.image = image32;
        this.image24 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(24, 24));
        this.image16 = ImageDescriptor.createFromImageData(image.getImageData().scaledTo(16, 16));
    }

    ComponentModel(final ComponentIndex component) {
        setPaletteType("DI");
        this.index = component;
        this.familyName = computeFamilyName();
        this.codePartListX = createCodePartList();
        this.image = null;
        this.image24 = null;
        this.image16 = null;
        createCodePartList();
    }

    /**
     * TODO change to StringBuilder impl? Seems, here StringBuilder instance is created per category
     */
    private String computeFamilyName() {
        return index.getCategories().stream().map(category -> category + "/" + index.getId().getFamily())
                .collect(Collectors.joining("|"));
    }

    /**
     * Creates unmodifiable list of code part templates (.javajet)
     * All Tacokit component have following 4 parts by default:
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
        return Collections.unmodifiableList(Arrays.asList(ECodePart.BEGIN, ECodePart.MAIN, ECodePart.END, ECodePart.FINALLY));
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
        return index.getDisplayName();
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
    @Override // TODO This is dummy implementation. Correct impl should be added soon
    public List<? extends IElementParameter> createElementParameters(final INode node) {
        return new ElementParameterCreator(this, node).createParameters();
    }

    /**
     * Creates component return variables
     * For the moment it returns only ERROR_MESSAGE and NB_LINE after variables
     * 
     * @return list of component return variables
     */
    @Override
    public List<? extends INodeReturn> createReturns(final INode iNode) {
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
        numberLinesMessage.setDisplayName(
                ComponentReturnVariableUtils.getTranslationForVariable(RETURN_TOTAL_RECORD_COUNT, RETURN_TOTAL_RECORD_COUNT));
        numberLinesMessage.setName(ComponentReturnVariableUtils.getStudioNameFromVariable(RETURN_TOTAL_RECORD_COUNT));
        numberLinesMessage.setAvailability(AFTER);
        returnVariables.add(numberLinesMessage);

        return returnVariables;
    }

    /**
     * Creates component connectors including "ITERATE", "ON_COMPONENT_OK" etc
     * TODO It is stub implementation. It creates connectors based on OUTGOING topology (i.e. for input component)
     * 
     * @param node component node - object representing component instance on design canvas
     */
    @Override
    public List<? extends INodeConnector> createConnectors(final INode node) {
        ArrayList<INodeConnector> connectors = new ArrayList<>();

        // create connectors for Input component
        INodeConnector main = creatInputConnector(FLOW_MAIN, node);
        // see org.talend.designer.core.generic.model.Component.createConnectors(INode)
        main.addConnectionProperty(FLOW_REF, FLOW_REF.getRGB(), FLOW_REF.getDefaultLineStyle());
        main.addConnectionProperty(FLOW_MERGE, FLOW_MERGE.getRGB(), FLOW_MERGE.getDefaultLineStyle());

        connectors.add(main);
        connectors.add(creatInputConnector(REJECT, node));
        connectors.add(createIterateConnector(node));

        // create Standard
        connectors.add(createConnector(RUN_IF, node));
        connectors.add(createConnector(ON_COMPONENT_OK, node));
        connectors.add(createConnector(ON_COMPONENT_ERROR, node));
        connectors.add(createConnector(ON_SUBJOB_OK, node));
        connectors.add(createConnector(ON_SUBJOB_ERROR, node));

        // create others
        Set<EConnectionType> existingConnectors = connectors.stream() //
                .map(connector -> connector.getDefaultConnectionType()) //
                .collect(Collectors.toSet()); //

        Arrays.stream(EConnectionType.values()) //
                .filter(type -> !existingConnectors.contains(type)) //
                .forEach(type -> { //
                    INodeConnector connector = createOtherConnector(type, node); //
                    if ((type == EConnectionType.PARALLELIZE) || (type == EConnectionType.SYNCHRONIZE)) { //
                        connector.setMaxLinkInput(1); //
                    } //
                    connectors.add(connector); //
                });
        return connectors;
    }

    /**
     * Creates connector with infinite (-1) incoming and outgoing connections
     *
     * @param type connector type
     * @param node component node - object representing component instance on design canvas
     * @return connector
     */
    private INodeConnector createConnector(EConnectionType type, INode node) {
        NodeConnector nodeConnector = new NodeConnector(node);
        nodeConnector.setName(type.getName());
        nodeConnector.setBaseSchema(type.getName());
        nodeConnector.setDefaultConnectionType(type);
        nodeConnector.setLinkName(type.getDefaultLinkName());
        nodeConnector.setMenuName(type.getDefaultMenuName());
        nodeConnector.addConnectionProperty(type, type.getRGB(), type.getDefaultLineStyle());
        return nodeConnector;
    }

    /**
     * Creates connector with 0 incoming and 1 outgoing connections
     * 
     * @param type connector type
     * @param node component node - object representing component instance on design canvas
     * @return connector
     */
    private INodeConnector creatInputConnector(EConnectionType type, INode node) {
        INodeConnector connector = createConnector(type, node);
        connector.setMinLinkInput(0);
        connector.setMaxLinkInput(0);
        connector.setMinLinkOutput(0);
        connector.setMaxLinkOutput(1);
        return connector;
    }

    /**
     * Creates connector which is not applicable for Tacokit component.
     * Thus, such connector has 0 incoming and 0 outgoing connections
     * 
     * @param type connector type
     * @param node component node - object representing component instance on design canvas
     * @return connector
     */
    private INodeConnector createOtherConnector(EConnectionType type, INode node) {
        INodeConnector connector = createConnector(type, node);
        connector.setMinLinkInput(0);
        connector.setMaxLinkInput(0);
        connector.setMinLinkOutput(0);
        connector.setMaxLinkOutput(0);
        return connector;
    }

    /**
     * Creates {@link EConnectionType#ITERATE} connector for outgoing topology (i.e. for input component)
     * Input components have 1 incoming iterate connections and infinite outgoing connections
     * 
     * @param node node component node - object representing component instance on design canvas
     * @return iterate connector
     */
    private INodeConnector createIterateConnector(INode node) {
        INodeConnector iterate = createConnector(EConnectionType.ITERATE, node);
        iterate.setMinLinkInput(0);
        iterate.setMaxLinkInput(1);
        iterate.setMinLinkOutput(0);
        iterate.setMaxLinkOutput(-1);
        return iterate;
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
     * All Tacokit componenta have same set of common templates:
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
}
