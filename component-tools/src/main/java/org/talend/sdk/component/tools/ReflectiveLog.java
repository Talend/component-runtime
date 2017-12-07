/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ReflectiveLog implements Log {

    private final Object delegate;

    private final Method error;

    private final Method debug;

    private final Method info;

    ReflectiveLog(final Object delegate) throws NoSuchMethodException {
        this.delegate = delegate;
        this.error = findMethod("error");
        this.debug = findMethod("debug");
        this.info = findMethod("info");
    }

    private Method findMethod(final String name) throws NoSuchMethodException {
        final Class<?> delegateClass = delegate.getClass();
        try {
            return delegateClass.getMethod(name, String.class);
        } catch (final NoSuchMethodException nsme) {
            return delegateClass.getMethod(name, CharSequence.class);
        }
    }

    @Override
    public void debug(final String msg) {
        try {
            debug.invoke(delegate, msg);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    @Override
    public void error(final String msg) {
        try {
            error.invoke(delegate, msg);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    @Override
    public void info(final String msg) {
        try {
            info.invoke(delegate, msg);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }
}
