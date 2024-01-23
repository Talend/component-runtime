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
package org.talend.sdk.component.junit;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * An iterable representing the input data.
 */
@Data
class InputFactoryIterable implements Iterable<Map<String, List<?>>>, Serializable {

    private final ControllableInputFactory inputFactory;

    private final Map<String, Iterator<?>> data;

    @Override
    public Iterator<Map<String, List<?>>> iterator() {
        return new InputFactoryIterator(inputFactory, data);
    }

    @Data
    private static class InputFactoryIterator implements Iterator<Map<String, List<?>>> {

        private final ControllableInputFactory inputFactory;

        private final Map<String, Iterator<?>> data;

        @Override
        public boolean hasNext() {
            return inputFactory.hasMoreData();
        }

        @Override
        public Map<String, List<?>> next() {
            return data.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> singletonList(e.getValue().next())));
        }
    }
}
