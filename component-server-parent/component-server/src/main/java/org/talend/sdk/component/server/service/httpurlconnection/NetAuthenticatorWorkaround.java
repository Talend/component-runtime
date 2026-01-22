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
package org.talend.sdk.component.server.service.httpurlconnection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class NetAuthenticatorWorkaround {

    @Inject
    private NetAuthenticatorController controller;

    private volatile Authenticator original;

    @PostConstruct
    private void init() {
        original = getAuthenticator();
        Authenticator.setDefault(new ApplicationAuthenticator(original, controller));
    }

    public void lazyInit() {
        // no-op
    }

    @PreDestroy
    private void destroy() {
        Authenticator.setDefault(original);
    }

    private Authenticator getAuthenticator() {
        return Stream
                .of(Authenticator.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isPrivate(f.getModifiers())
                        && f.getType() == Authenticator.class)
                .findFirst()
                .map(f -> {
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                    try {
                        return Authenticator.class.cast(f.get(null));
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .orElse(null);
    }

    private static class ApplicationAuthenticator extends Authenticator {

        private final Authenticator delegate;

        private final NetAuthenticatorController controller;

        private final Method getRequestorType;

        private final Method getPasswordAuthentication;

        private final Method getRequestingURL;

        private ApplicationAuthenticator(final Authenticator original, final NetAuthenticatorController controller) {
            this.getPasswordAuthentication = findMethod("getPasswordAuthentication");
            this.getRequestingURL = findMethod("getRequestingURL");
            this.getRequestorType = findMethod("getRequestorType");
            this.delegate = original;
            this.controller = controller;
        }

        private Method findMethod(final String name) {
            final Method declaredMethod;
            try {
                declaredMethod = Authenticator.class.getDeclaredMethod(name);
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            if (!declaredMethod.isAccessible()) {
                declaredMethod.setAccessible(true);
            }
            return declaredMethod;
        }

        private boolean shouldSkip() {
            return delegate == null || controller.isSkipped();
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            try {
                return shouldSkip() ? null
                        : PasswordAuthentication.class.cast(getPasswordAuthentication.invoke(delegate));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }

        @Override
        public URL getRequestingURL() {
            try {
                return shouldSkip() ? null : URL.class.cast(getRequestingURL.invoke(delegate));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }

        @Override
        public RequestorType getRequestorType() {
            try {
                return shouldSkip() ? null : RequestorType.class.cast(getRequestorType.invoke(delegate));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }
    }
}
