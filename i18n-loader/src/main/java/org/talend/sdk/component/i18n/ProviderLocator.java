/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.i18n;

import static java.lang.ClassLoader.getSystemClassLoader;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.spi.ResourceBundleControlProvider;

import lombok.Data;

public class ProviderLocator {

    private static final ProviderLocator LOCATOR = new ProviderLocator();

    private final Map<ClassLoader, ResourceBundleControlProvider> providers = new ConcurrentHashMap<>();

    public static ProviderLocator instance() {
        return LOCATOR;
    }

    public Provider current() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl == null) {
            tccl = getSystemClassLoader();
        }
        while (tccl != null) {
            final ResourceBundleControlProvider impl = providers.get(tccl);
            if (impl != null) {
                return new Provider(impl, tccl);
            }
            if (tccl == tccl.getParent()) {
                return null;
            }
            tccl = tccl.getParent();
        }
        return null;
    }

    /**
     * @param loader
     * the loader identifying the current context.
     * @param customProvider
     * the bundle control provider to associate with this loader.
     * @return a callback to call when you don't need the custom provider anymore.
     */
    public Runnable register(final ClassLoader loader, final ResourceBundleControlProvider customProvider) {
        if (providers.putIfAbsent(loader, customProvider) != null) {
            throw new IllegalArgumentException(
                    "A custom ResourceBundleControlProvider is already registered for " + loader);
        }
        return () -> providers.remove(loader, customProvider);
    }

    public Runnable register(final ClassLoader loader, final Predicate<String> baseNameFilter,
            final BiFunction<String, Locale, ResourceBundle> factory) {
        return register(loader, new BaseProvider() {

            @Override
            protected ResourceBundle createBundle(final String baseName, final Locale locale) {
                return factory.apply(baseName, locale);
            }

            @Override
            protected boolean supports(final String baseName) {
                return baseNameFilter.test(baseName);
            }
        });
    }

    @Data
    public static class Provider {

        private final ResourceBundleControlProvider provider;

        private final ClassLoader loader;
    }
}
