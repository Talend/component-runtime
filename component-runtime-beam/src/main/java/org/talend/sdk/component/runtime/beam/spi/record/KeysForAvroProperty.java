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
package org.talend.sdk.component.runtime.beam.spi.record;

public interface KeysForAvroProperty {

    String LABEL = "talend.component.label";

    // alias that indicate field is metadata if present.
    String METADATA_ALIAS_NAME = "talend.field.__METADATA__";

    String IS_ERROR_CAPABLE = "talend.component.record.entry.errorCapable";

    String RECORD_IN_ERROR = "talend.component.record.value.on.error";
}
