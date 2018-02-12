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
package org.talend.sdk.component.studio;

import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.studio.model.ReturnVariables.AFTER;
import static org.talend.sdk.component.studio.model.ReturnVariables.RETURN_ERROR_MESSAGE;
import static org.talend.sdk.component.studio.model.ReturnVariables.RETURN_TOTAL_RECORD_COUNT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.PluginChecker;
import org.talend.core.model.components.ComponentCategory;
import org.talend.core.model.components.EComponentType;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.metadata.types.JavaTypesManager;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IConnectionCategory;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.core.runtime.IAdditionalInfo;
import org.talend.core.runtime.util.ComponentReturnVariableUtils;
import org.talend.designer.core.model.components.AbstractBasicComponent;
import org.talend.designer.core.model.components.NodeReturn;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.studio.model.connector.ConnectorCreatorFactory;
import org.talend.sdk.component.studio.model.connector.TaCoKitNodeConnector;
import org.talend.sdk.component.studio.model.parameter.ElementParameterCreator;
import org.talend.sdk.component.studio.model.parameter.Metadatas;
import org.talend.sdk.component.studio.service.ComponentService;

// TODO: finish the impl
public class ComponentModel extends AbstractBasicComponent implements IAdditionalInfo {

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

    private Map<String, Object> additionalInfoMap = new HashMap<>();

    private boolean useLookup = false;

