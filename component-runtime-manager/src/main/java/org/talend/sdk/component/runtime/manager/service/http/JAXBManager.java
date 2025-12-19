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
package org.talend.sdk.component.runtime.manager.service.http;

import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.Headers;
import org.talend.sdk.component.api.service.http.HttpMethod;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.QueryParams;
import org.talend.sdk.component.api.service.http.Url;
import org.talend.sdk.component.runtime.manager.service.http.codec.JAXBDecoder;
import org.talend.sdk.component.runtime.manager.service.http.codec.JAXBEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class JAXB {

    static final boolean ACTIVE;
    static {
        boolean active;
        try {
            ofNullable(JAXB.class.getClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader)
                    .loadClass("jakarta.xml.bind.annotation.XmlType");
            active = true;
        } catch (final ClassNotFoundException e) {
            log.info("JAXB is not available in classloader {}", JAXB.class.getClassLoader());
            active = false;
        }
        ACTIVE = active;
    }
}

// indirection to make it optional
class JAXBManager {

    private volatile Map<Class<?>, JAXBContext> jaxbContexts = new HashMap<>();

    void initJaxbContext(final Method method) {
        Stream
                .concat(of(method.getGenericReturnType()),
                        of(method.getParameters())
                                .filter(p -> of(Path.class, Query.class, Header.class, QueryParams.class, Headers.class,
                                        HttpMethod.class, Url.class).noneMatch(p::isAnnotationPresent))
                                .map(Parameter::getParameterizedType))
                .map(RequestParser::toClassType)
                .filter(Objects::nonNull)
                .filter(cType -> cType.isAnnotationPresent(XmlRootElement.class)
                        || cType.isAnnotationPresent(XmlType.class))
                .forEach(rootElemType -> jaxbContexts.computeIfAbsent(rootElemType, k -> {
                    try {
                        return JAXBContext.newInstance(k);
                    } catch (final JAXBException e) {
                        throw new IllegalStateException(e);
                    }
                }));
    }

    boolean isEmpty() {
        return jaxbContexts.isEmpty();
    }

    JAXBEncoder newEncoder() {
        return new JAXBEncoder(jaxbContexts);
    }

    Decoder newDecoder() {
        return new JAXBDecoder(jaxbContexts);
    }
}
