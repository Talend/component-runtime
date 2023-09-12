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
import org.talend.sdk.component.api.record.Record;

public class RecordValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final List<String> messages = new ArrayList<>();
        components.stream()
                .filter(c -> Record.class.isAssignableFrom(c))
                .forEach(component -> {
                    messages.addAll(this.validateMethods(component.getDeclaredMethods()));
                });
        return messages.stream();
    }

    private List<String> validateMethods(final Method[] methods) {
        final List<String> messages = new ArrayList<>();
        Arrays.stream(methods)
                .filter(RecordValidator::isNotSafeRecordBuilderProvider)
                .forEach(method -> {
                    final String errorMessage = join("\n", asList(
                            format("Method %s calls unsafe Builder creator. This either means:",
                                    method.getName()),
                            "  * That the TCK method is safe and should belong to WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER"));
                    messages.add(errorMessage);
                });
        return messages;
    }

    private static String getFullName(final Method method) {
        System.err.println(
                "in RecordValidator: " + method.getDeclaringClass().getPackage().getName() + "." + method.getName());
        return method.getDeclaringClass().getPackage().getName() + method.getName();
    }

    private static boolean isNotSafeRecordBuilderProvider(final Method method) {
        return !WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER.contains(getFullName(method));
    }

    private static final Set<String> WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER = new HashSet<>(asList(
            "org.talend.sdk.component.api.record.getValue",
            "org.talend.sdk.component.api.record.build",
            "org.talend.sdk.component.api.record.getEntry",
            "org.talend.sdk.component.api.record.before",
            "org.talend.sdk.component.api.record.after",
            "org.talend.sdk.component.api.record.with",
            "org.talend.sdk.component.api.record.removeEntry",
            "org.talend.sdk.component.api.record.lambda$getEntry$0",
            "org.talend.sdk.component.api.record.withString",
            "org.talend.sdk.component.api.record.withArray",
            "org.talend.sdk.component.api.record.withFloat",
            "org.talend.sdk.component.api.record.withDouble",
            "org.talend.sdk.component.api.record.updateEntryByName",
            "org.talend.sdk.component.api.record.withDateTime",
            "org.talend.sdk.component.api.record.withLong",
            "org.talend.sdk.component.api.record.withBytes",
            "org.talend.sdk.component.api.record.withInt",
            "org.talend.sdk.component.api.record.withBoolean",
            "org.talend.sdk.component.api.record.withRecord",
            "org.talend.sdk.component.api.record.withDecimal",
            "org.talend.sdk.component.api.record.getCurrentEntries",
            "org.talend.sdk.component.api.record.withTimestamp",
            "org.talend.sdk.component.api.record.withInstant",
            "org.talend.sdk.component.runtime.record.get",
            "org.talend.sdk.component.runtime.record.equals",
            "org.talend.sdk.component.runtime.record.toString",
            "org.talend.sdk.component.runtime.record.hashCode",
            "org.talend.sdk.component.runtime.record.lambda$toString$0",
            "org.talend.sdk.component.runtime.record.lambda$toString$1",
            "org.talend.sdk.component.runtime.record.getSchema",
            "org.talend.sdk.component.runtime.record.withNewSchema",
            "org.talend.sdk.component.runtime.record.lambda$toString$2",
            "org.talend.sdk.component.runtime.record.lambda$withNewSchema$4",
            "org.talend.sdk.component.runtime.record.lambda$withNewSchema$3"));

}
