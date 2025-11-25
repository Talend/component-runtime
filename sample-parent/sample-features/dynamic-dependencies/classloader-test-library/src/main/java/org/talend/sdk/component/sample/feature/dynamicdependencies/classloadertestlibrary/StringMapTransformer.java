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
package org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.api.exception.ComponentException;

public class StringMapTransformer<T> {

    private final StringMapProvider stringMapProvider;

    public StringMapTransformer(final boolean failIfSeveralServicesFound) {
        ServiceLoader<StringMapProvider> serviceLoader = ServiceLoader.load(StringMapProvider.class);

        List<StringMapProvider> stringMapProviderList = new ArrayList<>();
        serviceLoader.iterator().forEachRemaining(stringMapProviderList::add);

        if (stringMapProviderList.size() <= 0) {
            throw new ComponentException("No %s service found.".formatted(StringMapProvider.class));
        }

        if (stringMapProviderList.size() > 1 && failIfSeveralServicesFound) {
            String join = stringMapProviderList.stream()
                    .map(m -> m.getClass().getName())
                    .collect(Collectors.joining("\n"));
            throw new ComponentException("More than one %s service has been found: %s"
                    .formatted(StringMapProvider.class, join));
        }

        this.stringMapProvider = stringMapProviderList.get(0);
    }

    public List<T> transform(final BiFunction<String, String, T> function) {
        Map<String, String> map = stringMapProvider.getMap();
        return map.entrySet()
                .stream()
                .map(e -> function.apply(e.getKey(), e.getValue()))
                .toList();
    }

    public String getResourceContent() {
        Stream<URL> resources = this.getClass().getClassLoader().resources("CLASSLOADER-TEST-SPI/resource.properties");
        return resources
                .map(url -> {
                    try (InputStream is = url.openStream()) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .filter(l -> !l.startsWith("#"))
                .collect(Collectors.joining("\n"));
    }

}