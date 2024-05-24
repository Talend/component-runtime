/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.emptySet;
import static java.util.Collections.enumeration;
import static java.util.Collections.singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.cxf.transport.servlet.ServletController;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.talend.sdk.component.server.front.security.ConnectionSecurityProvider;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class InMemoryRequest implements HttpServletRequest {

    private static final Cookie[] NO_COOKIE = new Cookie[0];

    private static final SimpleDateFormat[] DATE_FORMATS =
            { new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
                    new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
                    new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

    private final Map<String, Object> attributes = new HashMap<>();

    private final String method;

    private final Map<String, List<String>> headers;

    private final String requestUri;

    private final String pathInfo;

    private final String servletPath;

    private final String query;

    private final int port;

    private final ServletContext servletContext;

    private final ServletInputStream inputStream;

    private final Supplier<Principal> principalSupplier;

    private final ServletController controller;

    private String encoding;

    private long length;

    private String type;

    private Map<String, String[]> parameters = new HashMap<>();

    private Locale locale = Locale.getDefault();

    private BufferedReader reader;

    @Setter
    private InMemoryResponse response;

    private AsyncContext asyncContext;

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return NO_COOKIE;
    }

    @Override
    public long getDateHeader(final String name) {
        final String value = getHeader(name);
        if (value == null) {
            return -1L;
        }

        final SimpleDateFormat[] formats = new SimpleDateFormat[DATE_FORMATS.length];
        for (int i = 0; i < formats.length; i++) {
            formats[i] = SimpleDateFormat.class.cast(DATE_FORMATS[i].clone());
        }

        final long result = FastHttpDateFormat.parseDate(value, formats);
        if (result != -1L) {
            return result;
        }
        throw new IllegalArgumentException(value);
    }

    @Override
    public String getHeader(final String s) {
        final List<String> strings = headers.get(s);
        return strings == null || strings.isEmpty() ? null : strings.iterator().next();
    }

    @Override
    public Enumeration<String> getHeaders(final String s) {
        final List<String> strings = headers.get(s);
        return strings == null || strings.isEmpty() ? null : enumeration(strings);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(final String s) {
        final String value = getHeader(s);
        if (value == null) {
            return -1;
        }

        return Integer.parseInt(value);
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return pathInfo;
    }

    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getRemoteUser() {
        final Principal principal = getUserPrincipal();
        return principal == null ? null : principal.getName();
    }

    @Override
    public boolean isUserInRole(final String s) {
        return false; // if needed do it with the original request
    }

    @Override
    public Principal getUserPrincipal() {
        return principalSupplier.get();
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(requestUri);
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public HttpSession getSession(final boolean b) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(final HttpServletResponse httpServletResponse) {
        return false;
    }

    @Override
    public void login(final String s, final String s1) {
        // no-op
    }

    @Override
    public void logout() {
        // no-op
    }

    @Override
    public Collection<Part> getParts() {
        return emptySet();
    }

    @Override
    public Part getPart(final String s) {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(final Class<T> aClass) {
        return null;
    }

    @Override
    public Object getAttribute(final String s) {
        if (ConnectionSecurityProvider.SKIP.equalsIgnoreCase(s)) {
            return Boolean.TRUE;
        }
        return attributes.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public void setCharacterEncoding(final String s) {
        encoding = s;
    }

    @Override
    public int getContentLength() {
        return (int) length;
    }

    @Override
    public long getContentLengthLong() {
        return length;
    }

    @Override
    public String getContentType() {
        return type;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public String getParameter(final String s) {
        final String[] strings = parameters.get(s);
        return strings == null || strings.length == 0 ? null : strings[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(final String s) {
        return parameters.get(s);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return servletContext.getVirtualServerName();
    }

    @Override
    public int getServerPort() {
        return port;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return reader == null ? (reader = new BufferedReader(new InputStreamReader(getInputStream()))) : reader;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(final String s, final Object o) {
        attributes.put(s, o);
    }

    @Override
    public void removeAttribute(final String s) {
        attributes.remove(s);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return locale == null ? emptyEnumeration() : enumeration(singleton(locale));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getRealPath(final String s) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final String s) {
        return servletContext.getRequestDispatcher(s);
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        asyncContext = new AsyncContextImpl(this, response, true, controller).start();
        return asyncContext;
    }

    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse)
            throws IllegalStateException {
        asyncContext =
                new AsyncContextImpl(servletRequest, InMemoryResponse.class.cast(servletResponse), false, controller)
                        .start();
        return asyncContext;
    }

    @Override
    public boolean isAsyncStarted() {
        return asyncContext != null;
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }
}
