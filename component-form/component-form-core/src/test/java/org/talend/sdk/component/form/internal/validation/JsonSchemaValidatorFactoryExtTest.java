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
package org.talend.sdk.component.form.internal.validation;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.List;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.spi.JsonProvider;

import org.apache.johnzon.jsonschema.JsonSchemaValidator;
import org.apache.johnzon.jsonschema.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.validation.spi.ext.EnumValidationWithDefaultValue;
import org.talend.sdk.component.form.internal.validation.spi.ext.MaximumValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.MinimumValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.TypeValidation;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

class JsonSchemaValidatorFactoryExtTest {

    private final JsonProvider jsonp = JsonProvider.provider();

    private final JsonBuilderFactory jsonFactory = jsonp.createBuilderFactory(emptyMap());

    final JsonObject conf = jsonFactory
            .createObjectBuilder()
            .add("configuration",
                    jsonFactory
                            .createObjectBuilder()
                            .add("commonConfig",
                                    jsonFactory
                                            .createObjectBuilder()
                                            .add("fields", jsonFactory.createArrayBuilder().build())
                                            .add("tableName", "tably")
                                            .add("toto", "tata")
                                            .build())
                            .add("maxRecords", 1000)
                            .add("limit", 100)
                            .build())
            .add("configurationBad",
                    jsonFactory
                            .createObjectBuilder()
                            .add("commonConfig",
                                    jsonFactory
                                            .createObjectBuilder()
                                            .add("fields", jsonFactory.createArrayBuilder().build())
                                            .build())
                            .add("maxRecords", 1009)
                            .add("limit", 100)
                            .build())
            .add("tableName", "problem")
            .add("maxRecords0", 100)
            .add("maxRecords1", -1)
            .add("maxRecords2", 1000)
            .add("maxRecordsBad0", -2)
            .add("maxRecordsBad1", 1001)
            .add("dataStoreOk",
                    jsonFactory
                            .createObjectBuilder()
                            .add("username", "test")
                            .add("password", "test")
                            .add("jdbcUrl", jsonFactory.createObjectBuilder().add("setRawUrl", false).build())
                            .build())
            .add("dataStoreKo",
                    jsonFactory
                            .createObjectBuilder()
                            .add("username", JsonValue.NULL)
                            .add("password", "test")
                            .add("jdbcUrl",
                                    jsonFactory
                                            .createObjectBuilder()
                                            .add("setRawUrl", false)
                                            .add("rawUrl", JsonValue.NULL)
                                            .build())
                            .build())
            .build();

    private ConfigTypeNodes nodes;

    private Ui payload;

    private JsonSchemaValidatorFactoryExt factory;

    private JsonSchema jsonSchema;

    private Jsonb jsonb;

    private JsonSchema schemaEnum;

    private JsonObject jsonEnum;

    private JsonSchema schemaMaxRecords;

    private JsonObject jsonMaxRecords;

    private JsonSchema schemaDataset;

    private JsonObject jsonDataset;

    private JsonSchema schemaDataStore;

    private JsonObject jsonDataStore;

    @BeforeEach
    void setup() throws Exception {
        factory = new JsonSchemaValidatorFactoryExt();
        jsonb = JsonbBuilder.create();
        final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json");
        nodes = jsonb.fromJson(stream, ConfigTypeNodes.class);
        payload = new UiSpecService<>(null, jsonb)
                .convert("test", "en", nodes.getNodes().get("U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl"), null)
                .toCompletableFuture()
                .get();
        jsonSchema = payload.getJsonSchema();
        schemaEnum = jsonSchema
                .getProperties()
                .get("tableDataSet")
                .getProperties()
                .get("commonConfig")
                .getProperties()
                .get("tableName");
        jsonEnum = jsonb.fromJson(jsonb.toJson(schemaEnum), JsonObject.class);
        schemaMaxRecords = jsonSchema.getProperties().get("tableDataSet").getProperties().get("maxRecords");
        jsonMaxRecords = jsonb.fromJson(jsonb.toJson(schemaMaxRecords), JsonObject.class);
        schemaDataset = jsonSchema.getProperties().get("tableDataSet");
        jsonDataset = jsonb.fromJson(jsonb.toJson(schemaDataset), JsonObject.class);
        schemaDataStore = jsonSchema.getProperties().get("dataStore");
        jsonDataStore = jsonb.fromJson(jsonb.toJson(schemaDataStore), JsonObject.class);
    }

