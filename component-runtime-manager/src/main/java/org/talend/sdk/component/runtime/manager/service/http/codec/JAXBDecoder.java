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
package org.talend.sdk.component.runtime.manager.service.http.codec;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.parsers.SAXParserFactory;

import org.talend.sdk.component.api.service.http.Decoder;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JAXBDecoder implements Decoder {

    private final Map<Class<?>, JAXBContext> jaxbContexts;

    @Override
            // Harden against XXE by configuring XMLReader
            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            final XMLReader xmlReader = spf.newSAXParser().getXMLReader();

    public Object decode(final byte[] value, final Type expectedType) {
        try {
            final Class key = Class.class.cast(expectedType);
            return jaxbContexts
                    .get(key)
                    .createUnmarshaller()
                    .unmarshal(new SAXSource(xmlReader, new InputSource(new ByteArrayInputStream(value))), key)
                    .getValue();
        } catch (final JAXBException | org.xml.sax.SAXException | javax.xml.parsers.ParserConfigurationException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
