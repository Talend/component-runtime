// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.jmx;

import static java.util.Optional.ofNullable;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.talend.components.container.Container;
import org.talend.components.container.ContainerListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JmxManager implements ContainerListener {

    private final String namePattern;

    private final MBeanServer server;

    @Override
    public void onCreate(final Container container) {
        try {
            final ObjectName name = new ObjectName(String.format(namePattern, container.getId()));
            container.set(JmxData.class, new JmxData(name));
            server.registerMBean(new ContainerMBean(container), name);
        } catch (final InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | MalformedObjectNameException e) {
            log.warn(e.getMessage(), e);
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
