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
package org.talend.runtime.documentation;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.text.StringSubstitutor;
import org.apache.meecrowave.Meecrowave;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class ApiMockUpdate {

    public static void main(final String[] args) throws Exception {
        if (Boolean.parseBoolean(System.getenv("APIMOCK_SKIP"))) {
            log.warn("Api mock update skipped");
            return;
        }
        doMain(new File(args[0]), new File(args[1]));
    }

    private static void doMain(final File root, final File apiPath)
            throws IOException, ExecutionException, InterruptedException {
        System.setProperty("talend.component.manager.m2.repository", createM2WithComponents(root).getAbsolutePath());
        System.setProperty("talend.component.server.component.coordinates", "org.talend.demo:components:1.0.0");
        updateMocks(apiPath);
    }

    private static void updateMocks(final File apiPath)
            throws ExecutionException, InterruptedException, UnknownHostException {
        try (final Meecrowave server = new Meecrowave(new Meecrowave.Builder() {

            {
                randomHttpPort();
                setScanningExcludes(
                        "classworlds,container,freemarker,zipkin,backport,commons,component-form,component-runtime-junit,"
                                + "component-tools,crawler,doxia,exec,jsch,jcl,org.osgi,talend-component");
                setScanningPackageExcludes(
                        "org.talend.sdk.component.proxy,org.talend.sdk.component.runtime.server.vault,org.talend.sdk.component.server.vault.proxy");
            }
        }).bake()) {
            captureMocks(format("http://%s:%d", InetAddress.getLocalHost().getHostName(),
                    server.getConfiguration().getHttpPort()), apiPath);
        }
        log.warn("[updateMocks] finished.");
    }

    private static File createM2WithComponents(final File root) {
        final File target = new File(root, ".m2/repository");
        final File components = new File(root, ".m2/repository/org/talend/demo/components/1.0.0/components-1.0.0.jar");
        components.getParentFile().mkdirs();
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(components))) {
            final Path base = jarLocation(ApiMockUpdate.class).toPath();
            Files.walkFileTree(base, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    final String className = base.relativize(file).toString();
                    outputStream.putNextEntry(new JarEntry(className));
                    Files.copy(file, outputStream);
                    return CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return target;
    }

    private static void captureMocks(final String target, final File output)
            throws ExecutionException, InterruptedException {
        final String familyId = "Y29tcG9uZW50cyNNb2Nr";
        final String componentId = "Y29tcG9uZW50cyNNb2NrI01vY2tJbnB1dA";
        final String configurationId = "Y29tcG9uZW50cyNNb2NrI2RhdGFzZXQjdGFibGU";

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Map<String, byte[]> files = new HashMap<>();
        try {
            CompletableFuture
                    .allOf(
                            // env
                            capture(files, executor, "/api/v1/environment", target, emptyMap(),
                                    t -> t.request(APPLICATION_JSON_TYPE).get(byte[].class)),
                            // action
                            capture(files, executor, "/api/v1/action/index", target, emptyMap(),
                                    t -> t.request(APPLICATION_JSON_TYPE).get(byte[].class)),
                            //
                            capture(files, executor, "/api/v1/action/execute", target, emptyMap(), t -> t
                                    .queryParam("family", "Mock")
                                    .queryParam("type", "healthcheck")
                                    .queryParam("action", "basicAuth")
                                    .request(APPLICATION_JSON_TYPE)
                                    .post(entity("{\"url\":\"http://localhost:51234/unavailable\",\"username\":\"xxx\","
                                            + "\"password\":\"yyy\"}", APPLICATION_JSON_TYPE), byte[].class)),

                            // component
                            capture(files, executor, "/api/v1/component/index", target, emptyMap(),
                                    t -> t.request(APPLICATION_JSON_TYPE).get(byte[].class)),

                            capture(files, executor, "/api/v1/component/details", target, emptyMap(),
                                    t -> t
                                            .queryParam("identifiers", componentId)
                                            .request(APPLICATION_JSON_TYPE)
                                            .get(byte[].class)),

                            capture(files, executor, "/api/v1/component/dependencies", target, emptyMap(),
                                    t -> t
                                            .queryParam("identifiers", componentId)
                                            .request(APPLICATION_JSON_TYPE)
                                            .get(byte[].class)),
                            capture(files, executor, "/api/v1/component/dependency/{id}", target,
                                    map("id", componentId),
                                    t -> t.request(APPLICATION_OCTET_STREAM_TYPE).get(byte[].class)),
                            capture(files, executor, "/api/v1/component/icon/{id}", target, map("id", componentId),
                                    t -> t.request(APPLICATION_OCTET_STREAM_TYPE).get(byte[].class)),
                            capture(files, executor, "/api/v1/component/icon/family/{id}", target, map("id", familyId),
                                    t -> t.request(APPLICATION_OCTET_STREAM_TYPE).get(byte[].class)),
                            capture(files, executor, "/api/v1/component/migrate/{id}/{configurationVersion}", target,
                                    map("id", componentId, "configurationVersion", "1"),
                                    t -> t
                                            .request(APPLICATION_JSON_TYPE)
                                            .post(entity("{}", APPLICATION_JSON_TYPE), byte[].class)),

                            // configuration type
                            capture(files, executor, "/api/v1/configurationtype/index", target, emptyMap(),
                                    t -> t.request(APPLICATION_JSON_TYPE).get(byte[].class)),

                            capture(files, executor, "/api/v1/configurationtype/details", target, emptyMap(),
                                    t -> t
                                            .queryParam("identifiers", configurationId)
                                            .request(APPLICATION_JSON_TYPE)
                                            .get(byte[].class)),

                            capture(files, executor, "/api/v1/configurationtype/migrate/{id}/{configurationVersion}",
                                    target, map("id", configurationId, "configurationVersion", "1"),
                                    t -> t
                                            .request(APPLICATION_JSON_TYPE)
                                            .post(entity("{}", APPLICATION_JSON_TYPE), byte[].class)))
                    .get(10, MINUTES);
        } catch (final TimeoutException e) {
            log.error(e.getMessage(), e);
        }
        executor.shutdown();
        log.info("Updating {}", files.keySet());
        files.forEach((filePath, data) -> {
            final File apiResult = new File(output, filePath);
            apiResult.getParentFile().mkdirs();
            try {
                log.info("Storing {}", filePath);
                Files.write(apiResult.toPath(), data);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
            log.info("Stored {}", filePath);
        });
        executor.shutdownNow(); // ensure it is off before exiting the method
    }

    private static Map<String, String> map(final String... kv) {
        final Map<String, String> out = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put(kv[i], kv[i + 1]);
        }
        return out;
    }

    private static CompletableFuture<Void> capture(final Map<String, byte[]> files, final Executor executor,
            final String path, final String base, final Map<String, String> templates,
            final Function<WebTarget, byte[]> target) {
        return CompletableFuture.runAsync(() -> {
            final String outputPath = new StringSubstitutor(templates, "{", "}").replace(path);
            log.info("Trying to grab {}", outputPath);
            final Client client = ClientBuilder.newClient();
            try {
                WebTarget webTarget = client.target(base).path(path);
                for (final Map.Entry<String, String> tpl : templates.entrySet()) {
                    webTarget = webTarget.resolveTemplate(tpl.getKey(), tpl.getValue());
                }
                webTarget.property("http.connection.timeout", 30000L).property("http.receive.timeout", 60000L);
                files.put(outputPath, target.apply(webTarget));
                log.info("Grabbed to grab {}", outputPath);
            } catch (final ProcessingException | WebApplicationException ex) {
                log.error("Error on {}", outputPath, ex);
                throw ex;
            } finally {
                client.close();
            }
        }, executor);
    }
}
