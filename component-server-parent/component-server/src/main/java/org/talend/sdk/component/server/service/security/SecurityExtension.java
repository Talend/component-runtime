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
package org.talend.sdk.component.server.service.security;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessSyntheticObserverMethod;
import javax.enterprise.inject.spi.configurator.ObserverMethodConfigurator;
import javax.inject.Named;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.service.security.event.OnCommand;
import org.talend.sdk.component.server.service.security.event.OnConnection;

import lombok.extern.slf4j.Slf4j;

// this extension will:
// - capture all OnConnection/OnCommand observers
// - bind the configured one (ComponentServerConfiguration) to handle the security
@Slf4j
public class SecurityExtension implements Extension {

    private final Map<String, ObserverMethod<OnConnection>> onConnectionObservers = new HashMap<>();

    private final Map<String, ObserverMethod<OnCommand>> onCommandObservers = new HashMap<>();

    private ObserverMethod<OnConnection> onConnectionMtd;

    private ObserverMethod<OnCommand> onCommandMtd;

    void findOnConnectionMethod(@Observes final ProcessObserverMethod<OnConnection, ?> processObserverMethod) {
        if (ProcessSyntheticObserverMethod.class.isInstance(processObserverMethod)) {
            return;
        }
        onConnectionObservers.put(getName(processObserverMethod), processObserverMethod.getObserverMethod());
        processObserverMethod.veto();
    }

    void findOnCommandMethod(@Observes final ProcessObserverMethod<OnCommand, ?> processObserverMethod) {
        if (ProcessSyntheticObserverMethod.class.isInstance(processObserverMethod)) {
            return;
        }
        onCommandObservers.put(getName(processObserverMethod), processObserverMethod.getObserverMethod());
        processObserverMethod.veto();
    }

    void addSecurityHandlerObservers(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        final ObserverMethodConfigurator<OnConnection> onConnection = afterBeanDiscovery.addObserverMethod();
        onConnection.observedType(OnConnection.class).notifyWith(this::onConnection);
        final ObserverMethodConfigurator<OnCommand> onCommand = afterBeanDiscovery.addObserverMethod();
        onCommand.observedType(OnCommand.class).notifyWith(this::onCommand);
    }

    void bindSecurityHandlers(@Observes final AfterDeploymentValidation afterDeploymentValidation,
            final BeanManager beanManager) {
        final ComponentServerConfiguration configuration = ComponentServerConfiguration.class
                .cast(beanManager
                        .getReference(beanManager.resolve(beanManager.getBeans(ComponentServerConfiguration.class)),
                                ComponentServerConfiguration.class, beanManager.createCreationalContext(null)));

        final String connectionHandler = configuration.getSecurityConnectionHandler();
        onConnectionMtd = ofNullable(onConnectionObservers.get(connectionHandler))
                .orElseThrow(() -> new IllegalArgumentException("No handler '" + connectionHandler + "'"));

        final String commandHandler = configuration.getSecurityCommandHandler();
        onCommandMtd = ofNullable(onCommandObservers.get(commandHandler))
                .orElseThrow(() -> new IllegalArgumentException("No handler '" + commandHandler + "'"));

        // no more needed
        onConnectionObservers.clear();
        onCommandObservers.clear();

        log
                .info("Security configured with connection handler '{}' and command handler '{}'", connectionHandler,
                        commandHandler);
    }

    private void onConnection(final EventContext<OnConnection> onConnection) {
        onConnectionMtd.notify(onConnection);
    }

    private void onCommand(final EventContext<OnCommand> onCommand) {
        onCommandMtd.notify(onCommand);
    }

    private String getName(final ProcessObserverMethod<?, ?> processObserverMethod) {
        return ofNullable(processObserverMethod.getAnnotatedMethod().getDeclaringType().getAnnotation(Named.class))
                .map(Named::value)
                .orElseGet(() -> processObserverMethod.getAnnotatedMethod().getJavaMember().getName());
    }
}
