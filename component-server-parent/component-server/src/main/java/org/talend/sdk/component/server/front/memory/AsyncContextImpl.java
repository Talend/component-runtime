/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.memory;

import static java.util.Optional.ofNullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.servlet.ServletController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AsyncContextImpl implements AsyncContext {

    private final ServletRequest request;

    private final InMemoryResponse response;

    private final boolean originalRequestAndResponse;

    private final Collection<AsyncListener> listeners = new ArrayList<>();

    private final ServletController controller;

    private long timeout;

    AsyncContext start() {
        final AsyncEvent event = new AsyncEvent(this, request, response);
        executeOnListeners(l -> l.onStartAsync(event), listeners::clear);
        return this;
    }

    public void onError(final Throwable throwable) {
        final AsyncEvent event = new AsyncEvent(this, request, response, throwable);
        executeOnListeners(l -> l.onError(event), null);
        if (!response.isCommitted() && HttpServletResponse.class.isInstance(response)) {
            final HttpServletResponse http = HttpServletResponse.class.cast(response);
            http.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        complete();
    }

    private void executeOnListeners(final UnsafeConsumer<AsyncListener> fn, final Runnable afterCopy) {
        final List<AsyncListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners.size());
            ofNullable(afterCopy).ifPresent(Runnable::run);
        }
        listenersCopy.forEach(listener -> {
            try {
                fn.accept(listener);
            } catch (final Throwable t) {
                log.warn("callback failed for listener of type [" + listener.getClass().getName() + "]", t);
            }
        });
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return originalRequestAndResponse;
    }

    @Override
    public void dispatch() {
        final ServletRequest servletRequest = getRequest();
        if (!HttpServletRequest.class.isInstance(servletRequest)) {
            throw new IllegalStateException("Not a http request: " + servletRequest);
        }

        final HttpServletRequest sr = HttpServletRequest.class.cast(servletRequest);

        String path = sr.getRequestURI();
        final String cpath = sr.getContextPath();
        if (cpath.length() > 1) {
            path = path.substring(cpath.length());
        }
        dispatch(urlDecode(path));
    }

    @Override
    public void dispatch(final String path) {
        dispatch(request.getServletContext(), path);
    }

    @Override
    public void dispatch(final ServletContext context, final String path) {
        final ServletRequest servletRequest = getRequest();
        if (!HttpServletRequest.class.isInstance(servletRequest)) {
            throw new IllegalStateException("Not a http request: " + servletRequest);
        }

        final HttpServletRequest request = HttpServletRequest.class.cast(servletRequest);
        if (request.getAttribute(ASYNC_REQUEST_URI) == null) {
            request.setAttribute(ASYNC_REQUEST_URI, request.getRequestURI());
            request.setAttribute(ASYNC_CONTEXT_PATH, request.getContextPath());
            request.setAttribute(ASYNC_SERVLET_PATH, request.getServletPath());
            request.setAttribute(ASYNC_PATH_INFO, request.getPathInfo());
            request.setAttribute(ASYNC_QUERY_STRING, request.getQueryString());
        }

        try {
            controller.invoke(request, response);
        } catch (final ServletException ioe) {
            onError(ioe);
        }
    }

    @Override
    public void complete() {
        final AsyncEvent event = new AsyncEvent(this, request, response);
        executeOnListeners(l -> l.onComplete(event), null);
        try {
            response.getOutputStream().close();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void start(final Runnable run) {
        run.run();
    }

    @Override
    public void addListener(final AsyncListener listener) {
        listeners.add(new AsyncListenerWrapper(listener, request, response));
    }

    @Override
    public void addListener(final AsyncListener listener, final ServletRequest request,
            final ServletResponse response) {
        listeners.add(new AsyncListenerWrapper(listener, request, response));
    }

    @Override
    public <T extends AsyncListener> T createListener(final Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    @Override
    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    // taken from tomcat
    private static String urlDecode(final String str) {
        if (str == null) {
            return null;
        }

        if (str.indexOf('%') == -1) {
            // No %nn sequences, so return string unchanged
            return str;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(str.length() * 2);
        final OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        final char[] sourceChars = str.toCharArray();
        final int len = sourceChars.length;
        int ix = 0;

        try {
            while (ix < len) {
                char c = sourceChars[ix++];
                if (c == '%') {
                    osw.flush();
                    if (ix + 2 > len) {
                        throw new IllegalArgumentException("Missing digit: " + str);
                    }
                    char c1 = sourceChars[ix++];
                    char c2 = sourceChars[ix++];
                    if (isHexDigit(c1) && isHexDigit(c2)) {
                        baos.write(x2c(c1, c2));
                    } else {
                        throw new IllegalArgumentException("Missing digit: " + str);
                    }
                } else {
                    osw.append(c);
                }
            }
            osw.flush();

            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (final IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    private static boolean isHexDigit(final int c) {
        return ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
    }

    private static int x2c(final char b1, final char b2) {
        int digit = (b1 >= 'A') ? ((b1 & 0xDF) - 'A') + 10 : (b1 - '0');
        digit *= 16;
        digit += (b2 >= 'A') ? ((b2 & 0xDF) - 'A') + 10 : (b2 - '0');
        return digit;
    }

    private static class AsyncListenerWrapper implements AsyncListener {

        private final AsyncListener delegate;

        private final ServletRequest request;

        private final ServletResponse response;

        private AsyncListenerWrapper(final AsyncListener delegate, final ServletRequest request,
                final ServletResponse response) {
            this.delegate = delegate;
            this.request = request;
            this.response = response;
        }

        @Override
        public void onComplete(final AsyncEvent event) throws IOException {
            delegate.onComplete(wrap(event));
        }

        @Override
        public void onTimeout(final AsyncEvent event) throws IOException {
            delegate.onTimeout(wrap(event));
        }

        @Override
        public void onError(final AsyncEvent event) throws IOException {
            delegate.onError(wrap(event));
        }

        @Override
        public void onStartAsync(final AsyncEvent event) throws IOException {
            delegate.onStartAsync(wrap(event));
        }

        private AsyncEvent wrap(final AsyncEvent event) {
            if (request != null && response != null) {
                return new AsyncEvent(event.getAsyncContext(), request, response, event.getThrowable());
            }
            return event;
        }
    }

    @FunctionalInterface
    private interface UnsafeConsumer<T> {

        void accept(T t) throws IOException;
    }
}
