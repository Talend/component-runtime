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
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.catalina.core.StandardServer;
import org.apache.commons.cli.CommandLine;
import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.runner.Cli;

public class WebServer implements Runnable {

    private final Collection<String> serverArguments;

    private final Integer port;

    private final String componentGav;

    private final Log log;

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

    @Override
    public void run() {
        final String originalCompSystProp = System.getProperty("talend.component.server.component.coordinates");
        System.setProperty("talend.component.server.component.coordinates", componentGav);
        final AtomicReference<Meecrowave> ref = new AtomicReference<>();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final Cli cli = new Cli(buildArgs()) {

                @Override
                protected void doWait(final Meecrowave meecrowave, final CommandLine line) {
                    ref.set(meecrowave);
                    latch.countDown();
                    super.doWait(meecrowave, line);
                }
            };
            new Thread(() -> {
                try {
                    cli.run();
                } catch (final RuntimeException re) {
                    latch.countDown();
                    log.error(re.getMessage());
                    throw re;
                }
            }, getClass().getName() + '_' + findPort()).start();
            try {
                latch.await(2, MINUTES);
            } catch (final InterruptedException e) {
                Thread.interrupted();
                return;
            }
            final Scanner scanner = new Scanner(System.in);
            do {
                log.info("Enter 'exit' to quit");
            } while (!"exit".equals(scanner.nextLine()));
        } finally {
            if (originalCompSystProp == null) {
                System.clearProperty("talend.component.server.component.coordinates");
            } else {
                System.setProperty("talend.component.server.component.coordinates", originalCompSystProp);
            }
            ofNullable(ref.get()).ifPresent(mw -> StandardServer.class.cast(mw.getTomcat().getServer()).stopAwait());
        }
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
        return args.toArray(new String[args.size()]);
    }

    private String findPort() {
        return port == null ? "8080" : Integer.toString(port);
    }
}
