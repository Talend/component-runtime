/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LazyMap<A, B> extends ConcurrentHashMap<A, B> {

    private final Function<A, B> lazyFactory;

    public LazyMap(final int capacity, final Function<A, B> lazyFactory) {
        super(capacity);
        this.lazyFactory = lazyFactory;
    }

    @Override
    public B get(final Object key) {
        return super.computeIfAbsent((A) key, this.lazyFactory);
    }
}
