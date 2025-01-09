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
package org.talend.sdk.component.runtime.manager.reflect;

import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.Option;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Constructors {

    public static Constructor<?> findConstructor(final Class<?> type) {
        return Stream
                .of(type.getConstructors())
                // we select the constructor with arguments which uses @Option or no-arg
                // constructor if no param construct
                // is matching
                .sorted((c1, c2) -> {
                    final int options1 = Stream
                            .of(c1.getParameters())
                            .mapToInt(p -> p.isAnnotationPresent(Option.class) ? 1 : 0)
                            .sum();
                    final int options2 = Stream
                            .of(c1.getParameters())
                            .mapToInt(p -> p.isAnnotationPresent(Option.class) ? 1 : 0)
                            .sum();
                    if (options1 == options2) {
                        final int paramCount1 = c1.getParameterCount();
                        final int paramCount2 = c2.getParameterCount();
                        return paramCount2 - paramCount1;
                    }
                    return options2 - options1;

                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No constructor usable in " + type.getName()));
    }
}
