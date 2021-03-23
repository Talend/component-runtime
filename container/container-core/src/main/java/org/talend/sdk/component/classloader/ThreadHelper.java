/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.classloader;

import org.talend.sdk.component.lang.UnsafeSupplier;

public class ThreadHelper {

    public static <T> T runWithClassLoaderEx(final UnsafeSupplier<T> supplier, final ClassLoader contextualLoader)
            throws Throwable {

        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();

        thread.setContextClassLoader(contextualLoader);
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public static <T> T runWithClassLoader(final UnsafeSupplier<T> supplier, final ClassLoader contextualLoader) {
        try {
            return supplier.get();
        } catch (final RuntimeException | Error re) {
            throw re;
        } catch (final Throwable throwable) { // unlikely
            throw new IllegalStateException(throwable);
        }
    }

}
