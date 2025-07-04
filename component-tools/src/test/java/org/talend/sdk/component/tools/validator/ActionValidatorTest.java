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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.meta.ConditionalOutput;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Values;
import org.talend.sdk.component.api.service.completion.Values.Item;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.discovery.DiscoverDataset;
import org.talend.sdk.component.api.service.discovery.DiscoverDatasetResult;
import org.talend.sdk.component.api.service.discovery.DiscoverDatasetResult.DatasetDescription;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;
import org.talend.sdk.component.api.service.outputs.AvailableOutputFlows;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DatabaseSchemaMapping;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.api.service.update.Update;

class ActionValidatorTest {

    @Test
    void validateDatasetDiscoverAction() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());
        AnnotationFinder finderKO = new AnnotationFinder(new ClassesArchive(ActionDatasetDiscoveryKo.class));
        final Stream<String> errorsStreamKO =
                validator.validate(finderKO, Collections.singletonList(ActionDatasetDiscoveryKo.class));

        final String error = errorsStreamKO.collect(Collectors.joining(" / "));
        final String expected =
                "public java.lang.String org.talend.sdk.component.tools.validator.ActionValidatorTest$ActionDatasetDiscoveryKo.discoverBadReturnType() should have a datastore as first parameter (marked with @DataStore)";
        assertEquals(expected, error);
    }

    @Test
    void validateDiscoverProcessorSchema() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ActionDiscoverProcessorSchemaOk.class));
        final Stream<String> noerrors =
                validator.validate(finder, Collections.singletonList(ActionDiscoverProcessorSchemaOk.class));
        assertEquals(0, noerrors.count());
        finder = new AnnotationFinder(new ClassesArchive(ActionDiscoverProcessorSchemaKo.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(ActionDiscoverProcessorSchemaKo.class));
        assertEquals(13, errors.count());
    }

    @Test
    void validateDatabaseMapping() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ActionDatabaseMappingOK.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(ActionDatabaseMappingOK.class));
        assertEquals(0, noerrors.count());
        finder = new AnnotationFinder(new ClassesArchive(ActionDatabaseMappingKO.class));
        final List<String> errors = validator.validate(finder, Arrays.asList(ActionDatabaseMappingKO.class))
                .collect(Collectors.toList());
        assertEquals(3, errors.size());
        assertAll(() -> assertContains(errors, "should return a String"),
                () -> assertContains(errors, "should have an Object parameter marked with @Option"),
                () -> assertContains(errors, " its parameter being a datastore (marked with @DataStore)"));

    }

    @Test
    void validateDynamicDependencies() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ActionDynamicDependenciesOK.class));
        final Stream<String> noerrors =
                validator.validate(finder, Collections.singletonList(ActionDynamicDependenciesOK.class));
        assertEquals(0, noerrors.count());

        finder = new AnnotationFinder(new ClassesArchive(ActionDynamicDependenciesKO.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(ActionDynamicDependenciesKO.class));
        assertEquals(9, errors.count());
    }

    @Test
    void validate() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());

        AnnotationFinder finder =
                new AnnotationFinder(new ClassesArchive(ActionClassOK.class, FakeDataSet.class, FakeDataStore.class));
        final Stream<String> errorsStream =
                validator.validate(finder, Arrays.asList(ActionClassOK.class, FakeDataSet.class, FakeDataStore.class));
        final List<String> errors = errorsStream.collect(Collectors.toList());
        Assertions.assertTrue(errors.isEmpty(), () -> errors.get(0) + " as first error");

        AnnotationFinder finderKO = new AnnotationFinder(new ClassesArchive(ActionClassKO.class));
        final Stream<String> errorsStreamKO =
                validator.validate(finderKO, Collections.singletonList(ActionClassKO.class));
        final List<String> errorsKO = errorsStreamKO.collect(Collectors.toList());
        assertEquals(6, errorsKO.size(), () -> errorsKO.get(0) + " as first error");

        assertAll(() -> assertContains(errorsKO,
                "hello() doesn't return a class org.talend.sdk.component.api.service.completion.Values"),
                () -> assertContains(errorsKO, "is not declared into a service class"),
                () -> assertContains(errorsKO,
                        "health() should have its first parameter being a datastore (marked with @DataStore)"),
                () -> assertContains(errorsKO, "updatable() should return an object"));
    }

    @Test
    void validateAvailableOutputFlows() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(AvailableOutputFlowsOK.class,
                AvailableOutputProcessor.class, ConfigurationAO.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(AvailableOutputFlowsOK.class, AvailableOutputProcessor.class,
                        ConfigurationAO.class));
        assertEquals(0, noerrors.count());

        finder = new AnnotationFinder(new ClassesArchive(AvailableOutputFlowsKO.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(AvailableOutputFlowsKO.class));
        assertEquals(3, errors.count());
    }

    private void assertContains(List<String> errors, String contentPart) {
        final boolean present =
                errors.stream().anyMatch((String err) -> err.contains(contentPart));
        Assertions.assertTrue(present, "Errors don't have '" + contentPart + "'");
    }

    static class FakeData {

    }

    @DataStore
    static class FakeDataStore {

    }

    @DataSet
    static class FakeDataSet {

        @Proposable("testOK")
        private String prop;

        @Updatable("updatable")
        private FakeData data;
    }

    @Service
    static class ActionClassOK {

        @DynamicValues("testOK")
        public Values hello() {
            return new Values(Collections.singletonList(new Item("", "")));

        }

        @HealthCheck("tests")
        public HealthCheckStatus health(FakeDataStore dataStore) {
            return new HealthCheckStatus(HealthCheckStatus.Status.OK, "ok");
        }

        @DiscoverSchema("tests")
        public Schema discover(FakeDataSet ds) {
            return null;
        }

        @Update("updatable")
        public FakeData updatable() {
            return new FakeData();
        }

        @DiscoverDataset("tests")
        public DiscoverDatasetResult datasetDiscover(FakeDataStore dataStore) {
            final DatasetDescription datasetA = new DatasetDescription("datasetA");
            datasetA.addMetadata("type", "typeA");
            final DatasetDescription datasetB = new DatasetDescription("datasetB");
            datasetA.addMetadata("type", "typeB");
            return new DiscoverDatasetResult(Arrays.asList(datasetA, datasetB));
        }
    }

    static class ActionClassKO {

        @DynamicValues("testKO")
        public String hello() {
            return "";
        }

        @HealthCheck("tests")
        public HealthCheckStatus health() {
            return new HealthCheckStatus(HealthCheckStatus.Status.OK, "ok");
        }

        @Update("updatable")
        public String updatable() {
            return "";
        }
    }

    static class ActionDatasetDiscoveryKo {

        @DiscoverDataset
        public String discoverBadReturnType() {
            return "Should return " + DiscoverDatasetResult.class;
        }
    }

    static class ActionDiscoverProcessorSchemaOk {

        @DiscoverSchemaExtended("test-all")
        public Schema guessProcessorSchemaOk(final Schema incomingSchema, @Option FakeDataSet configuration,
                final String branch) {
            return null;
        }

        @DiscoverSchemaExtended("test-schema")
        public Schema guessProcessorSchemaOk(final Schema incomingSchema, @Option FakeDataSet configuration) {
            return null;
        }

        @DiscoverSchemaExtended("test-branch")
        public Schema guessProcessorSchemaOk(@Option FakeDataSet configuration, final String branch) {
            return null;
        }

        @DiscoverSchemaExtended("test-min")
        public Schema guessProcessorSchemaOk(@Option FakeDataSet configuration) {
            return null;
        }

        @DiscoverSchemaExtended("test-extra")
        public Schema guessProcessorSchemaOk(@Option FakeDataSet configuration, RecordBuilderFactory factory) {
            return null;
        }

        @DiscoverSchemaExtended("test-extra2")
        public Schema guessProcessorSchemaOk(@Option FakeDataSet configuration, final String branch,
                RecordBuilderFactory factory, final String branchy) {
            return null;
        }

        @DiscoverSchemaExtended("test-extra3")
        public Schema guessProcessorSchemaOk(@Option FakeDataSet configuration, final String branch,
                RecordBuilderFactory factory, final String branchy, final Schema incomingSchema, final Schema schema) {
            return null;
        }
    }

    static class ActionDiscoverProcessorSchemaKo {

        @DiscoverSchemaExtended("test-none")
        public Schema guessProcessorSchemaKo0() {
            return null;
        }

        @DiscoverSchemaExtended("schema")
        public Schema guessProcessorSchemaKo1(final Schema schema, final @Option FakeDataSet configuration,
                final String outgoing) {
            return null;
        }

        @DiscoverSchemaExtended("schema")
        public Schema guessProcessorSchemaKo2(final Schema schema, final @Option FakeDataSet configuration,
                final String outgoing) {
            return null;
        }

        @DiscoverSchemaExtended("record")
        public Record guessProcessorSchemaKo3(FakeDataSet configuration, final String outgoing) {
            return null;
        }

        @DiscoverSchemaExtended("record")
        public Record guessProcessorSchemaKo4(FakeDataSet configuration, final String branch) {
            return null;
        }

        @DiscoverSchemaExtended("record")
        public Record guessProcessorSchemaKo5(FakeDataSet configuration) {
            return null;
        }

        @DiscoverSchemaExtended("record")
        public Record guessProcessorSchemaKo6(@Option FakeDataSet configuration, RecordBuilderFactory factory) {
            return null;
        }
    }

    static class ActionDatabaseMappingOK {

        @DatabaseSchemaMapping("test-none")
        public String mappingsOk(@Option("datastore") FakeDataStore datastore) {
            return null;
        }
    }

    static class ActionDatabaseMappingKO {

        @DatabaseSchemaMapping("test-option")
        public String mappingsKoOption(FakeDataSet dataset) {
            return null;
        }

        @DatabaseSchemaMapping("test-return")
        public Schema mappingsKoReturn(@Option("datastore") FakeDataStore datastore) {
            return null;
        }
    }

    @Service
    static class ActionDynamicDependenciesOK {

        @DynamicDependencies("test-all")
        public List<String> getDynamicDependencies(@Option("configuration") final FakeDataSet dataset) {
            return null;
        }
    }

    @Service
    static class ActionDynamicDependenciesKO {

        @DynamicDependencies("error: return List<String>")
        public String getDynamicDependencies(@Option("configuration") final FakeDataSet dataset) {
            return null;
        }

        @DynamicDependencies("error-param:no Option, no Dataset")
        public List<String> getDynamicDependencies2() {
            return null;
        }

        @DynamicDependencies("error: param not dataset")
        public String getDynamicDependencies3(@Option("configuration") final FakeDataStore dataset) {
            return null;
        }

        @DynamicDependencies("error: param not option")
        public List<Object> getDynamicDependencies4(final FakeDataStore dataset) {
            return null;
        }

        @DynamicDependencies("error: List<T> T not String")
        public List<Object> getDynamicDependencies5(@Option("configuration") final FakeDataSet dataset) {
            return null;
        }

        @DynamicDependencies("error: 2 params")
        public List<String> getDynamicDependencies6(@Option("configuration") final FakeDataSet dataset,
                @Option("configuration") final FakeDataSet dataset2) {
            return null;
        }
    }

    @Service
    static class AvailableOutputFlowsOK {

        @AvailableOutputFlows("outflow-1")
        public List<String> getAvailableOutputs(@Option("configuration") final ConfigurationAO config) {
            return new ArrayList<>();
        }
    }

    @Service
    static class AvailableOutputFlowsKO {

        @AvailableOutputFlows("outflow-2")
        public Set<String> getAvailableOutputs(@Option("configuration") final FakeDataSet config) {
            return null;
        }
    }

    @Processor(name = "availableoutputprocessor")
    @ConditionalOutput("outflow-1")
    static class AvailableOutputProcessor implements Serializable {

        private ConfigurationAO config;

        @ElementListener
        public void process(@Input final Record input,
                @Output final OutputEmitter<Record> main,
                @Output("second") final OutputEmitter<Record> second) {
            // this method here is just used to declare some outputs.
        }
    }

    static class ConfigurationAO implements Serializable {

        @Option
        @Documentation("Show second output or not.")
        private boolean showSecond;
    }
}