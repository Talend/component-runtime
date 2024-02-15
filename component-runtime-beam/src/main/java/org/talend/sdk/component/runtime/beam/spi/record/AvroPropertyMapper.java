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
package org.talend.sdk.component.runtime.beam.spi.record;

import org.apache.avro.Schema;

// just an utility to "namespace" our internal properties
public interface AvroPropertyMapper {

    default String readProp(final Schema schema, final String name) {
        return schema.getProp("talend.component." + name);
    }

    default Schema setProp(final Schema schema, final String name, final String value) {
        schema.addProp("talend.component." + name, value);
        return schema;
    }
}
