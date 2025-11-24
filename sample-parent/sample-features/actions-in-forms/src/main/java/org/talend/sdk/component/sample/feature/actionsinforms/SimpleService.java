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
package org.talend.sdk.component.sample.feature.actionsinforms;

import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult.Status;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.sample.feature.actionsinforms.InputConfiguration.Dataset;

@Service
public class SimpleService {

    public static final String SUGGESTABLE = "SUGGESTABLE";

    public static final String UPDATABLE = "UPDATABLE";

    public static final String ASYNC_VALIDATION = "ASYNC_VALIDATION";

    @Service
    private RecordBuilderFactory recordBuilderFactory;

    @DiscoverSchema("dataset")
    public Schema discoverSchema(@Option final Dataset dataset) {
        return getSchema(dataset, "DiscoverSchema");
    }

    @DiscoverSchemaExtended
    public Schema discoverSchemaExtended(@Option final InputConfiguration config) {
        return getSchema(config.getDataset(), "DiscoverSchemaExtended");
    }

    private Schema getSchema(final Dataset dataset, final String name) {
        if (dataset.isGenerateExceptions()) {
            throw new ComponentException("Exception thrown in @" + name + " service.");
        }

        return recordBuilderFactory.newSchemaBuilder(Type.RECORD)
                .withEntry(recordBuilderFactory.newEntryBuilder().withName(name).withType(Type.STRING).build())
                .build();
    }

    @AsyncValidation(ASYNC_VALIDATION)
    public ValidationResult validate(final String input1) {
        if (input1.toLowerCase(Locale.ROOT).contains("exception")) {
            throw new ComponentException("Exception thrown in @AsyncValidation service.");
        }

        return new ValidationResult(
                input1.toLowerCase(Locale.ROOT).startsWith("ok") ? Status.OK : Status.KO,
                "The input1 field should start by 'OK'");
    }

    @Suggestions(SUGGESTABLE)
    public SuggestionValues getSuggestions(final InputConfiguration config) {
        if (config.getDataset().isGenerateExceptions()) {
            throw new ComponentException("Exception thrown in @Suggestion service.");
        }
        return new SuggestionValues(false, Stream.of("element1", "element2", "element3")
                .map(e -> new SuggestionValues.Item(e + "_id", e))
                .toList());
    }

    @Update(UPDATABLE)
    public Dataset updateDataset(final InputConfiguration config) {
        if (config.getDataset().isGenerateExceptions()) {
            throw new ComponentException("Exception thrown in @Update service.");
        }
        Dataset dataset = config.getDataset();
        Dataset newDataset = new Dataset();

        newDataset.setInput1(dataset.getInput1());
        newDataset.setInput2(dataset.getInput1());
        newDataset.setDatastore(dataset.getDatastore());
        newDataset.setSuggestable(dataset.getSuggestable());

        return newDataset;
    }

    /**
     * Return an empty iterator.
     */
    public Iterator<Record> getDataIterator(final InputConfiguration configuration) {
        return Collections.emptyIterator();
    }

}