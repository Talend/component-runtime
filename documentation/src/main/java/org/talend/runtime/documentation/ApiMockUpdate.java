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
package org.talend.runtime.documentation;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.io.ByteArrayInputStream;
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
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.stream.Stream;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
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
        if (args.length < 3 || args[1] == null) {
            log.warn("No credentials, skipping mock update");
            return;
        }
        doMain(args, new File(args[0]));
    }

    private static void doMain(final String[] args, final File output)
            throws IOException, ExecutionException, InterruptedException {
        System.setProperty("talend.component.manager.m2.repository", createM2WithComponents(output).getAbsolutePath());
        System.setProperty("talend.component.server.component.coordinates", "org.talend.demo:components:1.0.0");

        final FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(60000);
        try {
            ftp.connect(args[3]);
            ftp.setSoTimeout(60000);
            ftp.setDataTimeout(60000);
            ftp.setControlKeepAliveTimeout(60000);
            ftp.setControlKeepAliveReplyTimeout(60000);
            final int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return;
            }
            if (!ftp.login(args[1], args[2])) {
                ftp.disconnect();
                throw new IllegalArgumentException("Invalid credentials (" + args[1] + ")");
            }
            updateMocks(ftp);
            ftp.logout();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }
    }

    private static void updateMocks(final FTPClient ftp)
            throws ExecutionException, InterruptedException, UnknownHostException {
        try (final Meecrowave server = new Meecrowave(new Meecrowave.Builder() {

            {
                randomHttpPort();
                setScanningExcludes(
                        "classworlds,container,freemarker,zipkin,backport,commons,component-form,component-runtime-junit,"
                                + "component-tools,crawler,doxia,exec,jsch,jcl,org.osgi,talend-component");
                setScanningPackageExcludes("org.talend.sdk.component.proxy");
            }
        }).bake()) {
            captureMocks(format("http://%s:%d", InetAddress.getLocalHost().getHostName(),
                    server.getConfiguration().getHttpPort()), ftp);
        }
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

    private static void captureMocks(final String target, final FTPClient ftp)
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
                                    t -> t.queryParam("identifiers", componentId).request(APPLICATION_JSON_TYPE).get(
                                            byte[].class)),
                            capture(files, executor, "/api/v1/component/dependencies", target, emptyMap(),
                                    t -> t.queryParam("identifiers", componentId).request(APPLICATION_JSON_TYPE).get(
                                            byte[].class)),
                            capture(files, executor, "/api/v1/component/dependency/{id}", target,
                                    map("id", componentId),
                                    t -> t.request(APPLICATION_OCTET_STREAM_TYPE).get(byte[].class)),
                            capture(files, executor, "/api/v1/component/icon/{id}", target, map("id", componentId),
                                    t -> t.request(APPLICATION_OCTET_STREAM_TYPE).get(byte[].class)),
                            capture(files, executor, "/api/v1/component/icon/family/{id}", target, map("id", familyId),
                                    t -> t.request(APPLICATION_OCTET_STREAM_TYPE).get(byte[].class)),
                            capture(files, executor, "/api/v1/component/migrate/{id}/{configurationVersion}", target,
                                    map("id", componentId, "configurationVersion", "1"),
                                    t -> t.request(APPLICATION_JSON_TYPE).post(entity("{}", APPLICATION_JSON_TYPE),
                                            byte[].class)),

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
                                    t -> t.request(APPLICATION_JSON_TYPE).post(entity("{}", APPLICATION_JSON_TYPE),
                                            byte[].class)))
                    .get(10, MINUTES);
        } catch (final TimeoutException e) {
            log.error(e.getMessage(), e);
        }
        executor.shutdown();
        log.info("Updating {}", files.keySet());
        final Collection<String> createdPaths = new ArrayList<>();
        files.forEach((sshPath, data) -> { // keep a single SSH connection (in the original thread)
            final String[] pathSegments = sshPath.substring(1).split("/");
            for (int i = 1; i < pathSegments.length; i++) {
                final String parentPath = Stream.of(pathSegments).limit(i).collect(joining("/", "/", "/"));
                if (!createdPaths.contains(parentPath)) {
                    try {
                        log.info("Creating {}", parentPath);
                        ftp.mkd(parentPath);
                        createdPaths.add(parentPath);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            try {
                log.info("Storing {}", sshPath);
                if (!ftp.storeFile(sshPath, new ByteArrayInputStream(data))) {
                    log.info("Failed to store {}", sshPath);
                    throw new IllegalStateException("Can't upload " + sshPath);
                }
                log.info("Stored {}", sshPath);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            log.info("Uploaded {}", sshPath);
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
            final String sshPath = "/public_html" + new StringSubstitutor(templates, "{", "}").replace(path);
            log.info("Trying to grab {}", sshPath);
            final Client client = ClientBuilder.newClient();
            try {
                WebTarget webTarget = client.target(base).path(path);
                for (final Map.Entry<String, String> tpl : templates.entrySet()) {
                    webTarget = webTarget.resolveTemplate(tpl.getKey(), tpl.getValue());
                }
                webTarget.property("http.connection.timeout", 30000L).property("http.receive.timeout", 60000L);
                files.put(sshPath, target.apply(webTarget));
                log.info("Grabbed to grab {}", sshPath);
            } catch (final ProcessingException | WebApplicationException ex) {
                log.error("Error on {}", sshPath, ex);
                throw ex;
            } finally {
                client.close();
            }
        }, executor);
    }
}
