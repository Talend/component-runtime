/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit5.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.talend.sdk.component.junit.environment.Environment;
import org.talend.sdk.component.junit.environment.builtin.beam.DirectRunnerEnvironment;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.runtime.manager.ComponentManager;

// this is a bad test cause we have it in the pom in this module but what we must guarantee in terms of lifecycle
@Environment(DirectRunnerEnvironment.class)
@WithComponents("org.talend.sdk.component.test")
class BeamEnvironmentsWithComponentsTest {

    @EnvironmentalTest
    void execute() {
        assertEquals(
                "org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider$AvroRecordBuilderFactory",
                ComponentManager.instance().getRecordBuilderFactoryProvider().apply("test").getClass().getName());
    }
}
