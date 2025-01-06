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
package org.talend.sdk.component.runtime.base;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Serializer {

    public static byte[] toBytes(final Object delegate) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(ofNullable(delegate.getClass().getClassLoader()).orElse(old));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(delegate);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            thread.setContextClassLoader(old);
        }
        return baos.toByteArray();
    }
}
