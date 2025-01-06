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
package org.talend.sdk.component.server.front.memory;

import static java.util.Collections.singletonList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class InMemoryResponse implements HttpServletResponse {

    private final BooleanSupplier isOpen;

    private final Runnable onFlush;

    private final Consumer<byte[]> writeCallback;

    private final BiFunction<Integer, Map<String, List<String>>, String> preWrite;

    private int code = HttpServletResponse.SC_OK;

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private transient PrintWriter writer;

    private transient ServletByteArrayOutputStream sosi;

    private boolean commited = false;

    private String encoding = "UTF-8";

    private Locale locale = Locale.getDefault();

    public InMemoryResponse(final BooleanSupplier isOpen, final Runnable onFlush, final Consumer<byte[]> write,
            final BiFunction<Integer, Map<String, List<String>>, String> preWrite) {
        this.isOpen = isOpen;
        this.onFlush = onFlush;
        this.writeCallback = write;
        this.preWrite = preWrite;
    }

    /**
     * sets a header to be sent back to the browser
     *
     * @param name
     * the name of the header
     * @param value
     * the value of the header
     */
    public void setHeader(final String name, final String value) {
        headers.put(name, new ArrayList<>(singletonList(value)));
    }

    @Override
    public void setIntHeader(final String s, final int i) {
        setHeader(s, Integer.toString(i));
    }

    @Override
    public void setStatus(final int i) {
        setCode(i);
    }

    @Override
    public void setStatus(final int i, final String s) {
        setCode(i);
    }

    @Override
    public void addCookie(final Cookie cookie) {
        setHeader(cookie.getName(), cookie.getValue());
    }

    @Override
    public void addDateHeader(final String s, final long l) {
        setHeader(s, Long.toString(l));
    }

    @Override
    public void addHeader(final String s, final String s1) {
        Collection<String> list = headers.get(s);
        if (list == null) {
            setHeader(s, s1);
        } else {
            list.add(s1);
        }
    }

    @Override
    public void addIntHeader(final String s, final int i) {
        setIntHeader(s, i);
    }

    @Override
    public boolean containsHeader(final String s) {
        return headers.containsKey(s);
    }

    @Override
    public String encodeURL(final String s) {
        return toEncoded(s);
    }

    @Override
    public String encodeRedirectURL(final String s) {
        return toEncoded(s);
    }

    @Override
    public String encodeUrl(final String s) {
        return toEncoded(s);
    }

    @Override
    public String encodeRedirectUrl(final String s) {
        return encodeRedirectURL(s);
    }

    public String getHeader(final String name) {
        final Collection<String> strings = headers.get(name);
        return strings == null ? null : strings.iterator().next();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public Collection<String> getHeaders(final String s) {
        return headers.get(s);
    }

    @Override
    public int getStatus() {
        return getCode();
    }

    @Override
    public void sendError(final int i) throws IOException {
        setCode(i);
    }

    @Override
    public void sendError(final int i, final String s) throws IOException {
        setCode(i);
    }

    @Override
    public void sendRedirect(final String path) throws IOException {
        if (commited) {
            throw new IllegalStateException("response already committed");
        }
        resetBuffer();

        try {
            setStatus(SC_FOUND);

            setHeader("Location", toEncoded(path));
        } catch (final IllegalArgumentException e) {
            setStatus(SC_NOT_FOUND);
        }
    }

    @Override
    public void setDateHeader(final String s, final long l) {
        addDateHeader(s, l);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return sosi == null ? (sosi = createOutputStream()) : sosi;
    }

    @Override
    public PrintWriter getWriter() {
        return writer == null ? (writer = new PrintWriter(getOutputStream())) : writer;
    }

    @Override
    public boolean isCommitted() {
        return commited;
    }

    @Override
    public void reset() {
        createOutputStream();
    }

    private ServletByteArrayOutputStream createOutputStream() {
        return sosi = new ServletByteArrayOutputStream(isOpen, onFlush, writeCallback,
                () -> preWrite.apply(getStatus(), headers)) {

            @Override
            protected void beforeClose() throws IOException {
                onClose(this);
            }
        };
    }

    public void flushBuffer() {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public int getBufferSize() {
        return sosi.outputStream.size();
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    public void setCode(final int code) {
        this.code = code;
        commited = true;
    }

    public int getCode() {
        return code;
    }

    public void setContentType(final String type) {
        setHeader("Content-Type", type);
    }

    @Override
    public void setLocale(final Locale loc) {
        locale = loc;
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void resetBuffer() {
        sosi.outputStream.reset();
    }

    @Override
    public void setBufferSize(final int i) {
        // no-op
    }

    @Override
    public void setCharacterEncoding(final String s) {
        encoding = s;
    }

    @Override
    public void setContentLength(final int i) {
        // no-op
    }

    @Override
    public void setContentLengthLong(final long length) {
        // no-op
    }

    private String toEncoded(final String url) {
        return url;
    }

    protected void onClose(final OutputStream stream) throws IOException {
        // no-op
    }

    private static class ServletByteArrayOutputStream extends ServletOutputStream {

        private static final int BUFFER_SIZE = 1024 * 8;

        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private final BooleanSupplier isOpen;

        private final Runnable onFlush;

        private final Consumer<byte[]> writer;

        private final Supplier<String> preWrite;

        private boolean closed;

        private boolean headerWritten;

        private ServletByteArrayOutputStream(final BooleanSupplier isOpen, final Runnable onFlush,
                final Consumer<byte[]> write, final Supplier<String> preWrite) {
            this.isOpen = isOpen;
            this.onFlush = onFlush;
            this.writer = write;
            this.preWrite = preWrite;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(final WriteListener listener) {
            // no-op
        }

        @Override
        public void write(final int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            outputStream.write(b, off, len);
        }

        public void writeTo(final OutputStream out) throws IOException {
            outputStream.writeTo(out);
        }

        public void reset() {
            outputStream.reset();
        }

        @Override
        public void flush() throws IOException {
            if (!isOpen.getAsBoolean()) {
                return;
            }
            if (outputStream.size() >= BUFFER_SIZE) {
                doFlush();
            }
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }

            beforeClose();
            doFlush();
            closed = true;
        }

        protected void beforeClose() throws IOException {
            // no-op
        }

        private void doFlush() {
            final byte[] array = outputStream.toByteArray();
            final boolean written = array.length > 0 || !headerWritten;

            if (!headerWritten) {
                final String headers = preWrite.get();
                if (!headers.isEmpty()) {
                    writer.accept(headers.getBytes(StandardCharsets.UTF_8));
                }
                headerWritten = true;
            }

            if (array.length > 0) {
                outputStream.reset();
                writer.accept(array);
            }

            if (written) {
                onFlush.run();
            }
        }
    }
}
