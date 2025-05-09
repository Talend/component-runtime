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
package org.talend.sdk.component.tools.validator;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.record.Record;

public class RecordValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final List<String> messages = new ArrayList<>();
        components.stream()
                .forEach(component -> {
                    messages.addAll(this.validateMethods(component.getDeclaredMethods()));
                });
        return messages.stream();
    }

    private List<String> validateMethods(final Method[] methods) {
        final List<String> messages = new ArrayList<>();

        Arrays.stream(methods)
                .forEach(method -> {
                    if (isProducing(method, Record.Builder.class) && !isSafeEntryBuilderProvider(method)) {
                        final String errorMessage = join("\n", asList(
                                format("Method %s calls unsafe Builder creator. This either means:",
                                        getFullName(method)),
                                "  * That the TCK method is safe and should belong to WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER"));
                        messages.add(errorMessage);
                    }
                });
        return messages;
    }

    private static boolean isSafeEntryBuilderProvider(final Method method) {
        return WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER.contains(getFullName(method));
    }

    protected static boolean isProducing(final Method method, final Class<?> clazz) {
        return areTheSameClass(method.getReturnType(), clazz);
    }

    private static boolean areTheSameClass(final Class javaClass, final Class<?> clazz) {
        return javaClass.getPackage() != null && javaClass.getPackage().getName().equals(clazz.getPackage().getName())
                && javaClass.isAssignableFrom(clazz);
    }

    protected static String getFullName(final Method method) {
        return formatMethod(method.getDeclaringClass().getName(), method.getName(), namesOf(method.getParameters()));
    }

    private static String formatMethod(final String ownerName, final String methodName, final String parameters) {
        return ownerName + "." + methodName + "(" + parameters + ")";
    }

    private static String namesOf(final Parameter[] parameters) {
        return Arrays.stream(parameters).map(type -> type.getType().getName()).collect(Collectors.joining(", "));
    }

    /**
     * TCK Java methods that create a {@link Record.Builder} from another builder/record.
     * These are safe, they preserve the props from the other builder/record.
     */
    private static final Set<String> WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER = new HashSet<>(asList(
            "org.talend.sdk.component.runtime.record.RecordImpl.withNewSchema(org.talend.sdk.component.api.record.Schema)",

            "org.talend.sdk.component.runtime.record.RecordImpl.BuilderImpl.before(java.lang.String)",
            "org.talend.sdk.component.runtime.record.RecordImpl.BuilderImpl.after(java.lang.String)",
            "org.talend.sdk.component.runtime.record.RecordImpl.BuilderImpl.withRecord(java.lang.String, org.talend.sdk.component.api.record.Record)",
            "org.talend.sdk.component.runtime.record.RecordImpl.BuilderImpl.removeEntry(org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.runtime.record.RecordImpl.BuilderImpl.updateEntryByName(java.lang.String, org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Record.withNewSchema(org.talend.sdk.component.api.record.Schema)",
            "org.talend.sdk.component.api.record.Record$Builder.before(java.lang.String)",
            "org.talend.sdk.component.api.record.Record$Builder.after(java.lang.String)",
            "org.talend.sdk.component.api.record.Record$Builder.removeEntry(org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Record$Builder.withInstant(org.talend.sdk.component.api.record.Schema$Entry, java.time.Instant)",
            "org.talend.sdk.component.api.record.Record$Builder.updateEntryByName(java.lang.String, org.talend.sdk.component.api.record.Schema$Entry)",
            "org.talend.sdk.component.api.record.Record$Builder.updateEntryByName(java.lang.String, org.talend.sdk.component.api.record.Schema$Entry, java.util.function.Function)",
            "org.talend.sdk.component.api.record.Record$Builder.with(org.talend.sdk.component.api.record.Schema$Entry, java.lang.Object)",
            "org.talend.sdk.component.api.record.Record$Builder.withString(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.api.record.Record$Builder.withString(org.talend.sdk.component.api.record.Schema$Entry, java.lang.String)",
            "org.talend.sdk.component.api.record.Record$Builder.withBytes(java.lang.String, [B)",
            "org.talend.sdk.component.api.record.Record$Builder.withBytes(org.talend.sdk.component.api.record.Schema$Entry, [B)",
            "org.talend.sdk.component.api.record.Record$Builder.withDateTime(java.lang.String, java.util.Date)",
            "org.talend.sdk.component.api.record.Record$Builder.withDateTime(org.talend.sdk.component.api.record.Schema$Entry, java.util.Date)",
            "org.talend.sdk.component.api.record.Record$Builder.withDateTime(java.lang.String, java.time.ZonedDateTime)",
            "org.talend.sdk.component.api.record.Record$Builder.withDateTime(org.talend.sdk.component.api.record.Schema$Entry, java.time.ZonedDateTime)",
            "org.talend.sdk.component.api.record.Record$Builder.withDecimal(java.lang.String, java.math.BigDecimal)",
            "org.talend.sdk.component.api.record.Record$Builder.withDecimal(org.talend.sdk.component.api.record.Schema$Entry, java.math.BigDecimal)",
            "org.talend.sdk.component.api.record.Record$Builder.withTimestamp(java.lang.String, long)",
            "org.talend.sdk.component.api.record.Record$Builder.withTimestamp(org.talend.sdk.component.api.record.Schema$Entry, long)",
            "org.talend.sdk.component.api.record.Record$Builder.withInstant(java.lang.String, java.time.Instant)",
            "org.talend.sdk.component.api.record.Record$Builder.withInstant(org.talend.sdk.component.api.record.Schema$Entry,java.time.Instant)",
            "org.talend.sdk.component.api.record.Record$Builder.withInt(java.lang.String, int)",
            "org.talend.sdk.component.api.record.Record$Builder.withInt(org.talend.sdk.component.api.record.Schema$Entry, int)",
            "org.talend.sdk.component.api.record.Record$Builder.withLong(java.lang.String, long)",
            "org.talend.sdk.component.api.record.Record$Builder.withLong(org.talend.sdk.component.api.record.Schema$Entry, long)",
            "org.talend.sdk.component.api.record.Record$Builder.withFloat(java.lang.String, float)",
            "org.talend.sdk.component.api.record.Record$Builder.withFloat(org.talend.sdk.component.api.record.Schema$Entry, float)",
            "org.talend.sdk.component.api.record.Record$Builder.withDouble(java.lang.String, double)",
            "org.talend.sdk.component.api.record.Record$Builder.withDouble(org.talend.sdk.component.api.record.Schema$Entry, double)",
            "org.talend.sdk.component.api.record.Record$Builder.withBoolean(java.lang.String, boolean)",
            "org.talend.sdk.component.api.record.Record$Builder.withBoolean(org.talend.sdk.component.api.record.Schema$Entry, boolean)",
            "org.talend.sdk.component.api.record.Record$Builder.withRecord(java.lang.String, org.talend.sdk.component.api.record.Record)",
            "org.talend.sdk.component.api.record.Record$Builder.withRecord(org.talend.sdk.component.api.record.Schema$Entry, org.talend.sdk.component.api.record.Record)",
            "org.talend.sdk.component.api.record.Record$Builder.withArray(org.talend.sdk.component.api.record.Schema$Entry, java.util.Collection)",
            "org.talend.sdk.component.api.record.Record$Builder.withError(java.lang.String, java.lang.Object, java.lang.String, java.lang.Exception)",

            "org.talend.sdk.component.api.record.RecordImpl.withNewSchema(org.talend.sdk.component.api.record.Schema)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.with(org.talend.sdk.component.api.record.Schema$Entry, java.lang.Object)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withString(java.lang.String, java.lang.String)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withString(org.talend.sdk.component.api.record.Schema$Entry, java.lang.String)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withBytes(java.lang.String, [B)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withBytes(org.talend.sdk.component.api.record.Schema$Entry, [B)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDateTime(java.lang.String, java.util.Date)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDateTime(org.talend.sdk.component.api.record.Schema$Entry, java.util.Date)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDateTime(java.lang.String, java.time.ZonedDateTime)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDateTime(org.talend.sdk.component.api.record.Schema$Entry, java.time.ZonedDateTime)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDecimal(java.lang.String, java.math.BigDecimal)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDecimal(org.talend.sdk.component.api.record.Schema$Entry, java.math.BigDecimal)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withTimestamp(java.lang.String, long)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withTimestamp(org.talend.sdk.component.api.record.Schema$Entry, long)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withInstant(java.lang.String, java.time.Instant)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withInstant(org.talend.sdk.component.api.record.Schema$Entry,java.time.Instant)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withInt(java.lang.String, int)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withInt(org.talend.sdk.component.api.record.Schema$Entry, int)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withLong(java.lang.String, long)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withLong(org.talend.sdk.component.api.record.Schema$Entry, long)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withFloat(java.lang.String, float)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withFloat(org.talend.sdk.component.api.record.Schema$Entry, float)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDouble(java.lang.String, double)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withDouble(org.talend.sdk.component.api.record.Schema$Entry, double)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withBoolean(java.lang.String, boolean)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withBoolean(org.talend.sdk.component.api.record.Schema$Entry, boolean)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withRecord(java.lang.String, org.talend.sdk.component.api.record.Record)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withRecord(org.talend.sdk.component.api.record.Schema$Entry, org.talend.sdk.component.api.record.Record)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withArray(org.talend.sdk.component.api.record.Schema$Entry, java.util.Collection)",
            "org.talend.sdk.component.api.record.RecordImpl.BuilderImpl.withError(java.lang.String, java.lang.Object, java.lang.String, java.lang.Exception)"));

}
