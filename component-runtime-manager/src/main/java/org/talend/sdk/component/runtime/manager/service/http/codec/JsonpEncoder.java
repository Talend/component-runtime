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
package org.talend.sdk.component.runtime.manager.service.http.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.service.http.Encoder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JsonpEncoder implements Encoder {

    private final Jsonb jsonb;

    @Override
    public byte[] encode(final Object value) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            jsonb.toJson(value, out);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return outputStream.toByteArray();
    }
}
