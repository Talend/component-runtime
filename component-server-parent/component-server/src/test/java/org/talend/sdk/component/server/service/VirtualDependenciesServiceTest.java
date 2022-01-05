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
package org.talend.sdk.component.server.service;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.jar.JarFile;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.server.front.model.Dependencies;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.test.ComponentClient;

@MonoMeecrowaveConfig
class VirtualDependenciesServiceTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Test
    void getDependencies() {
        final String compId = client.getComponentId("custom", "noop");
        final Dependencies dependencies = base
                .path("component/dependencies")
                .queryParam("identifier", compId)
                .request(APPLICATION_JSON_TYPE)
                .get(Dependencies.class);
        assertEquals(1, dependencies.getDependencies().size());
        final DependencyDefinition definition = dependencies.getDependencies().get(compId);
        assertEquals(2, definition.getDependencies().size());
        assertEquals(Stream
                .of("virtual.talend.component.server.generated.component-with-user-jars:user-extension:jar:",
                        "virtual.talend.component.server.generated.component-with-user-jars:user-local-configuration-component-with-user-jars:jar:")
                .collect(toSet()),
                definition
                        .getDependencies()
                        .stream()
                        .map(it -> it.substring(0, it.lastIndexOf(':') + 1))
                        .collect(toSet()));
    }

    @Test
    void getVirtualDependencies(final TestInfo info, @TempDir final File folder) {
        final BiFunction<JarFile, String, Properties> readProperties = (jar, resource) -> {
            try {
                final InputStream inputStream = jar.getInputStream(jar.getEntry(resource));
                assertNotNull(inputStream);
                final Properties p = new Properties();
                p.load(inputStream);
                inputStream.close();
                return p;
            } catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        };

        { // user jar
            final File file = downloadJar(info, folder, getGav("user-extension"));
            try (final JarFile jar = new JarFile(file)) {
                final Properties p = readProperties
                        .apply(jar, "TALEND-INF/org.talend.sdk.component.server.test.custom.CustomService.properties");
                assertEquals(props("i.am", "in the classpath", "and.was.added", "by the user"), p);
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        }
        { // user config
            final File file = downloadJar(info, folder, getGav("user-local-configuration-component-with-user-jars"));
            try (final JarFile jar = new JarFile(file)) {
                final Properties p = readProperties.apply(jar, "TALEND-INF/local-configuration.properties");
                assertEquals(props("i.m.a.virtual.configuration.entry", "Yes I am!",
                        "i.m.another.virtual.configuration.entry", "Yes I am too!"), p);
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    void ensureActionUseVirtualClasspath() {
        final Response error = base
                .path("action/execute")
                .queryParam("type", "suggestions")
                .queryParam("family", "custom")
                .queryParam("action", "forTests")
                .request(APPLICATION_JSON_TYPE)
                .post(Entity.entity(emptyMap(), APPLICATION_JSON_TYPE));
        assertEquals(200, error.getStatus());
        final SuggestionValues result = error.readEntity(SuggestionValues.class);
        assertEquals(
                map("i.am", "in the classpath", "and.was.added", "by the user", "i.m.a.virtual.configuration.entry",
                        "Yes I am!", "i.m.another.virtual.configuration.entry", "Yes I am too!"),
                result
                        .getItems()
                        .stream()
                        .collect(toMap(SuggestionValues.Item::getId, SuggestionValues.Item::getLabel)));
    }

    private String getGav(final String artifact) {
        return base
                .path("component/dependencies")
                .queryParam("identifier", client.getComponentId("custom", "noop"))
                .request(APPLICATION_JSON_TYPE)
                .get(Dependencies.class)
                .getDependencies()
                .values()
                .stream()
                .flatMap(it -> it.getDependencies().stream())
                .filter(it -> it.contains(':' + artifact + ':'))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private Map<String, String> map(final String... kv) {
        return IntStream.range(0, kv.length / 2).boxed().reduce(new HashMap<>(), (p, i) -> {
            p.put(kv[i * 2], kv[i * 2 + 1]);
            return p;
        }, (p1, p2) -> p1);
    }

    private Properties props(final String... kv) {
        return IntStream.range(0, kv.length / 2).boxed().reduce(new Properties(), (p, i) -> {
            p.setProperty(kv[i * 2], kv[i * 2 + 1]);
            return p;
        }, (p1, p2) -> p1);
    }

    private File downloadJar(final TestInfo info, final File folder, final String id) {
        final InputStream stream = base
                .path("component/dependency/{id}")
                .resolveTemplate("id", id)
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .get(InputStream.class);
        final File file = new File(folder, info.getTestMethod().get().getName() + ".jar");
        try (final OutputStream outputStream = new FileOutputStream(file)) {
            IO.copy(stream, outputStream);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return file;
    }
}
