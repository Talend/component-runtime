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
package org.talend.sdk.component.runtime.manager.service;

import java.io.ObjectStreamException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.service.api.ComponentInstantiator;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProducerFinderImpl implements ProducerFinder {

    protected String plugin;

    protected ComponentInstantiator.Builder mapperFinder;

    protected Function<Object, Record> recordConverter;

    @Override
    public ProducerFinder init(final String plugin, final Object builder, final Function<Object, Record> converter) {
        this.plugin = plugin;
        mapperFinder = ComponentInstantiator.Builder.class.cast(builder);
        recordConverter = converter;
        return this;
    }

    @Override
    public Iterator<Record> find(final String familyName, final String inputName, final int version,
            final Map<String, String> configuration) {
        final ComponentInstantiator instantiator = getInstantiator(familyName, inputName);
        final Mapper mapper = findMapper(instantiator, version, configuration);

        return iterator(mapper.create());
    }

    protected ComponentInstantiator getInstantiator(final String familyName, final String inputName) {
        final ComponentInstantiator.MetaFinder datasetFinder = new ComponentInstantiator.ComponentNameFinder(inputName);
        final ComponentInstantiator instantiator =
                this.mapperFinder.build(familyName, datasetFinder, ComponentManager.ComponentType.MAPPER);
        if (instantiator == null) {
            log.error("Can't find {} for family {}.", inputName, familyName);
            throw new IllegalArgumentException(
                    String.format("Can't find %s for family %s.", inputName, familyName));
        }
        return instantiator;
    }

    protected Mapper findMapper(final ComponentInstantiator instantiator, final int version,
            final Map<String, String> configuration) {
        return (Mapper) instantiator.instantiate(configuration, version);
    }

    protected Iterator<Record> iterator(final Input input) {
        final Iterator<Object> iteratorObject = new InputIterator(input);

        return new IteratorMap<>(iteratorObject, recordConverter);
    }

    private Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, ProducerFinder.class.getName());
    }

    static class InputIterator implements Iterator<Object> {

        private final Input input;

        private Object nextObject;

        private boolean init;

        InputIterator(final Input input) {
            this.input = input;
        }

        private static Object findNext(final Input input) {
            return input.next();
        }

        @Override
        public boolean hasNext() {
            synchronized (input) {
                if (!init) {
                    init = true;
                    input.start();
                    nextObject = findNext(input);
                }
                if (nextObject == null) {
                    input.stop();
                }
            }
            return nextObject != null;
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final Object current = nextObject;
            nextObject = findNext(input);
            return current;
        }
    }

    @RequiredArgsConstructor
    static class IteratorMap<T, U> implements Iterator<U> {

        private final Iterator<T> wrappedIterator;

        private final Function<T, U> converter;

        @Override
        public boolean hasNext() {
            return this.wrappedIterator.hasNext();
        }

        @Override
        public U next() {
            final T next = this.wrappedIterator.next();
            return this.converter.apply(next);
        }
    }
}