    @Test
    void testValidationList() {
        List validations = factory.createDefaultValidations();
        assertEquals(18, validations.size());
        assertEquals(4, validations
                .stream()
                .filter(v -> EnumValidationWithDefaultValue.class.isInstance(v) || MinimumValidation.class.isInstance(v)
                        || MaximumValidation.class.isInstance(v) || TypeValidation.class.isInstance(v))
                .toArray().length);
    }

    @Test
    void testEnumValidation() throws Exception {
        assertEquals(5, schemaEnum.getEnumValues().size());
        final JsonSchemaValidator validator = factory.newInstance(jsonEnum);
        final ValidationResult errors = validator.apply(conf.get("tableName"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testEnumValidationDatasetOk() throws Exception {
        assertEquals(5, schemaEnum.getEnumValues().size());
        final JsonSchemaValidator validator = factory.newInstance(jsonDataset);
        final ValidationResult errors = validator.apply(conf.get("configuration"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testEnumValidationDatasetKo() throws Exception {
        assertEquals(5, schemaEnum.getEnumValues().size());
        schemaDataset.getProperties().get("commonConfig").getProperties().get("tableName").setDefaultValue(null);
        final JsonSchemaValidator validator =
                factory.newInstance(jsonb.fromJson(jsonb.toJson(schemaDataset), JsonObject.class));
        final ValidationResult errors = validator.apply(conf.get("configurationBad"));
        assertEquals(2, errors.getErrors().size());
    }

    @Test
    void testEnumValidationWithNull() throws Exception {
        assertEquals(5, schemaEnum.getEnumValues().size());
        final JsonSchemaValidator validator = factory.newInstance(jsonEnum);
        final ValidationResult errors = validator.apply(conf.get("tableNameNul"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testMaxRecordsOk() throws Exception {
        assertEquals(-1.0, schemaMaxRecords.getDefaultValue());
        final JsonSchemaValidator validator = factory.newInstance(jsonMaxRecords);
        ValidationResult errors = validator.apply(conf.get("maxRecords0"));
        assertEquals(0, errors.getErrors().size());
        errors = validator.apply(conf.get("maxRecords1"));
        assertEquals(0, errors.getErrors().size());
        errors = validator.apply(conf.get("maxRecords2"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testMaxRecordsKo() throws Exception {
        assertEquals(-1.0, schemaMaxRecords.getDefaultValue());
        final JsonSchemaValidator validator = factory.newInstance(jsonMaxRecords);
        ValidationResult errors = validator.apply(conf.get("maxRecordsBad0"));
        assertEquals(1, errors.getErrors().size());
        assertEquals("-2.0 is less than -1.0", errors.getErrors().stream().findFirst().get().getMessage());
        errors = validator.apply(conf.get("maxRecordsBad1"));
        assertEquals("1001.0 is more than 1000.0", errors.getErrors().stream().findFirst().get().getMessage());
        assertEquals(1, errors.getErrors().size());
        errors = validator.apply(conf.get("maxRecordsBad2"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testRequiredOptionOk() throws Exception {
        final JsonSchemaValidator validator = factory.newInstance(jsonDataStore);
        final ValidationResult errors = validator.apply(conf.get("dataStoreOk"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testRequiredOptionKo() throws Exception {
        final JsonSchemaValidator validator = factory.newInstance(jsonDataStore);
        final ValidationResult errors = validator.apply(conf.get("dataStoreKo"));
        assertEquals(2, errors.getErrors().size());
    }

    @Test
    void testPatternOptionOk() throws Exception {
        schemaDataStore.getProperties().get("username").setPattern("/^[a-zA-Z0-9]+$/");
        final JsonSchemaValidator validator =
                factory.newInstance(jsonb.fromJson(jsonb.toJson(schemaDataStore), JsonObject.class));
        final ValidationResult errors = validator.apply(conf.get("dataStoreOk"));
        assertEquals(0, errors.getErrors().size());
    }

    @Test
    void testPatternOptionKo() throws Exception {
        schemaDataStore.getProperties().get("username").setPattern("/^1[a-zA-Z0-9]+$/");
        final JsonSchemaValidator validator =
                factory.newInstance(jsonb.fromJson(jsonb.toJson(schemaDataStore), JsonObject.class));
        final ValidationResult errors = validator.apply(conf.get("dataStoreOk"));
        assertEquals(1, errors.getErrors().size());
        assertEquals("\"test\" doesn't match JavascriptRegex{/^1[a-zA-Z0-9]+$/}",
                errors.getErrors().stream().findFirst().get().getMessage());
    }

}