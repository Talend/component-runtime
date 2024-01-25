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
package org.talend.sdk.component.runtime.manager.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.function.Function;

import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParserFactory;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class DefaultServices {

    public static Object lookup(final String type) {
        if (type.equals(JsonBuilderFactory.class.getName())) {
            return ComponentManager.instance().getJsonpBuilderFactory();
        }
        if (type.equals(JsonReaderFactory.class.getName())) {
            return ComponentManager.instance().getJsonpReaderFactory();
        }
        if (type.equals(JsonGeneratorFactory.class.getName())) {
            return ComponentManager.instance().getJsonpGeneratorFactory();
        }
        if (type.equals(JsonParserFactory.class.getName())) {
            return ComponentManager.instance().getJsonpParserFactory();
        }
        if (type.equals(JsonWriterFactory.class.getName())) {
            return ComponentManager.instance().getJsonpWriterFactory();
        }
        if (type.equals(RecordBuilderFactory.class.getName())) {
            final Function<String, RecordBuilderFactory> provider =
                    ComponentManager.instance().getRecordBuilderFactoryProvider();
            return provider.apply(null);
        }
        throw new IllegalArgumentException(type + " can't be a global service, didn't you pass a null plugin?");
    }
}
