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
package org.talend.sdk.component.server.lang;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MapCache {

    // simple - but enough - protection against too much entries in the cache
    // this mainly targets ?query king of parameters
    public <A, B> void evictIfNeeded(final ConcurrentMap<A, B> cache, final int maxSize) {
        if (maxSize < 0) {
            cache.clear();
            return;
        }
        while (cache.size() > maxSize) {
            final Iterator<Map.Entry<A, B>> iterator = cache.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.remove();
            }
        }
    }
}
