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
package org.talend.sdk.component.server.lang;

import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collector;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class CustomCollectors {

    public static <T, K, V> Collector<T, ?, LinkedHashMap<K, V>> toLinkedMap(
            final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper) {
        return toMap(keyMapper, valueMapper, (v1, v2) -> {
            throw new IllegalArgumentException("conflict on key " + v1);
        }, LinkedHashMap::new);
    }
}
