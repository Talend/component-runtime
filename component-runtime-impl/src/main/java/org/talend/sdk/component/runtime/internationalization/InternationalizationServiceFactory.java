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
package org.talend.sdk.component.runtime.internationalization;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.sdk.component.api.internationalization.Language;
import org.talend.sdk.component.runtime.reflect.Defaults;

import lombok.RequiredArgsConstructor;

public class InternationalizationServiceFactory {

    public <T> T create(final Class<T> api, final ClassLoader loader) {
        if (!api.isInterface()) {
            throw new IllegalArgumentException(api + " is not an interface");
        }
        if (Stream
                .of(api.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class)
                .anyMatch(m -> m.getReturnType() != String.class)) {
            throw new IllegalArgumentException(api + " methods must return a String");
        }
        if (Stream
                .of(api.getMethods())
                .flatMap(m -> Stream.of(m.getParameters()))
                .anyMatch(p -> p.isAnnotationPresent(Language.class) && p.getType() != Locale.class)) {
            throw new IllegalArgumentException("@Language can only be used with Locales");
        }
        final String pck = api.getPackage().getName();
        return api
                .cast(Proxy
                        .newProxyInstance(loader, new Class<?>[] { api }, new InternationalizedHandler(
                                api.getName() + '.', (pck == null || pck.isEmpty() ? "" : (pck + '.')) + "Messages")));
    }

    @RequiredArgsConstructor
    private static class InternationalizedHandler implements InvocationHandler {

        private static final Object[] NO_ARG = new Object[0];

        private final String prefix;

        private final String messages;

        private final ConcurrentMap<Locale, ResourceBundle> bundles = new ConcurrentHashMap<>();

        private transient final ConcurrentMap<Method, MethodMeta> methods = new ConcurrentHashMap<>();

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.isDefault()) {
                final Class<?> declaringClass = method.getDeclaringClass();
                return Defaults
                        .of(declaringClass)
                        .unreflectSpecial(method, declaringClass)
                        .bindTo(proxy)
                        .invokeWithArguments(args);
            }

            if (Object.class == method.getDeclaringClass()) {
                switch (method.getName()) {
                case "equals":
                    return args != null && args.length == 1 && method.getDeclaringClass().isInstance(args[0])
                            && Proxy.isProxyClass(args[0].getClass()) && this == Proxy.getInvocationHandler(args[0]);
                case "hashCode":
                    return hashCode();
                default:
                    try {
                        return method.invoke(this, args);
                    } catch (final InvocationTargetException ite) {
                        throw ite.getTargetException();
                    }
                }
            }

            final MethodMeta methodMeta = methods
                    .computeIfAbsent(method, m -> new MethodMeta(createLocaleExtractor(m), createParameterFactory(m)));
            final Locale locale = methodMeta.localeExtractor.apply(args);
            final String template = getTemplate(locale, method);
            // note: if we need we could pool message formats but not sure we'll abuse of it
            // that much at runtime yet
            return new MessageFormat(template, locale).format(methodMeta.parameterFactory.apply(args));
        }

        private Function<Object[], Object[]> createParameterFactory(final Method method) {
            final Collection<Integer> included = new ArrayList<>();
            final Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (!parameters[i].isAnnotationPresent(Language.class)) {
                    included.add(i);
                }
            }
            if (included.size() == method.getParameterCount()) {
                return identity();
            }
            if (included.size() == 0) {
                return a -> NO_ARG;
            }
            return args -> {
                final Object[] modified = new Object[included.size()];
                int current = 0;
                for (final int i : included) {
                    modified[current++] = args[i];
                }
                return modified;
            };
        }

        private Function<Object[], Locale> createLocaleExtractor(final Method method) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < method.getParameterCount(); i++) {
                Parameter p = parameters[i];
                if (p.isAnnotationPresent(Language.class)) {
                    final int idx = i;
                    if (String.class == p.getType()) {
                        return params -> new Locale(ofNullable(params[idx]).map(String::valueOf).orElse("en"));
                    }
                    return params -> Locale.class.cast(params[idx]);
                }
            }
            return p -> Locale.getDefault();
        }

        private String getTemplate(final Locale locale, final Method method) {
            final ResourceBundle bundle = bundles
                    .computeIfAbsent(locale,
                            l -> ResourceBundle.getBundle(messages, l, Thread.currentThread().getContextClassLoader()));
            final String key = prefix + method.getName();
            return bundle.containsKey(key) ? bundle.getString(key) : method.getName();
        }
    }

    @RequiredArgsConstructor
    private static class MethodMeta {

        private final Function<Object[], Locale> localeExtractor;

        private final Function<Object[], Object[]> parameterFactory;
    }
}
