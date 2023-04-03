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
package org.talend.sdk.component.jmx;

import static java.util.Optional.ofNullable;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerListener;
import org.talend.sdk.component.container.ContainerManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JmxManager implements ContainerListener {

    private final ContainerManager manager;

    private final String namePattern;

    private final MBeanServer server;

    @Override
    public void onCreate(final Container container) {
        try {
            final ObjectName name = new ObjectName(String.format(namePattern, container.getId()));
            server.registerMBean(new ContainerMBean(manager, container), name);
            container.set(JmxData.class, new JmxData(name));
        } catch (final InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | MalformedObjectNameException e) {
            log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void onClose(final Container container) {
        ofNullable(container.get(JmxData.class)).ifPresent(d -> {
            try {
                server.unregisterMBean(d.name);
            } catch (final InstanceNotFoundException | MBeanRegistrationException e) {
                log.warn(e.getMessage(), e);
            }
        });
    }

    @RequiredArgsConstructor
    private static class JmxData {

        private final ObjectName name;
    }
}
