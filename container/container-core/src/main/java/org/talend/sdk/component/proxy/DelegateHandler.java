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
package org.talend.sdk.component.proxy;

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
            return args[0] != null && Proxy.isProxyClass(args[0].getClass())
                    && this == Proxy.getInvocationHandler(args[0]);
        default:
            return method.invoke(this, args);
        }
    }
}
