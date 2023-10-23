/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanTaskTest {

    @Test
    void run(@TempDir final File output) throws Exception {
        final File out = new File(output, "my.props");
        new ScanTask(singletonList(new File("target/test-classes")),
                singletonList("org.talend.test.valid.update.UpdateService"), singletonList("org.talend.test.valid.*"),
                "include-exclude", out).run();
        final Properties properties = new Properties();
        try (final InputStream stream = new FileInputStream(out)) {
            properties.load(stream);
        }
        assertEquals(1, properties.size());
        assertEquals("org.talend.test.valid.MyComponent,"
                + "org.talend.test.valid.MyInternalization,org.talend.test.valid.MySource,"
                + "org.talend.test.valid.customicon.MyComponent," + "org.talend.test.valid.customiconapi.MyComponent,"
                + "org.talend.test.valid.datasetassourceconfig.MyComponent,"
                + "org.talend.test.valid.datasetintwosourceswithonewithadditionalrequired.MyComponent,"
                + "org.talend.test.valid.datasetintwosourceswithonewithadditionalrequired.MyComponent2,"
                + "org.talend.test.valid.datastore.MyService,"
                + "org.talend.test.valid.exceptions.ValidComponentExceptionService,"
                + "org.talend.test.valid.localconfiguration.MyComponent,"
                + "org.talend.test.valid.nestedconfigtypes.WithNestedConfigTypes,"
                + "org.talend.test.valid.nesteddataset.MyComponent,"
                + "org.talend.test.valid.record.MyTestRecord,"
                + "org.talend.test.valid.schema.MySchema,"
                + "org.talend.test.valid.structure.MyComponentWithStructure,"
                + "org.talend.test.valid.update.Comp,org.talend.test.valid.wording.MyComponent",
                properties.getProperty("classes.list"));
    }
}
