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
package org.talend.sdk.component.junit;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.output.Processor;

/**
 * Test helper allowing you to manipulate mappers/processors unitarly.
 */
public interface ComponentsHandler {

    BaseComponentsHandler.Outputs collect(Processor processor, ControllableInputFactory inputs);

    BaseComponentsHandler.Outputs collect(Processor processor, ControllableInputFactory inputs, int bundleSize);

    <T> Stream<T> collect(Class<T> recordType, Mapper mapper, int maxRecords);

    <T> Stream<T> collect(Class<T> recordType, Mapper mapper, int maxRecords, int concurrency);

    <T> List<T> collectAsList(Class<T> recordType, Mapper mapper);

    <T> List<T> collectAsList(Class<T> recordType, Mapper mapper, int maxRecords);

    Mapper createMapper(Class<?> componentType, Object configuration);

    Processor createProcessor(Class<?> componentType, Object configuration);

    <T> List<T> collect(Class<T> recordType, String family, String component, int version,
            Map<String, String> configuration);

    <T> void process(Iterable<T> inputs, String family, String component, int version,
            Map<String, String> configuration);

    ComponentManager asManager();

    <T> void setInputData(Iterable<T> data);

    <T> List<T> getCollectedData(Class<T> recordType);

    <T> T findService(String plugin, Class<T> serviceClass);

    <T> T findService(Class<T> serviceClass);

    <T> T injectServices(T instance);
}
