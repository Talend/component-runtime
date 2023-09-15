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
import static org.talend.sdk.component.tools.validator.RecordValidator.getFullName;
import static org.talend.sdk.component.tools.validator.RecordValidator.isProducing;

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
                .forEach(component -> {
                    messages.addAll(this.validateMethods(component.getDeclaredMethods(), Schema.Builder.class));
                    messages.addAll(this.validateMethods(component.getDeclaredMethods(), Schema.Entry.Builder.class));
                });
        return messages.stream();
    }

    private List<String> validateMethods(final Method[] methods, Class checkClass) {
        final List<String> messages = new ArrayList<>();

        Arrays.stream(methods)
                .forEach(method -> {
                    if (isProducing(method, checkClass) && !isSafeEntryBuilderProvider(method)) {
                        final String errorMessage = join("\n", asList(
                                format("Method %s calls unsafe Builder creator. This either means:",
                                        getFullName(method)),
                                "  * That the TCK method is safe and should belong to WHITE_LIST_TCK_SCHEMA_BUILDER_PROVIDER"));
                        System.err.println("--" + getFullName(method));
                        messages.add(errorMessage);
                    }
                });
        return messages;
    }

    private static boolean isSafeEntryBuilderProvider(final Method method) {
        return WHITE_LIST_TCK_SCHEMA_BUILDER_PROVIDER.contains(getFullName(method));
    }

    private static final Set<String> WHITE_LIST_TCK_SCHEMA_BUILDER_PROVIDER = new HashSet<>(asList(
            "org.talend.sdk.component.runtime.record.SchemaImpl.toBuilder()",
            "org.talend.sdk.component.runtime.record.Schemas.moveBefore(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.runtime.record.Schemas.withType(org.talend.sdk.component.api.record.Schema$Type)",
            "org.talend.sdk.component.runtime.record.Schemas.withEntry(org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.runtime.record.Schemas.withElementSchema(org.talend.sdk.component.api.record.Schema)",
            "org.talend.sdk.component.api.record.Schema$Entry.toBuilder()",
            "org.talend.sdk.component.api.record.Schema$Entry$Builder.withNullable(boolean)",
            "org.talend.sdk.component.api.record.Schema$Entry$Builder.withMetadata(boolean)",
            "org.talend.sdk.component.api.record.Schema$Entry$Builder.withProps(java.util.Map)",
            "org.talend.sdk.component.api.record.Schema$Entry$Builder.withName(java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Entry$Builder.withRawName(java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Entry$Builder.withType(org.talend.sdk.component.api.record.Schema$Type)",
            "org.talend.sdk.component.api.record.Schema$Builder.remove(java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Builder.remove(org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Schema$Builder.swap(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Builder.withEntryBefore(java.lang.String, org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Schema$Builder.moveBefore(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Builder.moveAfter(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Builder.withEntryAfter(java.lang.String, org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Schema$Builder.withType(org.talend.sdk.component.api.record.Schema$Type)",
            "org.talend.sdk.component.api.record.Schema$Builder.withProp(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.api.record.Schema$Builder.withEntry(org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Schema$Builder.withProps(java.util.Map)",
            "org.talend.sdk.component.api.record.Schema$Builder.withElementSchema(org.talend.sdk.component.api.record.Schema)",
            "org.talend.sdk.component.api.service.schema.Schema$Entry.toBuilder()",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl.toBuilder()",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withNullable(boolean)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withMetadata(boolean)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withProps(java.util.Map)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withName(java.lang.String)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withRawName(java.lang.String)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withComment(java.lang.String)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withDefaultValue(java.lang.Object)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withProp(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withType(org.talend.sdk.component.api.record.Schema$Type)",
            "org.talend.sdk.component.runtime.record.SchemaImpl$EntryImpl$BuilderImpl.withElementSchema(org.talend.sdk.component.api.record.Schema)"));

}
