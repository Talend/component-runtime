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
package org.talend.sdk.component.starter.server.service.info;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.talend.sdk.component.starter.server.configuration.StarterConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ApplicationScoped
public class ServerInfo {

    private Date lastUpdate;

    private volatile Snapshot snapshot;

    @Inject
    @Getter(NONE)
    private StarterConfiguration configuration;

    private ScheduledExecutorService thread;

    private ScheduledFuture<?> future;

    private SAXParserFactory saxFactory;

    @PostConstruct
    private void init() {
        saxFactory = SAXParserFactory.newInstance();
        try {
            saxFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            saxFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (final ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException ex) {
            // ignore
        }

        doUpdate(() -> Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("starter-versions.properties"));

        if (!configuration.getAutoRefresh()) {
            return;
        }

        thread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, getClass().getName());
            }
        });
        future = thread.scheduleAtFixedRate(this::refresh, 0, configuration.getRefreshDelayMs(), MILLISECONDS);
    }

    private void doUpdate(final Supplier<InputStream> streamProvider) {
        final Snapshot newSnapshot = new Snapshot();
        try (final InputStream stream = streamProvider.get()) {
            final Properties properties = new Properties();
            properties.load(stream);
            properties.stringPropertyNames().forEach(name -> {
                try {
                    final Field field = Snapshot.class.getDeclaredField(name);
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    final String property = properties.getProperty(name);
                    if (!property.trim().isEmpty()) {
                        field.set(newSnapshot, property);
                    }
                } catch (final Exception ise) {
                    log.error(ise.getMessage(), ise);
                }
            });
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        snapshot = newSnapshot;
        lastUpdate = new Date();
    }

    // here we grab the starter latest version from central, then we grab the associated version metadata
    // and if we are not up to date we update that
    private void refresh() {
        final Client client = ClientBuilder.newClient();
        final WebTarget base =
                client.target("http://repo.maven.apache.org/maven2/org/talend/sdk/component/component-starter-server/");
        final QuickMvnMetadataParser handler = new QuickMvnMetadataParser();
        try (final InputStream metadata =
                base.path("maven-metadata.xml").request(MediaType.APPLICATION_XML_TYPE).get(InputStream.class)) {
            final SAXParser parser = saxFactory.newSAXParser();
            parser.parse(metadata, handler);
            if (handler.release != null) {
                final String release = handler.release.toString();
                if (!release.equals(snapshot.getKit())) {
                    synchronized (this) {
                        log.info("Updating current version from {} to {}", snapshot.getKit(), release);
                        doUpdate(() -> base
                                .path("{version}/component-starter-server-{version}-versions.properties")
                                .resolveTemplate("version", release)
                                .request(MediaType.APPLICATION_XML_TYPE)
                                .get(InputStream.class));
                    }
                }
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    @PreDestroy
    private void destroy() {
        ofNullable(future).ifPresent(f -> f.cancel(true));
        ofNullable(thread).ifPresent(ExecutorService::shutdownNow);
    }

    private static class QuickMvnMetadataParser extends DefaultHandler {

        private StringBuilder release;

        private StringBuilder text;

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                final Attributes attributes) {
            if ("release".equalsIgnoreCase(qName)) {
                release = new StringBuilder();
                text = release;
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) {
            if (text != null) {
                text.append(new String(ch, start, length));
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) {
            text = null;
        }
    }

    @Data
    @NoArgsConstructor(access = PRIVATE)
    public static class Snapshot {

        private String apiKit;

        private String kit;

        private String beam;

        private String avroJackson;

        private String surefire = "2.22.1";

        private String cxf;

        private String log4j2;
    }
}
