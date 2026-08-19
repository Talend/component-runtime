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
package org.talend.sdk.component.runtime.record.json;

import java.io.Writer;

import org.talend.sdk.component.api.record.Record;

import lombok.Getter;
import lombok.Setter;

public class OutputRecordHolder extends Writer {

    @Getter
    @Setter
    private Record record;

    @Getter
    @Setter
    private Object data;

    public OutputRecordHolder(final Object data) {
        this.data = data;
    }

    public OutputRecordHolder() {
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        if (len == 0) {
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        // no-op
    }

    @Override
    public void close() {
        flush();
    }
}
