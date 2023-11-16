/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.api;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;

import org.talend.sdk.component.server.extension.api.action.Action;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.DependencyDefinition;

/**
 * Called when the server starts to register custom extension.
 */
public interface ExtensionRegistrar {

    void registerAwait(final Runnable waiter);

    void registerActions(Collection<Action> actions);

    void registerComponents(Collection<ComponentDetail> components);

    void registerConfigurations(Collection<ConfigTypeNode> configurations);

    void registerDependencies(Map<String, DependencyDefinition> definitions);

    void createExtensionJarIfNotExist(String groupId, String artifactId, String version,
            Consumer<JarOutputStream> creator);
}