    public ComponentModel(final ComponentIndex component, final ComponentDetail detail, final ImageDescriptor image32) {
        setPaletteType(ComponentCategory.CATEGORY_4_DI.getName());
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

    @Override // this is our binding of slf4j
    public boolean isLog4JEnabled() {
        return true;
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
        return (detail.getType().equalsIgnoreCase("input")) //$NON-NLS-1$
                ? Collections.unmodifiableList(Arrays.asList(ECodePart.BEGIN, ECodePart.END, ECodePart.FINALLY))
                : Collections.unmodifiableList(Arrays.asList(ECodePart.BEGIN, ECodePart.MAIN, ECodePart.END_HEAD,
                        ECodePart.END_BODY, ECodePart.END_TAIL, ECodePart.FINALLY));
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
        List<INodeConnector> connectors = ConnectorCreatorFactory.create(detail, node).createConnectors();
        if (connectors != null) {
            for (INodeConnector connector : connectors) {
                if (EConnectionType.FLOW_MAIN.equals(connector.getDefaultConnectionType())) {
                    if (1 < connector.getMaxLinkInput()) {
                        useLookup = true;
                        break;
                    }
                }
            }
        }
        return connectors;
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
        return false;
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
                    final ComponentService.Dependencies dependencies = Lookups.service().getDependencies();

                    modulesNeeded = new ArrayList<>(20);
                    modulesNeeded.addAll(dependencies
                            .getCommon()
                            .stream()
                            .map(s -> new ModuleNeeded(getName(), "", true, s))
                            .collect(toList()));
                    modulesNeeded.add(new ModuleNeeded(getName(), "", true,
                            "mvn:org.talend.sdk.component/component-runtime-di/" + GAV.VERSION));
                    modulesNeeded
                            .add(new ModuleNeeded(getName(), "", true, "mvn:org.slf4j/slf4j-api/" + GAV.SLF4J_VERSION));
                    if (!PluginChecker.isTIS()) {
                        modulesNeeded.add(new ModuleNeeded(getName(), "", true,
                                "mvn:" + GAV.GROUP_ID + "/slf4j-standard/" + GAV.VERSION));
                    } else {
                        modulesNeeded.add(new ModuleNeeded(getName(), "", true,
                                "mvn:org.slf4j/slf4j-log4j12/" + GAV.SLF4J_VERSION));
                    }

                    final Map<String, ?> transitiveDeps = !Lookups.configuration().isActive() ? null
                            : Lookups.client().v1().component().dependencies(detail.getId().getId());
                    if (transitiveDeps != null && transitiveDeps.containsKey("dependencies")) {
                        final Collection<String> coordinates = Collection.class.cast(Map.class
                                .cast(Map.class.cast(transitiveDeps.get("dependencies")).values().iterator().next())
                                .get("dependencies"));
                        if (coordinates != null && coordinates.stream().anyMatch(
                                d -> d.contains("org.apache.beam") || d.contains(":beam-sdks-java-io"))) {
                            modulesNeeded.addAll(dependencies
                                    .getBeam()
                                    .stream()
                                    .map(s -> new ModuleNeeded(getName(), "", true, s))
                                    .collect(toList()));
                            // transitivity works through pom
                        }
                    }

                    // We're assuming that pluginLocation has format of groupId:artifactId:version
                    final String location = index.getId().getPluginLocation().trim();
                    modulesNeeded.add(new ModuleNeeded(getName(), "", true, locationToMvn(location)));
                }
            }
        }
        return modulesNeeded;
    }

    private String locationToMvn(final String location) {
        String[] segments = location.split(":");
        if (segments.length < 3) {
            throw new IllegalArgumentException("Invalid coordinate: " + location);
        }

        switch (segments.length) { // support some optional values 3: g:a:v, 4: g:a:t:v
        case 3:
            segments = new String[] { segments[0], segments[1], "jar", segments[2], "compile" };
            break;
        case 4:
            segments = (location + ":compile").split(":");
            break;
        default:
        }

        // mvn:group/artifact/version/type[/classifier]
        final int classifierOffset = segments.length == 5 ? 0 : 1;
        return "mvn:" + segments[0] + "/" + segments[1] + "/" + segments[3 + classifierOffset] + "/" + segments[2]
                + ((classifierOffset == 0) ? "" : "/" + segments[3]);
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

    @Override
    public String getTemplateFolder() {
        return "tacokit/jet_stub/generic/" + detail.getType().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getTemplateNamePrefix() {
        return detail.getType().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Check whether current component can use the given configuration
     * 
     * @param familyNodeName family name
     * @param configType configuration type
     * @param configName configuration name
     * @return true if support, otherwise false
     */
    public boolean supports(final String familyNodeName, final String configType, final String configName) {
        Collection<SimplePropertyDefinition> properties = detail.getProperties();
        if (properties == null || properties.isEmpty()) {
            return false;
        }
        String expectedFamilyName = index.getId().getFamily();
        if (expectedFamilyName == null || !expectedFamilyName.equals(familyNodeName)) { // $NON-NLS-1$
            return false;
        }
        Iterator<SimplePropertyDefinition> iter = properties.iterator();
        while (iter.hasNext()) {
            SimplePropertyDefinition simplePropDefine = iter.next();
            Map<String, String> metadata = simplePropDefine.getMetadata();
            if (metadata == null) {
                continue;
            }
            String type = metadata.get(Metadatas.CONFIG_TYPE);
            if (type == null || !type.equals(configType)) {
                continue;
            }
            String name = metadata.get(Metadatas.CONFIG_NAME);
            if (name == null || !name.equals(configName)) {
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean useLookup() {
        return useLookup;
    }

    @Override
    public Object getInfo(String key) {
        return additionalInfoMap.get(key);
    }

    @Override
    public void setInfo(String key, Object value) {
        additionalInfoMap.put(key, value);
    }

    @Override
    public void onEvent(String event, Object... parameters) {
        if (event == null) {
            return;
        }
        try {
            switch (event) {
            case IConnection.EVENT_UPDATE_INPUT_CONNECTION:
                onUpdateConnection(parameters);
                break;
            default:
                break;
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    private void onUpdateConnection(Object... parameters) {
        if (parameters == null || parameters.length < 2) {
            throw new IllegalArgumentException("Can only accept one node and one connection, please adapt it if needed.");
        }
        if (parameters[0] == null || parameters[1] == null) {
            return;
        }
        if (!INode.class.isInstance(parameters[0]) || !IConnection.class.isInstance(parameters[1])) {
            throw new IllegalArgumentException("Can only accept one node and one connection, please adapt it if needed.");
        }
        final String param_input_name = "INPUT_NAME"; //$NON-NLS-1$
        INode node = (INode) parameters[0];
        IConnection connection = (IConnection) parameters[1];
        EConnectionType lineStyle = connection.getLineStyle();
        // if (EConnectionType.FLOW_MAIN == lineStyle) {
        // return;
        // }

        if (connection instanceof IAdditionalInfo) {
            Set<String> usedInputSet = new HashSet<>();
            List<? extends IConnection> incomingConnections = node.getIncomingConnections();
            if (incomingConnections != null) {
                for (IConnection incomingConnection : incomingConnections) {
                    INodeConnector connector = incomingConnection.getTargetNodeConnector();
                    EConnectionType connType = (connector == null ? null : connector.getDefaultConnectionType());
                    if (connType != null && connType.hasConnectionCategory(IConnectionCategory.FLOW)) {
                        if (IAdditionalInfo.class.isInstance(incomingConnection)) {
                            IAdditionalInfo inconnInfo = (IAdditionalInfo) incomingConnection;
                            Object info = inconnInfo.getInfo(param_input_name);
                            if (info != null) {
                                usedInputSet.add(info.toString());
                            }
                        }
                    }
                }
            }
            List<String> availableInputs = new ArrayList<>();
            List<? extends INodeConnector> connectors = createConnectors(node);
            for (INodeConnector connector : connectors) {
                if (connector instanceof TaCoKitNodeConnector) {
                    if (((TaCoKitNodeConnector) connector).isInput()) {
                        String connectorName = connector.getName();
                        if (!availableInputs.contains(connectorName)) {
                            availableInputs.add(connectorName);
                        }
                    }
                }
            }
            availableInputs.removeAll(usedInputSet);
            Collections.sort(availableInputs);
            if (!availableInputs.isEmpty()) {
                IAdditionalInfo.class.cast(connection).setInfo(param_input_name, availableInputs.get(0));
            }
        }
    }

}
