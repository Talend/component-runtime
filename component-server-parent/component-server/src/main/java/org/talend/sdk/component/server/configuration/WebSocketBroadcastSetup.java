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
package org.talend.sdk.component.server.configuration;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationCallback;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.ContinuationProviderFactory;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.talend.sdk.component.server.front.cxf.CxfExtractor;
import org.talend.sdk.component.server.front.memory.InMemoryRequest;
import org.talend.sdk.component.server.front.memory.InMemoryResponse;
import org.talend.sdk.component.server.front.memory.MemoryInputStream;
import org.talend.sdk.component.server.front.memory.SimpleServletConfig;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// ensure any JAX-RS command can use websockets
@Slf4j
@Dependent
@WebListener
public class WebSocketBroadcastSetup implements ServletContextListener {

    private static final String EOM = "^@";

    @Inject
    private Bus bus;

    @Inject
    private CxfExtractor cxf;

    @Inject
    private Instance<Application> applications;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final ServerContainer container =
                ServerContainer.class.cast(sce.getServletContext().getAttribute(ServerContainer.class.getName()));

        final JAXRSServiceFactoryBean factory = JAXRSServiceFactoryBean.class
                .cast(bus
                        .getExtension(ServerRegistry.class)
                        .getServers()
                        .iterator()
                        .next()
                        .getEndpoint()
                        .get(JAXRSServiceFactoryBean.class.getName()));

