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
package org.talend.sdk.component.server.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class AsciidoctorService {

    private CompletableFuture<Asciidoctor> instance;

    private Thread loadingThread;

    private final Options options = OptionsBuilder
            .options()
            .backend("html")
            .headerFooter(false)
            .attributes(AttributesBuilder.attributes().attribute("showtitle"))
            .get();

    void init(@Observes @Initialized(ApplicationScoped.class) final ServletContext start) {
        instance = new CompletableFuture<>();
        loadingThread = new Thread(() -> instance.complete(Asciidoctor.Factory.create()));
        loadingThread.setContextClassLoader(start.getClassLoader());
        loadingThread.setName(getClass().getName() + "-init");
        loadingThread.start();
    }

    @PreDestroy
    private void stop() {
        if (loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
    }

    public String toHtml(final String input) {
        try {
            return instance.toCompletableFuture().get().render(input, options);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
