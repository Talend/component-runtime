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
package org.talend.sdk.component.studio.model.parameter;

import static org.talend.sdk.component.studio.model.action.Action.HEALTH_CHECK;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.talend.core.model.process.IElementParameter;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.studio.model.action.SettingsActionParameter;
import org.talend.sdk.component.studio.model.command.HealthCheckCommand;

/**
 * Binds together things required for HealthCheck callback
 */
public class HealthCheckResolver {

    private final ButtonParameter button;

    private final PropertyNode checkableNode;

    private final HealthCheckCommand command;

    private final ActionReference action;

    public HealthCheckResolver(final PropertyNode checkableNode, final ButtonParameter button,
            final HealthCheckCommand command, final Collection<ActionReference> actions) {
        this.button = button;
        this.command = command;
        this.checkableNode = checkableNode;
        this.action = actions
                .stream()
                .filter(a -> HEALTH_CHECK.equals(a.getType()))
                .filter(a -> a.getName().equals(checkableNode.getProperty().getHealthCheckName()))
                .findFirst()
                .get();
    }

    public void resolveParameters(final Map<String, IElementParameter> settings) {
        final String basePath = checkableNode.getProperty().getPath();
        final String alias = getParameterAlias();
        final PathCollector collector = new PathCollector();
        checkableNode.accept(collector);
        collector
                .getPaths()
                .stream()
                .map(settings::get)
                .filter(Objects::nonNull)
                .map(p -> (TaCoKitElementParameter) p)
                .forEach(p -> {
                    final String parameter = p.getName().replace(basePath, alias);
                    final SettingsActionParameter actionParameter = new SettingsActionParameter(p, parameter);
                    command.addParameter(actionParameter);
                });
        button.setCommand(command);
    }

    /**
     * Finds parameter alias (which is value of Option annotation in HealthCheck method)
     * This method builds property tree and assumes that root node path is a required alias
     * 
     * @return parameter alias
     */
    private String getParameterAlias() {
        final Collection<PropertyDefinitionDecorator> properties =
                PropertyDefinitionDecorator.wrap(action.getProperties());
        final PropertyNode root = new PropertyTreeCreator(new WidgetTypeMapper()).createPropertyTree(properties);
        return root.getProperty().getPath();
    }
}
