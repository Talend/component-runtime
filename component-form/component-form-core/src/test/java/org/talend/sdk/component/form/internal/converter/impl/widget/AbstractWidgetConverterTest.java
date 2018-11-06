/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.converter.impl.widget;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

class AbstractWidgetConverterTest {

    static Stream<Map.Entry<Map<String, String>, String>> conditionsSpec() {
        return Stream
                .of(
                        // simple single condition
                        new AbstractMap.SimpleEntry<>(new HashMap<String, String>() {

                            {
                                put("condition::if::target", "foo");
                                put("condition::if::value", "Bar");
                                put("condition::if::negate", "false");
                                put("condition::if::evaluationStrategy", "DEFAULT");
                            }
                        }, "{\"===\":[{\"var\":\"foo\"},\"Bar\"]}"),
                        // negate
                        new AbstractMap.SimpleEntry<>(new HashMap<String, String>() {

                            {
                                put("condition::if::target", "foo");
                                put("condition::if::value", "something");
                                put("condition::if::negate", "true");
                                put("condition::if::evaluationStrategy", "DEFAULT");
                            }
                        }, "{\"==\":[{\"===\":[{\"var\":\"foo\"},\"something\"]},false]}"),
                        // strategy "length"
                        new AbstractMap.SimpleEntry<>(new HashMap<String, String>() {

                            {
                                put("condition::if::target", "foo");
                                put("condition::if::value", "1");
                                put("condition::if::negate", "false");
                                put("condition::if::evaluationStrategy", "LENGTH");
                            }
                        }, "{\"===\":[{\"var\":\"foo.length\"},1]}"),
                        // strategy "contains"
                        new AbstractMap.SimpleEntry<>(new HashMap<String, String>() {

                            {
                                put("condition::if::target", "foo");
                                put("condition::if::value", "Bar");
                                put("condition::if::negate", "false");
                                put("condition::if::evaluationStrategy", "CONTAINS");
                            }
                        }, "{\"in\":[\"Bar\",{\"var\":\"foo\"}]}"),
                        // strategy "contains"+lowercase
                        new AbstractMap.SimpleEntry<>(new HashMap<String, String>() {

                            {
                                put("condition::if::target", "foo");
                                put("condition::if::value", "Bar");
                                put("condition::if::negate", "false");
                                put("condition::if::evaluationStrategy", "CONTAINS(lowercase=true)");
                            }
                        }, "{\"in\":[\"bar\",{\"var\":\"foo\"}]}"),
                        // multiple simple conditions
                        new AbstractMap.SimpleEntry<>(new LinkedHashMap<String, String>() {

                            {
                                put("condition::if::target::0", "foo");
                                put("condition::if::value::0", "Bar");
                                put("condition::if::negate::0", "false");
                                put("condition::if::evaluationStrategy::0", "DEFAULT");
                                put("condition::if::target::1", "other");
                                put("condition::if::value::1", "Dummy");
                                put("condition::if::negate::1", "false");
                                put("condition::if::evaluationStrategy::1", "DEFAULT");
                            }
                        }, "{\"and\":[{\"===\":[{\"var\":\"foo\"},\"Bar\"]},{\"===\":[{\"var\":\"other\"},\"Dummy\"]}]}"),
                        // multiple simple values
                        new AbstractMap.SimpleEntry<>(new LinkedHashMap<String, String>() {

                            {
                                put("condition::if::target", "foo");
                                put("condition::if::value", "Bar,Dummy");
                                put("condition::if::negate", "false");
                                put("condition::if::evaluationStrategy", "DEFAULT");
                            }
                        }, "{\"or\":[{\"===\":[{\"var\":\"foo\"},\"Bar\"]},{\"===\":[{\"var\":\"foo\"},\"Dummy\"]}]}"));
    }

    @ParameterizedTest
    @MethodSource("conditionsSpec")
    void condition(final Map.Entry<Map<String, String>, String> spec) throws Exception {
        final AtomicReference<Map<String, Collection<Object>>> condition = new AtomicReference<>();
        new AbstractWidgetConverter(emptyList(), emptyList(), emptyList(), null, "en") {

            @Override
            public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> context) {
                throw new UnsupportedOperationException();
            }

            {
                final SimplePropertyDefinition property =
                        new SimplePropertyDefinition("root", "root", null, null, null, null, spec.getKey(), null, null);
                final PropertyContext<?> ctx = new PropertyContext<>(property, null, null);
                condition.set(createCondition(ctx));
            }
        };
        final Map<String, Collection<Object>> jsonLogicCondition = condition.get();
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(false))) {
            assertEquals(spec.getValue(), jsonb.toJson(jsonLogicCondition));
        }
    }

    @Test
    void triggerArrayParameters() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create();
                final InputStream stream = Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("object-array-in-parameter.json")) {
            final ComponentDetail details = jsonb.fromJson(stream, ComponentDetail.class);
            final AtomicReference<UiSchema.Trigger> trigger = new AtomicReference<>();
            new AbstractWidgetConverter(new ArrayList<>(), details.getProperties(), emptyList(), new JsonSchema(),
                    "en") {

                @Override
                public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> context) {
                    throw new UnsupportedOperationException();
                }

                {
                    trigger
                            .set(toTrigger(details.getProperties(),
                                    details
                                            .getProperties()
                                            .stream()
                                            .filter(it -> it.getName().equals("filterAdvancedValueWrapper"))
                                            .findFirst()
                                            .orElseThrow(IllegalArgumentException::new),
                                    details
                                            .getActions()
                                            .stream()
                                            .filter(it -> it.getName().equals("updatableFilterAdvanced"))
                                            .findFirst()
                                            .orElseThrow(IllegalArgumentException::new)));
                }
            };
            final UiSchema.Parameter parameter = trigger
                    .get()
                    .getParameters()
                    .stream()
                    .filter(it -> it.getPath().equals("configuration.selectionFilter.filterLines"))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            assertEquals("filterLines", parameter.getKey());
        }
    }

    @Test
    void triggerWithParameters() {
        final AtomicReference<UiSchema.Trigger> trigger = new AtomicReference<>();
        final Set<SimplePropertyDefinition> singleton = singleton(new SimplePropertyDefinition("foo", "foo", null,
                "enum", null, new PropertyValidation(), new HashMap<String, String>() {

                    {
                        put("action::ty", "other::foo(bar=dummy)");
                        // put("action::ty::parameters", "."); // ignored in this test
                    }
                }, null, new LinkedHashMap<>()));
        new AbstractWidgetConverter(new ArrayList<>(), singleton, emptyList(), new JsonSchema(), "en") {

            @Override
            public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> context) {
                throw new UnsupportedOperationException();
            }

            {
                trigger
                        .set(toTrigger(singleton, singleton.iterator().next(),
                                new ActionReference("fam", "other::foo", "ty", null, emptyList())));
            }
        };
        final UiSchema.Trigger output = trigger.get();
        assertEquals("fam", output.getFamily());
        assertEquals("ty", output.getType());
        assertEquals("other::foo(bar=dummy)", output.getAction());
    }
}
