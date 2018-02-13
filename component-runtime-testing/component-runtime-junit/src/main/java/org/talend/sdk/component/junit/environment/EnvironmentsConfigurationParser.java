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
package org.talend.sdk.component.junit.environment;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class EnvironmentsConfigurationParser {

    private final Collection<EnvironmentProvider> environments;

    private final boolean parallel;

    public EnvironmentsConfigurationParser(final Class<?> testClass) {
        final Optional<Environments> config = ofNullable(testClass.getAnnotation(Environments.class));
        environments =
                Stream
                        .concat(config.map(Environments::value).map(Stream::of).orElseGet(Stream::empty),
                                ofNullable(testClass.getAnnotation(Environment.class)).map(Stream::of).orElseGet(
                                        Stream::empty))
                        .map(e -> {
                            try {
                                return e.value().getConstructor().newInstance();
                            } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                                throw new IllegalStateException(ex);
                            } catch (final InvocationTargetException ex) {
                                throw new IllegalStateException(ex.getTargetException());
                            }
                        })
                        .map(DecoratingEnvironmentProvider::new)
                        .collect(toList());
        parallel = config.map(Environments::parallel).orElse(false);
    }

    public Stream<EnvironmentProvider> stream() {
        final Stream<EnvironmentProvider> stream = environments.stream();
        if (parallel) {
            return stream.parallel();
        }
        return stream;
    }
}
