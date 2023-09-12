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
package org.talend.sdk.component.tools.validator;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.record.Schema;

public class SchemaValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final List<String> messages = new ArrayList<>();
        components.stream()
                .filter(c -> Schema.class.isInstance(c))
                .forEach(component -> {
                    messages.addAll(this.validateMethods(component.getDeclaredMethods()));
                });
        return messages.stream();
    }

    private List<String> validateMethods(final Method[] methods) {
        final List<String> messages = new ArrayList<>();
        Arrays.stream(methods)
                .filter(SchemaValidator::isNotSafeSchemaBuilderProvider)
                .forEach(method -> {
                    final String errorMessage = join("\n", asList(
                            format("Method %s calls unsafe Builder creator. This either means:",
                                    method.getName()),
                            "  * That the TCK method is safe and should belong to WHITE_LIST_TCK_SCHEMA_BUILDER_PROVIDER"));
                    messages.add(errorMessage);
                });
        return messages;
    }

    private static String getFullName(final Method method) {
        return method.getDeclaringClass().getPackage().getName() + "." + method.getName();
    }

    private static boolean isNotSafeSchemaBuilderProvider(final Method method) {
        return !WHITE_LIST_TCK_SCHEMA_BUILDER_PROVIDER.contains(getFullName(method));
    }

    private static final Set<String> WHITE_LIST_TCK_SCHEMA_BUILDER_PROVIDER = new HashSet<>(asList(
            "org.talend.sdk.component.runtime.record.getFieldsOrder",
            "org.talend.sdk.component.runtime.record.getAllEntries",
            "org.talend.sdk.component.runtime.record.getMetadata",
            "org.talend.sdk.component.runtime.record.canEqual",
            "org.talend.sdk.component.runtime.record.getEntriesOrdered",
            "org.talend.sdk.component.runtime.record.getProp",
            "org.talend.sdk.component.runtime.record.getElementSchema",
            "org.talend.sdk.component.runtime.record.getProps",
            "org.talend.sdk.component.runtime.record.toBuilder",
            "org.talend.sdk.component.runtime.record.equals",
            "org.talend.sdk.component.runtime.record.toString",
            "org.talend.sdk.component.runtime.record.hashCode",
            "org.talend.sdk.component.runtime.record.getType",
            "org.talend.sdk.component.runtime.record.getEntries",
            "org.talend.sdk.component.runtime.record.naturalOrder",
            "org.talend.sdk.component.api.service.record.newEntryBuilder",
            "org.talend.sdk.component.api.service.record.newRecordBuilder",
            "org.talend.sdk.component.api.service.record.newSchemaBuilder",
            "org.talend.sdk.component.api.record.withEntryBefore",
            "org.talend.sdk.component.api.record.moveAfter",
            "org.talend.sdk.component.api.record.withEntryAfter",
            "org.talend.sdk.component.api.record.moveBefore",
            "org.talend.sdk.component.api.record.withProp",
            "org.talend.sdk.component.api.record.withElementSchema",
            "org.talend.sdk.component.api.record.withProps",
            "org.talend.sdk.component.api.record.withType",
            "org.talend.sdk.component.api.record.withEntry",
            "org.talend.sdk.component.api.record.remove",
            "org.talend.sdk.component.api.record.build",
            "org.talend.sdk.component.api.record.swap"));
}
