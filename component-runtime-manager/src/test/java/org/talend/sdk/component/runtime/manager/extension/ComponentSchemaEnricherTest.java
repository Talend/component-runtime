/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.extension;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.schema.FixedSchema;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

class ComponentSchemaEnricherTest {

    private final ComponentMetadataEnricher enricher = new ComponentSchemaEnricher();

    @Test
    void fixedSchemaMetadataPresent() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::ui::schema::fixed", "discover");
            }
        }, enricher.onComponent(ProcessorWithFixedSchema.class, ProcessorWithFixedSchema.class.getAnnotations()));
    }

    @Test
    void fixedSchemaMetadataNotPresent() {
        assertEquals(emptyMap(), enricher.onComponent(MyProcessor.class, MyProcessor.class.getAnnotations()));
    }

    @Processor
    @FixedSchema("discover")
    private static class ProcessorWithFixedSchema {

    }

    @Processor
    private static class MyProcessor {

    }
}
