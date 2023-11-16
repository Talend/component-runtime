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
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.catalina.core.StandardServer;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.runner.Cli;

public class WebServer implements Runnable {

    private final Collection<String> serverArguments;

    private final Integer port;

    private final String componentGav;

    private final Log log;

    private final Collection<Consumer<Meecrowave.Builder>> onOpen = new ArrayList<>();

    public WebServer(final Collection<String> serverArguments, final Integer port, final Object log, final String gav) {
        this.serverArguments = serverArguments;
        this.port = port;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        this.componentGav = gav;
    }

    public WebServer onOpen(final Consumer<Meecrowave.Builder> task) {
        onOpen.add(task);
        return this;
    }

    public WebServer openBrowserWhenReady() {
        return onOpen(builder -> Browser.open("http://localhost:" + builder.getHttpPort(), log));
    }

    @Override
    public void run() {
        final String originalCompSystProp =
                setSystemProperty("talend.component.server.component.coordinates", componentGav);
        final String skipClasspathSystProp = setSystemProperty("component.manager.classpath.skip", "true");
        final String skipCallersSystProp = setSystemProperty("component.manager.callers.skip", "true");
        final AtomicReference<Meecrowave> ref = new AtomicReference<>();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                try (final Meecrowave meecrowave = new Meecrowave(Cli.create(buildArgs()))) {
                    meecrowave.start().deployClasspath(new Meecrowave.DeploymentMeta("", null, stdCtx -> {
                        stdCtx.setResources(new StandardRoot() {

                            @Override
                            protected void registerURLStreamHandlerFactory() {
                                // no-op - gradle supports to reuse the same JVM so it would be broken
                            }
                        });
                    }, null));

                    ref.set(meecrowave);
                    latch.countDown();
                    onOpen.forEach(it -> it.accept(meecrowave.getConfiguration()));
                    meecrowave.getTomcat().getServer().await();
                } catch (final RuntimeException re) {
                    latch.countDown();
                    log.error(re.getMessage());
                    throw re;
                }
            }, getClass().getName() + '_' + findPort()).start();
            try {
                latch.await(2, MINUTES);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            final boolean batch = Boolean.parseBoolean(System.getProperty("talend.web.batch", "false"));
            final int timeout = Integer.parseInt(System.getProperty("talend.web.batch.timeout", "2"));
            if (!batch) {
                log.info("\n\n  You can now access the UI at http://localhost:" + port + "\n\n");
                final Scanner scanner = new Scanner(System.in);
                do {
                    log.info("Enter 'exit' to quit");
                } while (!shouldQuit(scanner.nextLine()));
            } else {
                log.info(String.format(
                        "Server running at http://localhost:%d in non-interactive mode. Will shutdown in %d minutes.",
                        port, timeout));
                try {
                    Thread.currentThread().sleep(MINUTES.toMillis(timeout));
                    log.info("Shutting down.");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            reset("talend.component.server.component.coordinates", originalCompSystProp);
            reset("component.manager.classpath.skip", skipClasspathSystProp);
            reset("component.manager.callers.skip", skipCallersSystProp);
            ofNullable(ref.get()).ifPresent(mw -> StandardServer.class.cast(mw.getTomcat().getServer()).stopAwait());
        }
    }

    private String setSystemProperty(final String key, final String value) {
        final String old = System.getProperty(key);
        System.setProperty(key, value);
        return old;
    }

    private void reset(final String key, final String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private boolean shouldQuit(final String value) {
        return Stream.of("exit", "quit", "X").anyMatch(v -> v.equalsIgnoreCase(value));
    }

    private String[] buildArgs() {
        final Collection<String> args = new ArrayList<>();
        if (serverArguments != null) {
            args.addAll(serverArguments);
        }
        if (serverArguments != null && serverArguments.contains("--http")) {
            if (port != null) {
                log.info("port configuration ignored since serverArguments already defines it");
            }
        } else {
            args.add("--http");
            args.add(findPort());
        }
        if (!args.contains("--scanning-exclude")) { // nicer default logging
            args.add("--scanning-exclude");
            args
                    .add("animal-sniffer-annotations,checker-qual,component-form,component-server-model,"
                            + "error_prone_annotations,failureaccess,freemarker,j2objc-annotations,jib-core,"
                            + "jsr305,listenablefuture,talend-component-maven-plugin,"
                            + "avro,beam,paranamer,xz,component-api,component-spi,component-runtime-impl,"
                            + "component-runtime-manager,component-runtime-design-extension,container-core,"
                            + "component-runtime-beam");
        }
        if (!args.contains("--use-shutdown-hook")) {
            args.add("--use-shutdown-hook");
            args.add("false");
        }
        return args.toArray(new String[0]);
    }

    private String findPort() {
        return port == null ? "8080" : Integer.toString(port);
    }
}
