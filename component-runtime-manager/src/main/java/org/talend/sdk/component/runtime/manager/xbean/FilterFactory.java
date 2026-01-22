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
package org.talend.sdk.component.runtime.manager.xbean;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.stream.Stream;

import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.finder.filter.FilterList;
import org.apache.xbean.finder.filter.Filters;
import org.apache.xbean.finder.filter.PrefixFilter;

import lombok.NoArgsConstructor;

// "Filters" but xbean already took it
@NoArgsConstructor(access = PRIVATE)
public class FilterFactory {

    public static Filter and(final Filter first, final Filter second) {
        if (Stream
                .of(first, second)
                .anyMatch(f -> !FilterList.class.isInstance(f)
                        || !FilterList.class.cast(f).getFilters().stream().allMatch(PrefixFilter.class::isInstance))) {
            throw new IllegalArgumentException("And only works with filter list of prefix filters"); // for optims
        }
        final FilterList list1 = FilterList.class.cast(first);
        final FilterList list2 = FilterList.class.cast(second);
        return Filters
                .prefixes(Stream
                        .of(list1.getFilters(), list2.getFilters())
                        .flatMap(Collection::stream)
                        .map(PrefixFilter.class::cast)
                        .map(PrefixFilter::getPrefix)
                        .distinct()
                        .toArray(String[]::new));
    }
}
