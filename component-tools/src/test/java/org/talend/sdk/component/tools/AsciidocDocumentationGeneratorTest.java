/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
            assertEquals("//component_start:my\n" + "\n" + "== my\n" + "\n" + "super my component\n" + "\n"
                    + "//configuration_start\n" + "\n" + "=== Configuration\n" + "\n"
                    + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                    + "|input|the input value|def|Always enabled|configuration.input|-\n"
                    + "|nested|it is nested|-|Always enabled|configuration.nested|dataset\n"
                    + "|datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore\n"
                    + "|url|The url|-|Always enabled|configuration.nested.datastore.url|datastore\n"
                    + "|user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset\n" + "|===\n"
                    + "\n" + "//configuration_end\n" + "\n" + "//component_end:my\n" + "\n" + "//component_start:my2\n"
                    + "\n" + "== my2\n" + "\n" + "super my component2\n" + "\n" + "//configuration_start\n" + "\n"
                    + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|ds|ds configuration|-|Always enabled|ds|dataset\n"
                    + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n"
                    + "|Input|an input|-|Always enabled|ds.datastore.input|datastore\n" + "|===\n" + "\n"
                    + "//configuration_end\n" + "\n" + "//component_end:my2\n", reader.lines().collect(joining("\n")));
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
            assertEquals("//component_start:WithNestedConfigTypes\n" + "\n" + "== WithNestedConfigTypes\n" + "\n"
                    + "super my component\n" + "\n" + "//configuration_start\n" + "\n" + "=== Configuration\n" + "\n"
                    + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                    + "|conf With Datase|config with dataset|-|Always enabled|configuration.confWithDataset|-\n"
                    + "|dataset|dataset configuration|-|Always enabled|configuration.confWithDataset.dataset|dataset\n"
                    + "|Advanced parameter|Advanced parameter|-|Always enabled|configuration.confWithDataset.dataset.advanced|dataset\n"
                    + "|config With Datastore|config with datastore|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore|dataset\n"
                    + "|datastore|...|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore.datastore|datastore\n"
                    + "|Advanced in datastore|Advanced in datastore|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore.datastore.advanced_ds|datastore\n"
                    + "|user|the user to log in|-|Always enabled|configuration.confWithDataset.dataset.configWithDatastore.datastore.user|datastore\n"
                    + "|input|the input value|-|Always enabled|configuration.input|-\n" + "|===\n" + "\n"
                    + "//configuration_end\n" + "\n" + "//component_end:WithNestedConfigTypes\n",
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
            assertEquals("//component_start:my\n" + "\n" + "== my\n" + "\n" + "Awesome Doc\n" + "\n"
                    + "//configuration_start\n" + "\n" + "=== Configuration\n" + "\n"
                    + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                    + "|input|the input value|def|Always enabled|configuration.input|-\n"
                    + "|nested|it is nested|-|Always enabled|configuration.nested|dataset\n"
                    + "|datastore|the datastore|-|Always enabled|configuration.nested.datastore|datastore\n"
                    + "|url|The url|-|Always enabled|configuration.nested.datastore.url|datastore\n"
                    + "|user|the user to log in|unknown|Always enabled|configuration.nested.user|dataset\n" + "|===\n"
                    + "\n" + "//configuration_end\n" + "\n" + "//component_end:my\n" + "\n" + "//component_start:my2\n"
                    + "\n" + "== my2\n" + "\n" + "super my component2\n" + "\n" + "//configuration_start\n" + "\n"
                    + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|ds|ds configuration|-|Always enabled|ds|dataset\n"
                    + "|datastore|the datastore|-|Always enabled|ds.datastore|datastore\n"
                    + "|Input|an input|-|Always enabled|ds.datastore.input|datastore\n" + "|===\n" + "\n"
                    + "//configuration_end\n" + "\n" + "//component_end:my2\n", reader.lines().collect(joining("\n")));
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
            assertEquals("//component_start:configurationWithArrayOfObject\n" + "\n"
                    + "== configurationWithArrayOfObject\n" + "\n" + "//configuration_start\n" + "\n"
                    + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|configuration|Aggregate fields.|-|Always enabled|configuration|-\n"
                    + "|groupBy|The list of fields used for the aggregation.|1|Always enabled|configuration.groupBy|-\n"
                    + "|groupBy[${index}]|groupBy[${index}] configuration|<empty>|Always enabled|configuration.groupBy[${index}]|-\n"
                    + "|operations|The list of operation that will be executed.|1|Always enabled|configuration.operations|-\n"
                    + "|fieldPath|The source field path.|<empty>|Always enabled|configuration.operations[${index}].fieldPath|-\n"
                    + "|operation|The operation to apply.|SUM|Always enabled|configuration.operations[${index}].operation|-\n"
                    + "|outputFieldPath|The resulting field name.|<empty>|`fieldPath` is equal to `a value` or `anaother value`|configuration.operations[${index}].outputFieldPath|-\n"
                    + "|===\n" + "\n" + "//configuration_end\n" + "\n"
                    + "//component_end:configurationWithArrayOfObject\n", reader.lines().collect(joining("\n")));
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
            assertEquals("//component_start:activeif\n" + "\n" + "== activeif\n" + "\n" + "//configuration_start\n"
                    + "\n" + "=== Configuration\n" + "\n" + "[cols=\"d,d,m,a,e,d\",options=\"header\"]\n" + "|===\n"
                    + "|Display Name|Description|Default Value|Enabled If|Configuration Path|Configuration Type\n"
                    + "|configuration|configuration configuration|-|Always enabled|configuration|-\n"
                    + "|advanced|advanced configuration|false|Always enabled|configuration.advanced|-\n"
                    + "|advancedOption|advancedOption configuration|-|All of the following conditions are met:\n" + "\n"
                    + "- `advanced` is equal to `false`\n" + "- `query` is empty\n"
                    + "|configuration.advancedOption|-\n"
                    + "|query|query configuration|-|All of the following conditions are met:\n" + "\n"
                    + "- `toggle` is equal to `true`\n" + "- `type` is equal to `mysql` or `oracle`\n"
                    + "|configuration.query|-\n"
                    + "|toggle|toggle configuration|false|Always enabled|configuration.toggle|-\n"
                    + "|token|token configuration|-|`toggle` is equal to `true`|configuration.token|-\n"
                    + "|type|type configuration|-|Always enabled|configuration.type|-\n" + "|===\n" + "\n"
                    + "//configuration_end\n" + "\n" + "//component_end:activeif\n",
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
