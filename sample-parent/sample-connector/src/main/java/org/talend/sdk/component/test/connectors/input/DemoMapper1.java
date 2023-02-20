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
package org.talend.sdk.component.test.connectors.input;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.test.connectors.config.InputConfig;
import org.talend.sdk.component.test.connectors.migration.AbstractMigrationHandler;

@Version(value = InputConfig.INPUT_CONFIG_VERSION,
        migrationHandler = AbstractMigrationHandler.InputMigrationHandler.class)
@Icon(value = Icon.IconType.CUSTOM, custom = "mapper")
@PartitionMapper(name = "DemoMapper1", infinite = false)
@Documentation("Doc: default DemoMapper1 documentation without Internationalization.")
public class DemoMapper1 implements Serializable {

    /*
     * A partition mapper (Input component) requires three methods marked with specific annotations:
     * 1. @Assessor for the evaluating method
     * 2. @Split for the dividing method
     * 3. @Emitter for the Producer factory
     */

    private InputConfig config;

    public DemoMapper1(final @Option("configin") InputConfig config) {

        this.config = config;
    }

    @Assessor
    public long estimateSize() {
        return 1500L;
    }

    @Split
    public List<DemoMapper1> split(@PartitionSize final int desiredNbSplits) {

        return Collections.singletonList(this);
    }

    @Emitter
    public DemoInput1 createSource() {

        return new DemoInput1(this.config);
    }

}
