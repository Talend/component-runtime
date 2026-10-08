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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

class InMemoryResponseTest {

    private final List<byte[]> written = new ArrayList<>();

    private final AtomicInteger flushes = new AtomicInteger();

    private boolean open = true;

    private InMemoryResponse response(final String preWrite) {
        return new InMemoryResponse(() -> open, flushes::incrementAndGet, written::add, (status, headers) -> preWrite);
    }

    private String all() {
        final StringBuilder out = new StringBuilder();
        written.forEach(b -> out.append(new String(b, StandardCharsets.UTF_8)));
        return out.toString();
    }

    @Test
    void headers() {
        final InMemoryResponse response = response("");
        assertNull(response.getHeader("x"));
        assertFalse(response.containsHeader("x"));
        response.setHeader("X", "1");
        assertTrue(response.containsHeader("x"));
        response.addHeader("x", "2");
        response.addHeader("y", "3");
        assertEquals("1", response.getHeader("x"));
        assertEquals(List.of("1", "2"), response.getHeaders("X"));
        assertEquals("3", response.getHeader("y"));
        response.setIntHeader("i", 4);
        response.addIntHeader("j", 5);
        response.setDateHeader("d", 6L);
        response.addDateHeader("e", 7L);
        assertEquals("4", response.getHeader("i"));
        assertEquals("5", response.getHeader("j"));
        assertEquals("6", response.getHeader("d"));
        assertEquals("7", response.getHeader("e"));
        response.addCookie(new Cookie("c", "v"));
        assertEquals("v", response.getHeader("c"));
        assertEquals(7, response.getHeaderNames().size());
        response.setContentType("text/plain");
        assertEquals("text/plain", response.getContentType());
    }

    @Test
    void statusAndMisc() throws IOException {
        final InMemoryResponse response = response("");
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertFalse(response.isCommitted());
        response.setStatus(201);
        assertEquals(201, response.getCode());
        assertTrue(response.isCommitted());
        response.sendError(500);
        assertEquals(500, response.getStatus());
        response.sendError(404, "nope");
        assertEquals(404, response.getStatus());
        assertEquals("a", response.encodeURL("a"));
        assertEquals("b", response.encodeRedirectURL("b"));
        assertEquals("UTF-8", response.getCharacterEncoding());
        response.setCharacterEncoding("ISO-8859-1");
        assertEquals("ISO-8859-1", response.getCharacterEncoding());
        response.setLocale(Locale.FRANCE);
        assertEquals(Locale.FRANCE, response.getLocale());
        response.setBufferSize(1);
        response.setContentLength(1);
        response.setContentLengthLong(1);
    }

    @Test
    void redirect() throws IOException {
        final InMemoryResponse response = response("");
        response.getOutputStream().write('x');
        response.sendRedirect("/there");
        assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
        assertEquals("/there", response.getHeader("Location"));
        assertEquals(0, response.getBufferSize());
        assertThrows(IllegalStateException.class, () -> response.sendRedirect("/again"));
    }

    @Test
    void redirectWithoutClearingBuffer() throws IOException {
        final InMemoryResponse response = response("");
        response.getOutputStream();
        response.sendRedirect("/there", 301, false);
        assertEquals(301, response.getStatus());
    }

    @Test
    void writeAndClose() throws IOException {
        final InMemoryResponse response = response("HEAD\n");
        final ServletOutputStream out = response.getOutputStream();
        assertSame(out, response.getOutputStream());
        assertTrue(out.isReady());
        out.setWriteListener(null);
        out.write("ab".getBytes(StandardCharsets.UTF_8), 0, 2);
        out.write('c');
        assertEquals(3, response.getBufferSize());
        out.flush(); // below buffer size, nothing written yet
        assertTrue(written.isEmpty());
        out.close();
        out.close();
        assertEquals("HEAD\nabc", all());
        assertEquals(1, flushes.get());
    }

    @Test
    void writerAndEmptyHeader() throws IOException {
        final InMemoryResponse response = response("");
        response.getWriter().print("hi");
        assertSame(response.getWriter(), response.getWriter());
        response.flushBuffer();
        response.getOutputStream().close();
        assertEquals("hi", all());
    }

    @Test
    void flushBufferWithoutWriter() {
        response("").flushBuffer();
    }

    @Test
    void closeWithoutDataStillFlushes() throws IOException {
        final InMemoryResponse response = response("");
        response.getOutputStream().close();
        assertTrue(written.isEmpty());
        assertEquals(1, flushes.get());
    }

    @Test
    void flushWhenClosedConnectionIsIgnored() throws IOException {
        open = false;
        final InMemoryResponse response = response("");
        final ServletOutputStream out = response.getOutputStream();
        out.write(new byte[9000], 0, 9000);
        out.flush();
        assertTrue(written.isEmpty());
    }

    @Test
    void bigFlushWritesData() throws IOException {
        final InMemoryResponse response = response("H");
        final ServletOutputStream out = response.getOutputStream();
        out.write(new byte[9000], 0, 9000);
        out.flush();
        assertEquals(2, written.size());
        assertEquals(9001, written.stream().mapToInt(b -> b.length).sum());
    }

    @Test
    void reset() throws IOException {
        final InMemoryResponse response = response("");
        response.getOutputStream().write('a');
        response.reset();
        assertEquals(0, response.getBufferSize());
    }
}
