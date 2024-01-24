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
package org.talend.sdk.component.api.base;

import java.util.Iterator;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

// helper class to implement {@link @Producer}
// using a buffer
@RequiredArgsConstructor
public class BufferizedProducerSupport<T> {

    private final Supplier<Iterator<T>> supplier;

    private Iterator<T> current;

    public T next() {
        if (current == null || !current.hasNext()) {
            current = supplier.get();
        }
        return current != null && current.hasNext() ? current.next() : null;
    }
}
