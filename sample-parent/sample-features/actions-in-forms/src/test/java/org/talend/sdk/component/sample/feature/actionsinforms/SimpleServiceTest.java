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

import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult.Status;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.sample.feature.actionsinforms.InputConfiguration.Dataset;

@WithComponents("org.talend.sdk.component.sample.feature.actionsinforms")
public class SimpleServiceTest {

    private InputConfiguration configuration;

    @Service
    SimpleService service;

    @BeforeEach
    public void beforeEach() {
        configuration = new InputConfiguration();
    }

    @Test
    void discoverShema() {
        Schema schema = service.discoverShema(configuration.getDataset());
        Assertions.assertNotNull(schema);

        configuration.getDataset().setGenerateExceptions(true);
        Assertions.assertThrowsExactly(ComponentException.class,
                () -> service.discoverShema(configuration.getDataset()));
    }

    @Test
    void validate() {
        ValidationResult ok = service.validate("OKabcdef");
        Assertions.assertEquals(Status.OK, ok.getStatus());

        ValidationResult ko = service.validate("xxabcdef");
        Assertions.assertEquals(Status.KO, ko.getStatus());

        configuration.getDataset().setGenerateExceptions(true);
        Assertions.assertThrowsExactly(ComponentException.class,
                () -> service.validate("OKabc_exception_def"));
    }

    @Test
    void getSuggestions() {
        SuggestionValues values = service.getSuggestions(configuration);
        Assertions.assertNotNull(values);

        configuration.getDataset().setGenerateExceptions(true);
        Assertions.assertThrowsExactly(ComponentException.class,
                () -> service.getSuggestions(configuration));
    }

    @Test
    void updateDataset() {

        configuration.getDataset().setSuggestable("suggestion1");
        configuration.getDataset().setInput1("Hello");
        configuration.getDataset().setInput2("Good bye");
        configuration.getDataset().setGenerateExceptions(false);

        Dataset dataset = service.updateDataset(configuration);

        Assertions.assertNotNull(dataset);
        Assertions.assertEquals("suggestion1", dataset.getSuggestable());
        Assertions.assertEquals("Hello", dataset.getInput1());
        Assertions.assertEquals("Hello", dataset.getInput2());
        Assertions.assertEquals(false, dataset.isGenerateExceptions());

        configuration.getDataset().setGenerateExceptions(true);
        Assertions.assertThrowsExactly(ComponentException.class,
                () -> service.updateDataset(configuration));
    }

    @Test
    void getDataIterator() {
        Iterator<Record> dataIterator = service.getDataIterator(configuration);
        Assertions.assertFalse(dataIterator.hasNext());
    }
}