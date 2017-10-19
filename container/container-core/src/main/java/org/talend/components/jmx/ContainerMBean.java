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

import org.talend.components.container.Container;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContainerMBean implements DynamicMBean {

    private final Container delegate;

    private MBeanInfo info;

    @Override
    public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
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
            delegate.reload();
            break;
        default:
            throw new UnsupportedOperationException("Unknown action: '" + actionName + "'");
        }
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info == null ? (info = new MBeanInfo(ContainerMBean.class.getName(), "MBean for container " + delegate.getId(),
                new MBeanAttributeInfo[] {
                        new MBeanAttributeInfo("closed", boolean.class.getName(), "Is the container already closed", true, false,
                                false),
                        new MBeanAttributeInfo(
                                "created", Date.class.getName(), "When was the container created", true, false, false) },
                new MBeanConstructorInfo[0],
                new MBeanOperationInfo[] { new MBeanOperationInfo("reload",
                        "Reloads the container (ie stops it, recreates the classloader from the same files and starts it. Allows to kind of hot reload a plugin.",
                        new MBeanParameterInfo[0], void.class.getName(), MBeanOperationInfo.ACTION) },
                new MBeanNotificationInfo[0])) : info;
    }
}
