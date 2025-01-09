/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.util.Date;
import java.util.stream.Stream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerManager;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContainerMBean implements DynamicMBean {

    private final ContainerManager manager;

    private final Container delegate;

    private MBeanInfo info;

    @Override
    public Object getAttribute(final String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute name can't be null");
        }
        switch (attribute) {
            case "closed":
                return delegate.isClosed();
            case "created":
                return delegate.getCreated();
            default:
                throw new AttributeNotFoundException(attribute);
        }
    }

    @Override
    public AttributeList getAttributes(final String[] attributes) {
        final AttributeList attributeList = new AttributeList();
        if (attributes != null) {
            Stream.of(attributes).forEach(name -> {
                try {
                    attributeList.add(new Attribute(name, getAttribute(name)));
                } catch (final AttributeNotFoundException | MBeanException | ReflectionException e) {
                    // no-op: skip
                }
            });
        }
        return attributeList;
    }

    @Override
    public void setAttribute(final Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new AttributeNotFoundException();
    }

    @Override
    public AttributeList setAttributes(final AttributeList attributes) {
        return new AttributeList();
    }

    @Override
    public Object invoke(final String actionName, final Object[] params, final String[] signature)
            throws MBeanException, ReflectionException {
        if (actionName == null) {
            throw new IllegalArgumentException("Action can't be null");
        }
        switch (actionName) {
            case "reload":
                delegate.get(ContainerManager.Actions.class).reload();
                break;
            default:
                throw new UnsupportedOperationException("Unknown action: '" + actionName + "'");
        }
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info == null ? (info = new MBeanInfo(ContainerMBean.class.getName(),
                "MBean for container " + delegate.getId(),
                new MBeanAttributeInfo[] {
                        new MBeanAttributeInfo("closed", boolean.class.getName(), "Is the container already closed",
                                true, false, false),
                        new MBeanAttributeInfo("created", Date.class.getName(), "When was the container created", true,
                                false, false) },
                new MBeanConstructorInfo[0],
                new MBeanOperationInfo[] { new MBeanOperationInfo("reload",
                        "Reloads the container (ie stops it, recreates the classloader from the same files and starts it. Allows to kind of hot reload a plugin.",
                        new MBeanParameterInfo[0], void.class.getName(), MBeanOperationInfo.ACTION) },
                new MBeanNotificationInfo[0])) : info;
    }
}
