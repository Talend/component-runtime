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
package org.talend.sdk.component.feature.form.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult.Status;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.SuggestionValues.Item;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.feature.form.config.ADataset;
import org.talend.sdk.component.feature.form.config.ADatastore;
import org.talend.sdk.component.feature.form.config.DynamicElements.SomeComplexConfig;

@Service
public class UIService {

    public final static String HEALTHCHECK = "HEALTHCHECK";

    public final static String DISCOVERSCHEMA = "DISCOVERSCHEMA";

    public final static String SUGGESTABLE = "SUGGESTABLE";

    public final static String UPDATABLE = "UPDATABLE";

    public final static String ASYNC_VALIDATION = "ASYNC_VALIDATION";

    public final static String ASYNC_VALIDATION_ONSTRING = "ASYNC_VALIDATION2";

    @Service
    private RecordBuilderFactory recordBuilderFactory;

    @HealthCheck(HEALTHCHECK)
    public HealthCheckStatus healthCheck(@Option("configuration") final ADatastore datastore) {
        if (datastore.isHealthcheckOk()) {
            return new HealthCheckStatus(HealthCheckStatus.Status.OK, "Connection's configuration is ok.");
        } else {
            return new HealthCheckStatus(HealthCheckStatus.Status.KO, "Can't connect...");
        }
    }

    @DiscoverSchema(DISCOVERSCHEMA)
    public Schema discoverSchema(@Option("configuration") final ADataset dataset) {
        return recordBuilderFactory.newSchemaBuilder(Type.RECORD)
                .withEntry(recordBuilderFactory
                        .newEntryBuilder()
                        .withName("configuration")
                        .withType(Type.STRING)
                        .withNullable(false)
                        .build())
                .withEntry(recordBuilderFactory
                        .newEntryBuilder()
                        .withName("fixedString")
                        .withType(Type.STRING)
                        .withNullable(false)
                        .build())
                .withEntry(recordBuilderFactory
                        .newEntryBuilder()
                        .withName("fixedInt")
                        .withType(Type.INT)
                        .withNullable(false)
                        .build())
                .withEntry(recordBuilderFactory
                        .newEntryBuilder()
                        .withName("fixedBoolean")
                        .withType(Type.BOOLEAN)
                        .withNullable(false)
                        .build())
                .build();
    }

    @Suggestions(SUGGESTABLE)
    public SuggestionValues loadSuggestables(final SomeComplexConfig someComplexConfig, final String singleString) {
        List<Item> items = IntStream.range(0, someComplexConfig.getAnInteger())
                .mapToObj(i -> new SuggestionValues.Item(singleString + "_" + i,
                        singleString + "_" + someComplexConfig.getAString() +
                                "_" + someComplexConfig.isABoolean() + "_" + i))
                .collect(Collectors.toList());
        return new SuggestionValues(true, items);
    }

    @Update(UPDATABLE)
    public SomeComplexConfig update(final String singleString,
            final String suggestedElement) {

        String aString = singleString == null ? "" : singleString.toUpperCase(Locale.ROOT);
        boolean aBoolean = singleString == null || singleString.trim().isEmpty();
        int anInteger = suggestedElement == null || suggestedElement.isEmpty() ? 0
                : Integer.parseInt(suggestedElement.substring(
                        suggestedElement.lastIndexOf("_") + 1)) + 1;

        SomeComplexConfig scc = new SomeComplexConfig(
                aString,
                aBoolean,
                anInteger);

        return scc;
    }

    // Todo: Not implemented in WebUI; In the studio async validation works only on primitive types.
    @AsyncValidation(ASYNC_VALIDATION)
    public ValidationResult asyncValidation(final SomeComplexConfig someComplexConfig) {
        if (someComplexConfig.getAnInteger() % 3 == 0) {
            return new ValidationResult(Status.KO, "The integer must not be a multiple of 3.");
        }

        if (someComplexConfig.getAString().toLowerCase().contains("abc")) {
            return new ValidationResult(Status.KO, "The string must not contain 'abc' in any character case.");
        }

        return new ValidationResult(Status.OK, "The configuration is ok.");
    }

    @AsyncValidation(ASYNC_VALIDATION_ONSTRING)
    public ValidationResult asyncValidation(final String someComplexConfig) {
        if (someComplexConfig.toLowerCase().contains("abc")) {
            return new ValidationResult(Status.KO, "The string must not contain 'abc' in any character case.");
        }

        return new ValidationResult(Status.OK, "The configuration is ok.");
    }

}