        final String appBase = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(applications.iterator(), Spliterator.IMMUTABLE), false)
                .filter(a -> a.getClass().isAnnotationPresent(ApplicationPath.class))
                .map(a -> a.getClass().getAnnotation(ApplicationPath.class))
                .map(ApplicationPath::value)
                .findFirst()
                .map(s -> !s.startsWith("/") ? "/" + s : s)
                .orElse("/api/v1");
        final String version = appBase.replaceFirst("/api", "");

        final DestinationRegistry registry = cxf.getRegistry();
        final ServletContext servletContext = sce.getServletContext();

        final WebSocketRegistry webSocketRegistry = new WebSocketRegistry(registry);
        final ServletController controller = new ServletController(webSocketRegistry,
                new SimpleServletConfig(servletContext, "Talend Component Kit Websocket Transport"),
                new ServiceListGeneratorServlet(registry, bus));
        webSocketRegistry.controller = controller;

        Stream
                .concat(factory
                        .getClassResourceInfo()
                        .stream()
                        .flatMap(cri -> cri.getMethodDispatcher().getOperationResourceInfos().stream())
                        .filter(cri -> cri.getAnnotatedMethod().getDeclaringClass().getName().startsWith("org.talend."))
                        .map(ori -> {
                            final String uri = ori.getClassResourceInfo().getURITemplate().getValue()
                                    + ori.getURITemplate().getValue();
                            return ServerEndpointConfig.Builder
                                    .create(Endpoint.class,
                                            "/websocket" + version + "/"
                                                    + String.valueOf(ori.getHttpMethod()).toLowerCase(ENGLISH) + uri)
                                    .configurator(new ServerEndpointConfig.Configurator() {

                                        @Override
                                        public <T> T getEndpointInstance(final Class<T> clazz)
                                                throws InstantiationException {
                                            final Map<String, List<String>> headers = new HashMap<>();
                                            if (!ori.getProduceTypes().isEmpty()) {
                                                headers
                                                        .put(HttpHeaders.CONTENT_TYPE, singletonList(
                                                                ori.getProduceTypes().iterator().next().toString()));
                                            }
                                            if (!ori.getConsumeTypes().isEmpty()) {
                                                headers
                                                        .put(HttpHeaders.ACCEPT, singletonList(
                                                                ori.getConsumeTypes().iterator().next().toString()));
                                            }
                                            return (T) new JAXRSEndpoint(appBase, controller, servletContext,
                                                    ori.getHttpMethod(), uri, headers);
                                        }
                                    })
                                    .build();
                        }),
                        Stream
                                .of(ServerEndpointConfig.Builder
                                        .create(Endpoint.class, "/websocket" + version + "/bus")
                                        .configurator(new ServerEndpointConfig.Configurator() {

                                            @Override
                                            public <T> T getEndpointInstance(final Class<T> clazz)
                                                    throws InstantiationException {

                                                return (T) new JAXRSEndpoint(appBase, controller, servletContext, "GET",
                                                        "/", emptyMap());
                                            }
                                        })
                                        .build()))
                .sorted(Comparator.comparing(ServerEndpointConfig::getPath))
                .peek(e -> log.info("Deploying WebSocket(path={})", e.getPath()))
                .forEach(config -> {
                    try {
                        container.addEndpoint(config);
                    } catch (final DeploymentException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    private static class JAXRSEndpoint extends Endpoint {

        private final String appBase;

        private final ServletController controller;

        private final ServletContext context;

        private final String defaultMethod;

        private final String defaultUri;

        private final Map<String, List<String>> baseHeaders;

        @RequiredArgsConstructor
        private class PartialMessageHandler implements Partial<byte[]> {

            private final Session session;

            private InMemoryRequest request;

            private InMemoryResponse response;

            private void handleStart(final StringBuilder buffer, final InputStream message) {
                final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                headers.putAll(baseHeaders);

                try { // read headers from the message
                    String line;
                    int del;
                    while ((line = readLine(buffer, message)) != null) {
                        final boolean done = line.endsWith(EOM);
                        if (done) {
                            line = line.substring(0, line.length() - EOM.length());
                        }
                        if (!line.isEmpty()) {
                            del = line.indexOf(':');
                            if (del < 0) {
                                headers.put(line.trim(), emptyList());
                            } else {
                                headers
                                        .put(line.substring(0, del).trim(),
                                                singletonList(line.substring(del + 1).trim()));
                            }
                        }
                        if (done) {
                            break;
                        }
                    }
                } catch (final IOException ioe) {
                    throw new IllegalStateException(ioe);
                }

                final List<String> uris = headers.get("destination");
                final String uri;
                if (uris == null || uris.isEmpty()) {
                    uri = defaultUri;
                } else {
                    uri = uris.iterator().next();
                }

                final List<String> methods = headers.get("destinationMethod");
                final String method;
                if (methods == null || methods.isEmpty()) {
                    method = defaultMethod;
                } else {
                    method = methods.iterator().next();
                }

                final String queryString;
                final String path;
                final int query = uri.indexOf('?');
                if (query > 0) {
                    queryString = uri.substring(query + 1);
                    path = uri.substring(0, query);
                } else {
                    queryString = null;
                    path = uri;
                }

                request = new InMemoryRequest(method.toUpperCase(ENGLISH), headers, path,
                        appBase + path, appBase, queryString, 8080, context, new WebSocketInputStream(message),
                        session::getUserPrincipal, controller);
                response = new InMemoryResponse(session::isOpen,
                        () -> {
                            if (session.getBasicRemote().getBatchingAllowed()) {
                                try {
                                    session.getBasicRemote().flushBatch();
                                } catch (final IOException e) {
                                    throw new IllegalStateException(e);
                                }
                            }
                        }, bytes -> {
                            try {
                                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                            } catch (final IOException e) {
                                throw new IllegalStateException(e);
                            }
                        }, (status, responseHeaders) -> {
                            final StringBuilder top = new StringBuilder("MESSAGE\r\n");
                            top.append("status: ").append(status).append("\r\n");
                            responseHeaders
                                    .forEach((k,
                                            v) -> top.append(k)
                                                    .append(": ")
                                                    .append(String.join(",", v))
                                                    .append("\r\n"));
                            top.append("\r\n");// empty line, means the next bytes are the payload
                            return top.toString();
                        }) {

                    @Override
                    protected void onClose(final OutputStream stream) throws IOException {
                        stream.write(EOM.getBytes(StandardCharsets.UTF_8));
                    }
                };
                request.setResponse(response);
            }

            @Override
            public void onMessage(final byte[] byteBuffer, final boolean last) {
                final ByteArrayInputStream message = new ByteArrayInputStream(byteBuffer);

                final StringBuilder buffer = new StringBuilder(128);
                try { // read headers from the message
                    if (request != null) {
                        ((WebSocketInputStream) request.getInputStream()).addStream(message);
                    } else if ("SEND".equalsIgnoreCase(readLine(buffer, message))) {
                        handleStart(buffer, message);
                    } else {
                        throw new IllegalArgumentException("not a message");
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

                if (last) {
                    try {
                        controller.invoke(request, response);
                    } catch (final ServletException e) {
                        throw new IllegalArgumentException(e);
                    } finally {
                        request = null;
                        response = null;
                    }
                }
            }
        }

        @Override
        public void onOpen(final Session session, final EndpointConfig endpointConfig) {
            log.debug("Opened session {}", session.getId());
            session.addMessageHandler(byte[].class, new PartialMessageHandler(session));
        }

        @Override
        public void onClose(final Session session, final CloseReason closeReason) {
            log.debug("Closed session {}", session.getId());
        }

        @Override
        public void onError(final Session session, final Throwable throwable) {
            log.warn("Error for session {}", session.getId(), throwable);
        }

        private static String readLine(final StringBuilder buffer, final InputStream in) throws IOException {
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    break;
                } else if (c != '\r') {
                    buffer.append((char) c);
                }
            }

            if (buffer.length() == 0) {
                return null;
            }
            final String string = buffer.toString();
            buffer.setLength(0);
            return string;
        }
    }

    private static class WebSocketInputStream extends MemoryInputStream {

        private int previous = Integer.MAX_VALUE;

        private final Queue<InputStream> queue = new LinkedList<>();

        private WebSocketInputStream(final InputStream delegate) {
            super(delegate);
            queue.add(delegate);
        }

        @Override
        public int read() throws IOException {
            if (finished) {
                return -1;
            }
            if (previous != Integer.MAX_VALUE) {
                previous = Integer.MAX_VALUE;
                return previous;
            }
            final int read = delegate().read();
            if (read == '^') {
                previous = delegate().read();
                if (previous == '@') {
                    finished = true;
                    return -1;
                }
            }
            if (read < 0) {
                finished = true;
            }
            return read;
        }

        private InputStream delegate() throws IOException {
            if (queue.isEmpty()) {
                throw new IOException("Don't have an input stream.");
            }

            if (queue.peek().available() == 0) {
                queue.remove();
            }

            return queue.peek();
        }

        public void addStream(final InputStream stream) {
            queue.add(stream);
        }
    }

    private static class WebSocketRegistry implements DestinationRegistry {

        private final DestinationRegistry delegate;

        private ServletController controller;

        private WebSocketRegistry(final DestinationRegistry registry) {
            this.delegate = registry;
        }

        @Override
        public void addDestination(final AbstractHTTPDestination destination) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeDestination(final String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AbstractHTTPDestination getDestinationForPath(final String path) {
            return wrap(delegate.getDestinationForPath(path));
        }

        @Override
        public AbstractHTTPDestination getDestinationForPath(final String path, final boolean tryDecoding) {
            return wrap(delegate.getDestinationForPath(path, tryDecoding));
        }

        @Override
        public AbstractHTTPDestination checkRestfulRequest(final String address) {
            return wrap(delegate.checkRestfulRequest(address));
        }

        @Override
        public Collection<AbstractHTTPDestination> getDestinations() {
            return delegate.getDestinations();
        }

        @Override
        public AbstractDestination[] getSortedDestinations() {
            return delegate.getSortedDestinations();
        }

        @Override
        public Set<String> getDestinationsPaths() {
            return delegate.getDestinationsPaths();
        }

        private AbstractHTTPDestination wrap(final AbstractHTTPDestination destination) {
            try {
                return destination == null ? null : new WebSocketDestination(destination, this);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class WebSocketDestination extends AbstractHTTPDestination {

        static final Logger LOG = LogUtils.getL7dLogger(ServletDestination.class);

        private final AbstractHTTPDestination delegate;

        private WebSocketDestination(final AbstractHTTPDestination delegate, final WebSocketRegistry registry)
                throws IOException {
            super(delegate.getBus(), registry, new EndpointInfo(), delegate.getPath(), false);
            this.delegate = delegate;
            this.cproviderFactory = new WebSocketContinuationFactory(registry);
        }

        @Override
        public EndpointReferenceType getAddress() {
            return delegate.getAddress();
        }

        @Override
        public Conduit getBackChannel(final Message inMessage) throws IOException {
            return delegate.getBackChannel(inMessage);
        }

        @Override
        public EndpointInfo getEndpointInfo() {
            return delegate.getEndpointInfo();
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMessageObserver(final MessageObserver observer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MessageObserver getMessageObserver() {
            return delegate.getMessageObserver();
        }

        @Override
        protected Logger getLogger() {
            return LOG;
        }

        @Override
        public Bus getBus() {
            return delegate.getBus();
        }

        @Override
        public void invoke(final ServletConfig config, final ServletContext context, final HttpServletRequest req,
                final HttpServletResponse resp) throws IOException {
            // eager create the message to ensure we set our continuation for @Suspended
            Message inMessage = retrieveFromContinuation(req);
            if (inMessage == null) {
                inMessage = new MessageImpl();

                final ExchangeImpl exchange = new ExchangeImpl();
                exchange.setInMessage(inMessage);
                setupMessage(inMessage, config, context, req, resp);

                exchange.setSession(new HTTPSession(req));
                MessageImpl.class.cast(inMessage).setDestination(this);
            }

            delegate.invoke(config, context, req, resp);
        }

        @Override
        public void finalizeConfig() {
            delegate.finalizeConfig();
        }

        @Override
        public String getBeanName() {
            return delegate.getBeanName();
        }

        @Override
        public EndpointReferenceType getAddressWithId(final String id) {
            return delegate.getAddressWithId(id);
        }

        @Override
        public String getId(final Map<String, Object> context) {
            return delegate.getId(context);
        }

        @Override
        public String getContextMatchStrategy() {
            return delegate.getContextMatchStrategy();
        }

        @Override
        public boolean isFixedParameterOrder() {
            return delegate.isFixedParameterOrder();
        }

        @Override
        public boolean isMultiplexWithAddress() {
            return delegate.isMultiplexWithAddress();
        }

        @Override
        public HTTPServerPolicy getServer() {
            return delegate.getServer();
        }

        @Override
        public void assertMessage(final Message message) {
            delegate.assertMessage(message);
        }

        @Override
        public boolean canAssert(final QName type) {
            return delegate.canAssert(type);
        }

        @Override
        public String getPath() {
            return delegate.getPath();
        }
    }

    private static class WebSocketContinuationFactory implements ContinuationProviderFactory {

        private static final String KEY = WebSocketContinuationFactory.class.getName();

        private final WebSocketRegistry registry;

        private WebSocketContinuationFactory(final WebSocketRegistry registry) {
            this.registry = registry;
        }

        @Override
        public ContinuationProvider createContinuationProvider(final Message inMessage, final HttpServletRequest req,
                final HttpServletResponse resp) {
            return new WebSocketContinuation(inMessage, req, resp, registry);
        }

        @Override
        public Message retrieveFromContinuation(final HttpServletRequest req) {
            return Message.class.cast(req.getAttribute(KEY));
        }
    }

    private static class WebSocketContinuation implements ContinuationProvider, Continuation {

        private final Message message;

        private final HttpServletRequest request;

        private final HttpServletResponse response;

        private final WebSocketRegistry registry;

        private final ContinuationCallback callback;

        private Object object;

        private boolean resumed;

        private boolean pending;

        private boolean isNew;

        private WebSocketContinuation(final Message message, final HttpServletRequest request,
                final HttpServletResponse response, final WebSocketRegistry registry) {
            this.message = message;
            this.request = request;
            this.response = response;
            this.registry = registry;
            this.request
                    .setAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE,
                            message.getExchange().getInMessage());
            this.callback = message.getExchange().get(ContinuationCallback.class);
        }

        @Override
        public Continuation getContinuation() {
            return this;
        }

        @Override
        public void complete() {
            message.getExchange().getInMessage().remove(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE);
            if (callback != null) {
                final Exception ex = message.getExchange().get(Exception.class);
                if (ex == null) {
                    callback.onComplete();
                } else {
                    callback.onError(ex);
                }
            }
            try {
                response.getWriter().close();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean suspend(final long timeout) {
            isNew = false;
            resumed = false;
            pending = true;
            message.getExchange().getInMessage().getInterceptorChain().suspend();
            return true;
        }

        @Override
        public void resume() {
            resumed = true;
            try {
                registry.controller.invoke(request, response);
            } catch (final ServletException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void reset() {
            pending = false;
            resumed = false;
            isNew = false;
            object = null;
        }

        @Override
        public boolean isNew() {
            return isNew;
        }

        @Override
        public boolean isPending() {
            return pending;
        }

        @Override
        public boolean isResumed() {
            return resumed;
        }

        @Override
        public boolean isTimeout() {
            return false;
        }

        @Override
        public Object getObject() {
            return object;
        }

        @Override
        public void setObject(final Object o) {
            object = o;
        }

        @Override
        public boolean isReadyForWrite() {
            return true;
        }
    }
}
