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
package org.talend.sdk.component.starter.server.test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.Meecrowave;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;

public class Client {

    @Target(TYPE)
    @Retention(RUNTIME)
    @ExtendWith(ClientRuleExtension.class)
    public @interface Active {
    }

    public static class ClientRuleExtension implements BeforeAllCallback, AfterAllCallback, JUnit5InjectionSupport {

        private static final ExtensionContext.Namespace NAMESPACE =
                ExtensionContext.Namespace.create(ClientRuleExtension.class.getName());

        @Override
        public void afterAll(final ExtensionContext context) {
            javax.ws.rs.client.Client.class
                    .cast(context.getStore(NAMESPACE).get(javax.ws.rs.client.Client.class.getName()))
                    .close();
        }

        @Override
        public void beforeAll(final ExtensionContext context) {
            final javax.ws.rs.client.Client client = ClientBuilder.newClient();
            context.getStore(NAMESPACE).put(javax.ws.rs.client.Client.class.getName(), client);
            context.getStore(NAMESPACE).put(WebTarget.class.getName(), target(client));
        }

        private WebTarget target(final javax.ws.rs.client.Client client) {
            final Meecrowave.Builder config = CDI.current().select(Meecrowave.Builder.class).get();
            return client.target("http://localhost:" + config.getHttpPort() + "/api");
        }

        @Override
        public boolean supports(final Class<?> type) {
            return WebTarget.class == type || javax.ws.rs.client.Client.class == type;
        }

        @Override
        public Object findInstance(final ExtensionContext extensionContext, final Class<?> type) {
            return extensionContext.getStore(NAMESPACE).get(type.getName());
        }

        @Override
        public Class<? extends Annotation> injectionMarker() {
            return Override.class; // ignored actually
        }
    }
}
