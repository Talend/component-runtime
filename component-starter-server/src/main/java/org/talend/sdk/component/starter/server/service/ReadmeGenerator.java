/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;

@ApplicationScoped
public class ReadmeGenerator {

    public String createReadme(final String name, final Map<FacetGenerator, List<String>> filesPerFacet) {
        final Map<String, List<FacetGenerator>> facetByCategory = filesPerFacet
                .keySet()
                .stream()
                .collect(toMap(f -> f.category().getHumanName(), f -> new ArrayList<>(singletonList(f)), (u, u2) -> {
                    if (u == null) {
                        return u2;
                    }
                    u.addAll(u2);
                    return u;
                }, TreeMap::new));

        final StringBuilder builder = new StringBuilder();
        builder.append("= ").append(name).append("\n\n");

        facetByCategory.forEach((c, f) -> {
            builder.append("== ").append(c).append("\n\n");
            f.stream().sorted(Comparator.comparing(FacetGenerator::name)).forEach(facet -> {
                builder.append("=== ").append(facet.name()).append("\n\n");
                builder.append(ofNullable(facet.readme()).orElseGet(() -> ofNullable(facet.description()).orElse("")));
                builder.append("\n\n");

                final Collection<String> files = filesPerFacet.get(facet);
                if (files != null && !files.isEmpty()) {
                    builder.append("==== Files generated by this facet\n\n");
                    files.forEach(file -> builder.append("- ").append(file).append("\n"));
                }

                builder.append("\n\n");
            });
        });

        return builder.toString();
    }
}
