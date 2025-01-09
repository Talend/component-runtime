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
package org.talend.sdk.component.test.connectors.service;

import java.lang.reflect.Field;

import org.talend.sdk.component.api.configuration.action.BuiltInSuggestable;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.connection.CreateConnection;

@Service
public class BuiltInSuggestableServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - BuiltInSuggestable
     * https://talend.github.io/component-runtime/main/latest/ref-actions.html#_built_in_suggestable
     *
     */

    public final static String BUILTIN_SUGGESTABLE = "action_BUILTIN_SUGGESTABLE";

    /**
     * BuiltInSuggestable action
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/ref-actions.html#_built_in_suggestable
     * API: @org.talend.sdk.component.api.configuration.action.BuiltInSuggestable
     *
     * REMARK: THis implementation only validates the endpoint / action process,
     * it is not using the application execution side of BuiltInSuggestable
     *
     */

    @CreateConnection(BUILTIN_SUGGESTABLE)
    public Object createConnection() throws IllegalAccessException {
        class Inner {

            @BuiltInSuggestable(value = BuiltInSuggestable.Name.CUSTOM, name = "filed_1")
            private String field1;

            @BuiltInSuggestable(value = BuiltInSuggestable.Name.INCOMING_SCHEMA_ENTRY_NAMES, name = "filed_2")
            private String field2;

        }

        Inner inner = new Inner();

        Field[] fields = Inner.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(BuiltInSuggestable.class)) {
                BuiltInSuggestable bs = field.getAnnotation(BuiltInSuggestable.class);
                field.setAccessible(true);
                field.set(inner, bs.name());
                field.set(inner, bs.value().toString());
            }
        }

        return "{\"field1\":" + "\"" + inner.field1 + "\"" + "," + "\"field2\":" + "\"" + inner.field2 + "\"" + "}";
    }
}
