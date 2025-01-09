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
package org.talend.sdk.component.server.service;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.path.PathFactory;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@MonoMeecrowaveConfig
class VirtualDependenciesServiceTemplatingTest {

    @ParameterizedTest
    @MethodSource("replaceGavConfigurationSource")
    void replaceGav(final Pair config) {
        assertEquals(config.expected,
                new VirtualDependenciesService()
                        .replaceByGav("Foo Plugin", config.input,
                                singletonMap(new Artifact("foo", "bar", "jar", "", "any", "test"), PathFactory
                                        .get(System.getProperty("talend.component.server.user.extensions.location"))
                                        .resolve("component-with-user-jars/bar.ajr"))));
    }

    private static Stream<Pair> replaceGavConfigurationSource() {
        return Stream
                .of(new Pair("a=b\nc=d", "a=b\nc=d"),
                        new Pair("a=userJar(dummy)\nc=d",
                                "a=virtual.talend.component.server.generated.foo_plugin:dummy:jar:unknown\nc=d"),
                        new Pair("a=b\nc=userJar(dummy)",
                                "a=b\nc=virtual.talend.component.server.generated.foo_plugin:dummy:jar:unknown"),
                        new Pair("a=b\n" + "c=userJar(dummy)\n" + "another[0]=userJar(other)", "a=b\n"
                                + "c=virtual.talend.component.server.generated.foo_plugin:dummy:jar:unknown\n"
                                + "another[0]=virtual.talend.component.server.generated.foo_plugin:other:jar:unknown"));
    }

    @ToString
    @RequiredArgsConstructor
    private static class Pair {

        private final String input;

        private final String expected;
    }
}
