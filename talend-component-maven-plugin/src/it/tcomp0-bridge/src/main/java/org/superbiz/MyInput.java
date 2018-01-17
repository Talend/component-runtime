/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.superbiz;

import static java.util.Collections.singletonList;
import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.Split;

@Version
@Icon(FILE_JOB_O)
@PartitionMapper(family = "test", name = "input")
public class MyInput implements Serializable {
    private Configuration configuration;

    public MyInput(@Option("config") final Configuration configuration) {
        this.configuration = configuration;
    }

    @Assessor
    public long estimateSize() {
        return 1;
    }

    @Split
    public List<MyInput> split() {
        return singletonList(this);
    }

    @Emitter
    public SomeInput createInput() {
        return new SomeInput(configuration);
    }

    public static class SomeInput implements Serializable {
        private int remaining = 5;

        public SomeInput(final Configuration configuration) {
            // no-op
        }

        @Producer
        public Data next() {
            if (remaining-- == 0) {
                return null;
            }
            final Data data = new Data();
            data.value = new Date().toString();
            return data;
        }
    }

    public static class Data {
        private String value;

        public String getValue() {
            return value;
        }
    }

    @DataSet
    public static class Configuration {
        @Option("name")
        private String name;
    }
}
