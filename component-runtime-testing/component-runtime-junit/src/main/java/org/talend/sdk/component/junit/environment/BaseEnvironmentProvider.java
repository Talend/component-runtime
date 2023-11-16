/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.environment;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.stream.Stream;

public abstract class BaseEnvironmentProvider implements EnvironmentProvider {

    @Override
    public final AutoCloseable start(final Class<?> clazz, final Annotation[] annotations) {
        final AutoCloseable autoCloseable = Stream
                .of(annotations)
                .filter(a -> a.annotationType() == EnvironmentConfigurations.class)
                .findFirst()
                .map(config -> {
                    final EnvironmentConfigurations configurations = EnvironmentConfigurations.class.cast(config);
                    return Stream
                            .of(configurations.value())
                            .filter(c -> c.environment().equals(getName())
                                    || c.environment().equals(getName().replace("Environment", "")))
                            .findFirst()
                            .map(conf -> {
                                final Collection<Runnable> releases = Stream.of(conf.systemProperties()).map(p -> {
                                    final String old = System.getProperty(p.key());
                                    System.setProperty(p.key(), p.value());
                                    return (Runnable) () -> {
                                        if (old == null) {
                                            System.clearProperty(p.key());
                                        } else {
                                            System.clearProperty(p.value());
                                        }
                                    };
                                }).collect(toList());
                                return (AutoCloseable) () -> releases.forEach(Runnable::run);
                            })
                            .orElseGet(() -> () -> {

                            });
                })
                .orElse(() -> {
                });
        final AutoCloseable closeable = doStart(clazz, annotations);
        return () -> {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } finally {
                autoCloseable.close();
            }
        };
    }

    public String getName() {
        return getClass().getSimpleName().replace("Environment", "");
    }

    protected abstract AutoCloseable doStart(Class<?> clazz, Annotation[] annotations);
}
