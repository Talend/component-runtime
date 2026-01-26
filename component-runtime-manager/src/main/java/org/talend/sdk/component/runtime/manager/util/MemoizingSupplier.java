/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Supplier that retains value.
 */
public class MemoizingSupplier<T> implements Supplier<T> {

    private final Lock lock = new ReentrantLock();

    private final Supplier<T> delegate;

    private volatile T value = null;

    public MemoizingSupplier(final Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        if (value == null) {
            lock.lock();
            try {
                if (value == null) {
                    value = delegate.get();
                }
            } finally {
                lock.unlock();
            }
        }
        return value;
    }
}
