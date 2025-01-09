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
package org.talend.test.valid.schema;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Schema;

@Version
@Icon(FILE_JOB_O)
@Processor(family = "test", name = "schema")
public class MySchema implements Schema, Serializable {

    public MySchema(@Option("configuration") final MyConfig config) {
        // no-op
    }

    @ElementListener
    public JsonObject passthrough(final JsonObject item) {
        return item;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public Schema getElementSchema() {
        return null;
    }

    @Override
    public List<Entry> getEntries() {
        return null;
    }

    @Override
    public List<Entry> getMetadata() {
        return null;
    }

    @Override
    public Stream<Entry> getAllEntries() {
        return null;
    }

    @Override
    public Map<String, String> getProps() {
        return null;
    }

    @Override
    public String getProp(String property) {
        return null;
    }

    public static class MyConfig implements Serializable {

        @Option
        @Documentation("the input value")
        private String input;
    }
}
