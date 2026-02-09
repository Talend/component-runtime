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
package org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.spiConsumers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractSPIConsumer<S, T> {

    private final Optional<S> spiImpl;

    protected AbstractSPIConsumer(final Class clazz) {
        ServiceLoader<S> serviceLoader = ServiceLoader.load(clazz, AbstractSPIConsumer.class.getClassLoader());

        List<S> implProvider = new ArrayList<>();

        try {
            serviceLoader.iterator().forEachRemaining(implProvider::add);
        } catch (Throwable e) {
            log.error("Can't load %s spi implementation: %s.".formatted(clazz, e.getMessage()), e);
        }

        if (implProvider.size() <= 0) {
            log.error("No SPI service found for %s.".formatted(clazz));
            spiImpl = Optional.empty();
            return;
        }

        if (implProvider.size() > 1) {
            String join = implProvider.stream()
                    .map(m -> m.getClass().getName())
                    .collect(Collectors.joining("\n"));
            log.error("More than one %s service has been found: %s.".formatted(clazz, join));
            // For testing purpose (the goal of this connector), better to fail in that case.
            spiImpl = Optional.empty();
            return;
        }

        this.spiImpl = Optional.of(implProvider.get(0));
    }

    public abstract String getValue();

    public T transform(final Function<String, T> function) {
        String value = this.getValue();
        return function.apply(value);
    }

    public Optional<S> getSPIImpl() {
        return this.spiImpl;
    }

}