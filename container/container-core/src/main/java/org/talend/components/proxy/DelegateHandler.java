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
package org.talend.components.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import lombok.Data;

@Data
public class DelegateHandler implements InvocationHandler {

    protected final Object delegate;

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            return doHandleObjectMethods(method, args);
        }

        try {
            return doInvoke(method, args);
        } catch (final InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

    protected Object doInvoke(final Method method, final Object[] args) throws Throwable {
        return method.invoke(delegate, args);
    }

    protected Object doHandleObjectMethods(final Method method, final Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        switch (method.getName()) {
        case "equals":
            return args[0] != null && Proxy.isProxyClass(args[0].getClass()) && this == Proxy.getInvocationHandler(args[0]);
        default:
            return method.invoke(this, args);
        }
    }
}
