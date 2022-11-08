/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.customizer;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum Indices {
    BEAM_SDKS_JAVA_CORE("TALEND-INF/classloader/indices/beam-sdks-java-core") {

        @Override
        protected boolean isAvailable() {
            return true;
        }
    },
    SPARK_CORE("TALEND-INF/classloader/indices/spark-core") {

        private volatile Boolean isThere;

        @Override
        protected boolean isAvailable() {
            if (isThere == null) {
                synchronized (this) {
                    if (isThere == null) {
                        isThere = hasClass("org.apache.beam.runners.spark.SparkRunner");
                    }
                }
            }
            return isThere;
        }
    },
    SPARK_STREAMING("TALEND-INF/classloader/indices/spark-streaming") {

        @Override
        protected boolean isAvailable() {
            return SPARK_CORE.isAvailable();
        }
    },
    BEAM_RUNNERS_SPARK("TALEND-INF/classloader/indices/beam-runners-spark-3") {

        @Override
        protected boolean isAvailable() {
            return SPARK_CORE.isAvailable();
        }
    },
    BEAM_RUNNERS_DIRECT_JAVA("TALEND-INF/classloader/indices/beam-runners-direct-java") {

        @Override
        protected boolean isAvailable() {
            return hasClass("org.apache.beam.runners.direct.DirectRunner");
        }
    };

    private final String indexResource;

    private volatile Collection<String> classes;

    protected abstract boolean isAvailable();

    protected boolean hasClass(final String name) {
        try {
            ofNullable(Indices.class.getClassLoader()).orElseGet(ClassLoader::getSystemClassLoader).loadClass(name);
            return true;
        } catch (final NoClassDefFoundError | ClassNotFoundException cnfe) {
            return false;
        }
    }

    Stream<String> getClasses() {
        if (classes != null) {
            return classes.stream();
        }
        synchronized (this) {
            if (classes != null) {
                return classes.stream();
            }

            final ClassLoader classLoader =
                    ofNullable(Indices.class.getClassLoader()).orElseGet(ClassLoader::getSystemClassLoader);
            try (final BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    ofNullable(classLoader)
                                            .map(loader -> ofNullable(loader.getResourceAsStream(indexResource))
                                                    .orElseGet(() -> loader
                                                            .getResourceAsStream(indexResource + ".overriden")))
                                            .orElseGet(() -> new ByteArrayInputStream(new byte[0])),
                                    StandardCharsets.UTF_8))) {
                return (classes = reader
                        .lines()
                        .map(String::trim)
                        .filter(it -> !it.startsWith("#") && !it.isEmpty())
                        .collect(toList())).stream();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
