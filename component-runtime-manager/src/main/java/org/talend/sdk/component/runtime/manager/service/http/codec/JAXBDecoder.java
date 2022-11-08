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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.talend.sdk.component.api.service.http.Decoder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JAXBDecoder implements Decoder {

    private final Map<Class<?>, JAXBContext> jaxbContexts;

    @Override
    public Object decode(final byte[] value, final Type expectedType) {
        try {
            final Class key = Class.class.cast(expectedType);
            return jaxbContexts
                    .get(key)
                    .createUnmarshaller()
                    .unmarshal(new StreamSource(new ByteArrayInputStream(value)), key)
                    .getValue();
        } catch (final JAXBException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
