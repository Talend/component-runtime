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
package org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.spiConsumers;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.talend.sdk.component.api.exception.ComponentException;

public abstract class AbstractSPIConsumer<S, T> {

    private final S spiImpl;

    protected AbstractSPIConsumer(final Class clazz, final boolean failIfSeveralServicesFound) {
        ServiceLoader<S> serviceLoader = ServiceLoader.load(clazz);

        List<S> stringMapProviderList = new ArrayList<>();
        serviceLoader.iterator().forEachRemaining(stringMapProviderList::add);

        if (stringMapProviderList.size() <= 0) {
            throw new ComponentException("No SPI service found for %s.".formatted(clazz));
        }

        if (stringMapProviderList.size() > 1 && failIfSeveralServicesFound) {
            String join = stringMapProviderList.stream()
                    .map(m -> m.getClass().getName())
                    .collect(Collectors.joining("\n"));
            throw new ComponentException("More than one %s service has been found: %s"
                    .formatted(clazz, join));
        }

        this.spiImpl = stringMapProviderList.get(0);
    }

    public abstract List<String> getValues();

    public List<T> transform(final Function<String, T> function) {
        List<String> strings = this.getValues();
        return strings
                .stream()
                .map(function::apply)
                .toList();
    }

    public S getSPIImpl() {
        return this.spiImpl;
    }

}