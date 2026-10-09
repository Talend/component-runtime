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

import static java.util.Collections.list;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.security.ConnectionSecurityProvider;

class InMemoryRequestTest {

    private InMemoryRequest request(final Map<String, List<String>> headers, final Principal principal,
            final ServletContext context, final byte[] body) {
        return new InMemoryRequest("GET", headers, "/api/v1/x", "/x", "/api", "a=b", 8080, context,
                new MemoryInputStream(body == null ? null : new ByteArrayInputStream(body)), () -> principal, null);
    }

    @Test
    void basics() {
        final ServletContext context = mock(ServletContext.class);
        when(context.getContextPath()).thenReturn("/ctx");
        when(context.getVirtualServerName()).thenReturn("vserver");
        final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(context.getRequestDispatcher("/p")).thenReturn(dispatcher);
        final Principal principal = () -> "bob";
        final InMemoryRequest request = request(new HashMap<>(), principal, context, null);

        assertEquals("GET", request.getMethod());
        assertEquals("/x", request.getPathInfo());
        assertEquals("/x", request.getPathTranslated());
        assertEquals("/ctx", request.getContextPath());
        assertEquals("a=b", request.getQueryString());
        assertEquals("bob", request.getRemoteUser());
        assertEquals(principal, request.getUserPrincipal());
        assertEquals("/api/v1/x", request.getRequestURI());
        assertEquals("/api/v1/x", request.getRequestURL().toString());
        assertEquals("/api", request.getServletPath());
        assertEquals("HTTP/1.1", request.getProtocol());
        assertEquals("http", request.getScheme());
        assertEquals("vserver", request.getServerName());
        assertEquals(8080, request.getServerPort());
        assertSame(context, request.getServletContext());
        assertSame(dispatcher, request.getRequestDispatcher("/p"));
    }

    @Test
    void sessionAndSecurityDefaults() {
        final InMemoryRequest request = request(new HashMap<>(), null, mock(ServletContext.class), null);
        assertNull(request.getAuthType());
        assertEquals(0, request.getCookies().length);
        assertNull(request.getRequestedSessionId());
        assertNull(request.getSession());
        assertNull(request.getSession(true));
        assertNull(request.changeSessionId());
        assertFalse(request.isRequestedSessionIdValid());
        assertFalse(request.isRequestedSessionIdFromCookie());
        assertFalse(request.isRequestedSessionIdFromURL());
        assertFalse(request.authenticate(null));
        assertFalse(request.isUserInRole("any"));
        request.login("u", "p");
        request.logout();
        assertTrue(request.getParts().isEmpty());
        assertNull(request.getPart("p"));
        assertNull(request.upgrade(null));
    }

    @Test
    void connectionAndAsyncDefaults() {
        final InMemoryRequest request = request(new HashMap<>(), null, mock(ServletContext.class), null);
        assertNull(request.getRemoteAddr());
        assertNull(request.getRemoteHost());
        assertEquals(0, request.getRemotePort());
        assertNull(request.getLocalName());
        assertNull(request.getLocalAddr());
        assertEquals(0, request.getLocalPort());
        assertFalse(request.isSecure());
        assertNull(request.getServletConnection());
        assertNotNull(request.getRequestId());
        assertEquals("", request.getProtocolRequestId());
        assertTrue(request.isAsyncSupported());
        assertFalse(request.isAsyncStarted());
        assertNull(request.getAsyncContext());
        assertThrows(UnsupportedOperationException.class, request::getDispatcherType);
    }

    @Test
    void noPrincipal() {
        assertNull(request(new HashMap<>(), null, mock(ServletContext.class), null).getRemoteUser());
    }

    @Test
    void headers() {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("h", List.of("v1", "v2"));
        headers.put("i", List.of("12"));
        headers.put("empty", List.of());
        headers.put("d", List.of("Thu, 01 Jan 1970 00:00:01 GMT"));
        headers.put("bad", List.of("not a date"));
        final InMemoryRequest request = request(headers, null, mock(ServletContext.class), null);

        assertEquals("v1", request.getHeader("h"));
        assertNull(request.getHeader("missing"));
        assertNull(request.getHeader("empty"));
        assertEquals(List.of("v1", "v2"), list(request.getHeaders("h")));
        assertNull(request.getHeaders("missing"));
        assertNull(request.getHeaders("empty"));
        assertEquals(4 + 1, list(request.getHeaderNames()).size());
        assertEquals(12, request.getIntHeader("i"));
        assertEquals(-1, request.getIntHeader("missing"));
        assertEquals(-1L, request.getDateHeader("missing"));
        assertEquals(1000L, request.getDateHeader("d"));
        assertThrows(IllegalArgumentException.class, () -> request.getDateHeader("bad"));
    }

    @Test
    void attributesAndEncoding() {
        final InMemoryRequest request = request(new HashMap<>(), null, mock(ServletContext.class), null);
        assertEquals(Boolean.TRUE, request.getAttribute(ConnectionSecurityProvider.SKIP));
        assertNull(request.getAttribute("a"));
        request.setAttribute("a", "b");
        assertEquals("b", request.getAttribute("a"));
        assertEquals(List.of("a"), list(request.getAttributeNames()));
        request.removeAttribute("a");
        assertNull(request.getAttribute("a"));

        assertNull(request.getCharacterEncoding());
        request.setCharacterEncoding("UTF-8");
        assertEquals("UTF-8", request.getCharacterEncoding());
        assertEquals(0, request.getContentLength());
        assertEquals(0L, request.getContentLengthLong());
        assertNull(request.getContentType());
    }

    @Test
    void parametersAndLocale() {
        final InMemoryRequest request = request(new HashMap<>(), null, mock(ServletContext.class), null);
        assertNull(request.getParameter("p"));
        assertNull(request.getParameterValues("p"));
        assertFalse(request.getParameterNames().hasMoreElements());
        request.getParameterMap().put("p", new String[] { "1", "2" });
        request.getParameterMap().put("e", new String[0]);
        assertEquals("1", request.getParameter("p"));
        assertNull(request.getParameter("e"));
        assertArrayEquals(new String[] { "1", "2" }, request.getParameterValues("p"));
        assertEquals(2, list(request.getParameterNames()).size());
        assertEquals(Locale.getDefault(), request.getLocale());
        assertEquals(List.of(Locale.getDefault()), list(request.getLocales()));
    }

    @Test
    void body() throws IOException {
        final InMemoryRequest request = request(new HashMap<>(), null, mock(ServletContext.class),
                "hello".getBytes(StandardCharsets.UTF_8));
        assertNotNull(request.getInputStream());
        final java.io.BufferedReader reader = request.getReader();
        assertSame(reader, request.getReader());
        assertEquals("hello", reader.readLine());

        final InMemoryRequest empty = request(new HashMap<>(), null, mock(ServletContext.class), null);
        assertEquals(-1, empty.getInputStream().read());
    }
}
