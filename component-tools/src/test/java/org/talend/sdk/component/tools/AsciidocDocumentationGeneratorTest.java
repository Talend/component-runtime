/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class AsciidocDocumentationGeneratorTest extends GeneratorBase {

    @Test
    void generateAdoc(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".asciidoc");
        new AsciidocDocumentationGenerator(
                new File[] {
                        copyBinaries("org.talend.test.valid", temporaryFolder, info.getTestMethod().get().getName()) },
                output, null, 2, null, null, null, null, log, findWorkDir(), "1.0", Locale.ROOT).run();
        assertTrue(output.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(output))) {
            assertEquals("""
                         //component_start:my

                         == my

                         super my component

                         //configuration_start

                         === Configuration

                         [cols="d,d,m,a,e,d",options="header"]
                         |===
                         |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                         |configuration|configuration configuration|-|Always enabled|configuration|-
                         |input|the input value|def|Always enabled|configuration.input|-
                         |nested|it is nested|-|Always enabled|configuration.nested|dataset
                         |datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore
                         |url|The url|-|Always enabled|configuration.nested.datastore.url|datastore
                         |user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset
                         |===

                         //configuration_end

                         //component_end:my

                         //component_start:my2

                         == my2

                         super my component2

                         //configuration_start

                         === Configuration

                         [cols="d,d,m,a,e,d",options="header"]
                         |===
                         |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                         |ds|ds configuration|-|Always enabled|ds|dataset
                         |datastore|the datastore|-|Always enabled|ds.datastore|datastore
                         |Input|an input|-|Always enabled|ds.datastore.input|datastore
                         |===

                         //configuration_end

                         //component_end:my2
                         """, reader.lines().collect(joining("\n")));
        }
    }

    @Test
    void generateAdocWithDataSetDataStore(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".asciidoc");
        new AsciidocDocumentationGenerator(
                new File[] { copyBinaries("org.talend.test.valid.nestedconfigtypes", temporaryFolder,
                        info.getTestMethod().get().getName()) },
                output, null, 2, null, null, null, null, log, findWorkDir(), "1.0", Locale.ROOT).run();
        assertTrue(output.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(output))) {
            assertEquals(
                    """
                    //component_start:WithNestedConfigTypes

                    == WithNestedConfigTypes

                    super my component

                    //configuration_start

                    === Configuration

                    [cols="d,d,m,a,e,d",options="header"]
                    |===
                    |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                    |configuration|configuration configuration|-|Always enabled|configuration|-
                    |conf With Datase|config with dataset|-|Always enabled|configuration.confWithDataset|-
                    |dataset|dataset configuration|-|Always enabled|configuration.confWithDataset.dataset|dataset
                    |Advanced parameter|Advanced parameter|-|Always enabled|configuration.confWithDataset.dataset.advanced|dataset
                    |config With Datastore|config with datastore|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore|dataset
                    |datastore|...|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore.datastore|datastore
                    |Advanced in datastore|Advanced in datastore|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore.datastore.advanced_ds|datastore
                    |user|the user to log in|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore.datastore.user|datastore
                    |input|the input value|-|Always enabled|configuration.input|-
                    |===

                    //configuration_end

                    //component_end:WithNestedConfigTypes
                    """,
                    reader.lines().collect(joining("\n")));
        }
    }

    @Test
    void generateAdocWithI18n(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".asciidoc");
        new AsciidocDocumentationGenerator(
                new File[] {
                        copyBinaries("org.talend.test.valid", temporaryFolder, info.getTestMethod().get().getName()) },
                output, null, 2, null, null, null, null, log, findWorkDir(), "1.0", new Locale("test")).run();
        assertTrue(output.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(output))) {
            assertEquals("""
                         //component_start:my

                         == my

                         Awesome Doc

                         //configuration_start

                         === Configuration

                         [cols="d,d,m,a,e,d",options="header"]
                         |===
                         |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                         |configuration|configuration configuration|-|Always enabled|configuration|-
                         |input|the input value|def|Always enabled|configuration.input|-
                         |nested|it is nested|-|Always enabled|configuration.nested|dataset
                         |datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore
                         |url|The url|-|Always enabled|configuration.nested.datastore.url|datastore
                         |user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset
                         |===

                         //configuration_end

                         //component_end:my

                         //component_start:my2

                         == my2

                         super my component2

                         //configuration_start

                         === Configuration

                         [cols="d,d,m,a,e,d",options="header"]
                         |===
                         |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                         |ds|ds configuration|-|Always enabled|ds|dataset
                         |datastore|the datastore|-|Always enabled|ds.datastore|datastore
                         |Input|an input|-|Always enabled|ds.datastore.input|datastore
                         |===

                         //configuration_end

                         //component_end:my2
                         """, reader.lines().collect(joining("\n")));
        }
    }

    @Test
    void generateAdocAdvancedConfig(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".asciidoc");
        new AsciidocDocumentationGenerator(
                new File[] { copyBinaries("org.talend.test.configuration", temporaryFolder,
                        info.getTestMethod().get().getName()) },
                output, null, 2, null, null, null, null, log, findWorkDir(), "1.0", Locale.ROOT).run();
        assertTrue(output.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(output))) {
            assertEquals(
                    """
                    //component_start:configurationWithArrayOfObject

                    == configurationWithArrayOfObject

                    //configuration_start

                    === Configuration

                    [cols="d,d,m,a,e,d",options="header"]
                    |===
                    |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                    |configuration|Aggregate fields.|-|Always enabled|configuration|-
                    |groupBy|The list of fields used for the aggregation.|1|Always enabled|configuration.groupBy|-
                    |groupBy[${index}]|groupBy[${index}] configuration|<empty>|Always enabled|configuration.groupBy[${index}]|-
                    |operations|The list of operation that will be executed.|1|Always enabled|configuration.operations|-
                    |fieldPath|The source field path.|<empty>|Always enabled|configuration.operations[${index}].fieldPath|-
                    |operation|The operation to apply.|SUM|Always enabled|configuration.operations[${index}].operation|-
                    |outputFieldPath|The resulting field name.|<empty>|`fieldPath` is equal to `a value` or `anaother value`|configuration.operations[${index}].outputFieldPath|-
                    |===

                    //configuration_end

                    //component_end:configurationWithArrayOfObject
                    """,
                    reader.lines().collect(joining("\n")));
        }
    }

    @Test
    void generateAdocWithConditions(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".asciidoc");
        new AsciidocDocumentationGenerator(
                new File[] { copyBinaries("org.talend.test.activeif", temporaryFolder,
                        info.getTestMethod().get().getName()) },
                output, null, 2, null, null, null, null, log, findWorkDir(), "1.0", Locale.ROOT).run();
        assertTrue(output.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(output))) {
            assertEquals("""
                         //component_start:activeif

                         == activeif

                         //configuration_start

                         === Configuration

                         [cols="d,d,m,a,e,d",options="header"]
                         |===
                         |Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type
                         |configuration|configuration configuration|-|Always enabled|configuration|-
                         |advanced|advanced configuration|false|Always enabled|configuration.advanced|-
                         |advancedOption|advancedOption configuration|-|All of the following conditions are met:

                         - `advanced` is equal to `false`
                         - `query` is empty
                         |configuration.advancedOption|-
                         |query|query configuration|-|All of the following conditions are met:

                         - `toggle` is equal to `true`
                         - `type` is equal to `mysql` or `oracle`
                         |configuration.query|-
                         |toggle|toggle configuration|false|Always enabled|configuration.toggle|-
                         |token|token configuration|-|`toggle` is equal to `true`|configuration.token|-
                         |type|type configuration|-|Always enabled|configuration.type|-
                         |===

                         //configuration_end

                         //component_end:activeif
                         """,
                    reader.lines().collect(joining("\n")));
        }
    }

    @Test
    void generateHtmlPdf(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final String testMethod = info.getTestMethod().get().getName();
        final File output = new File(temporaryFolder, testMethod + ".asciidoc");
        final File outputHtml = new File(temporaryFolder, testMethod + ".html");
        final File outputPdf = new File(temporaryFolder, testMethod + ".pdf");
        new AsciidocDocumentationGenerator(
                new File[] {
                        copyBinaries("org.talend.test.valid", temporaryFolder, info.getTestMethod().get().getName()) },
                output, "SuperTitle", 2, new HashMap<String, String>() {

                    {
                        put("html", outputHtml.getAbsolutePath());
                        put("pdf", outputPdf.getAbsolutePath());
                    }
                }, null, null, null, log, findWorkDir(), "1.0", Locale.ROOT).run();
        assertTrue(outputHtml.exists());
        assertTrue(outputPdf.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(outputHtml))) {
            assertEquals("<!DOCTYPE html>", reader.lines().limit(1).findFirst().get());
        }
    }
}
