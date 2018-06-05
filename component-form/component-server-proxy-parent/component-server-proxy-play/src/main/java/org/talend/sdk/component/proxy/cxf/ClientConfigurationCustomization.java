/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.cxf;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.emptyMap;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import javax.ws.rs.client.Client;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.client.ClientProducer;

public class ClientConfigurationCustomization implements Extension {

    void onProducer(@Observes final ProcessAnnotatedType<ClientProducer> producer) {
        producer
                .configureAnnotatedType()
                .filterMethods(m -> m.getJavaMember().getReturnType() == Client.class
                        && m.getJavaMember().getName().equals("client"))
                .forEach(m -> m.add(new AnnotationLiteral<CxfCustomization>() {
                }));
    }

    @InterceptorBinding
    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    public @interface CxfCustomization {
    }

    @Interceptor
    @CxfCustomization
    @Priority(Interceptor.Priority.APPLICATION)
    public static class CxfCustomizationInterceptor implements Serializable {

        @Inject
        private ProxyConfiguration configuration;

        @AroundInvoke
        public Object around(final InvocationContext context) throws Exception {
            final Bus originalBus = BusFactory.getDefaultBus(false);
            BusFactory.setDefaultBus(new CXFBusFactory().createBus(emptyMap(), createClientConfiguration()));
            try {
                return context.proceed();
            } finally {
                BusFactory.getAndSetThreadDefaultBus(originalBus);
            }
        }

        private Map<String, Object> createClientConfiguration() {
            final Map<String, Object> config = new HashMap<>();
            final Config configSource = ConfigProvider.getConfig();
            Stream
                    .of(AsyncHTTPConduitFactory.class.getDeclaredFields())
                    .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                    .map(f -> {
                        try {
                            if (!f.isAccessible()) {
                                f.setAccessible(true);
                            }
                            return f.get(null);
                        } catch (final IllegalAccessException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(key -> key.startsWith("org.apache.cxf.transport.http.async"))
                    .forEach(key -> configSource
                            .getOptionalValue(configuration.getPrefix() + key, String.class)
                            .ifPresent(val -> config.put(key, val)));
            return config;
        }
    }
}
