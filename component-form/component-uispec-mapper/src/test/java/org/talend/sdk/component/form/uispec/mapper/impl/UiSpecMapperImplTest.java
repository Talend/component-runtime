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
package org.talend.sdk.component.form.uispec.mapper.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.form.uispec.mapper.api.model.View;
import org.talend.sdk.component.form.uispec.mapper.api.provider.TitleMapProvider;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

class UiSpecMapperImplTest {

    @Test
    void createForm() {
        final Supplier<Ui> form =
                new UiSpecMapperImpl(new UiSpecMapperImpl.Configuration(singletonList(new TitleMapProvider() {

                    private int it = 0;

                    @Override
                    public String reference() {
                        return "vendors";
                    }

                    @Override
                    public Collection<UiSchema.NameValue> get() {
                        return asList(new UiSchema.NameValue.Builder().withName("k" + ++it).withValue("v" + it).build(),
                                new UiSchema.NameValue.Builder().withName("k" + ++it).withValue("v" + it).build());
                    }
                }))).createFormFor(ComponentModel.class);

        IntStream.of(1, 2).forEach(it -> {
            try (final Jsonb jsonb = JsonbBuilder
                    .create(new JsonbConfig()
                            .withFormatting(true)
                            .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
                    final BufferedReader reader =
                            new BufferedReader(new InputStreamReader(
                                    Thread
                                            .currentThread()
                                            .getContextClassLoader()
                                            .getResourceAsStream("component-model-" + it + ".json"),
                                    StandardCharsets.UTF_8))) {
                assertEquals(reader.lines().collect(joining("\n")), jsonb.toJson(form.get()));
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Data
    public static abstract class BaseModel {

        @View.Skip
        private String id;

        @View.Skip
        private Date created;

        @View.Skip
        private Date updated;

        @View.Schema(type = "hidden", readOnly = true)
        private long version;
    }

    @Data
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class ComponentModel extends BaseModel {

        @View.Schema(length = 1024, required = true, position = 1, reference = "vendors")
        private String vendor;

        @View.Schema(length = 2048, required = true, position = 2)
        private String name;

        @View.Schema(length = 2048, required = true, position = 3)
        private String license;

        @View.Schema(length = 2048, required = true, position = 4)
        private String sources;

        @View.Schema(length = 2048, required = true, position = 5)
        private String bugtracker;

        @View.Schema(length = 2048, required = true, position = 6)
        private String documentation;

        @View.Schema(widget = "textarea", length = 8192, required = true, position = 7)
        private String description;

        @View.Schema(widget = "textarea", length = 8192, position = 8)
        private String changelog;
    }
}
