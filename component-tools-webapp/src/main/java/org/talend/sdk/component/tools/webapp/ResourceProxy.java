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
package org.talend.sdk.component.tools.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

@Dependent
@WebFilter(urlPatterns = "/*", asyncSupported = true, filterName = "ResourceProxy")
public class ResourceProxy implements Filter {

    @Inject
    private UIConfiguration uiConfiguration;

    private volatile String js;

    private volatile String css;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest http = HttpServletRequest.class.cast(request);
        final String uri = http.getRequestURI().substring(http.getContextPath().length());
        if (uri.startsWith("/main-")) {
            if (uri.endsWith(".js") && uiConfiguration.getJsLocation().isPresent()) {
                serveJs(response);
                return;
            } else if (uri.endsWith(".css") && uiConfiguration.getCssLocation().isPresent()) {
                serveCss(response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void serveCss(final ServletResponse response) throws IOException {
        if (css == null) {
            synchronized (this) {
                if (css == null) {
                    css = load(uiConfiguration.getCssLocation().orElseThrow(IllegalArgumentException::new));
                }
            }
        }
        response.getWriter().write(css);
    }

    private void serveJs(final ServletResponse response) throws IOException {
        if (js == null) {
            synchronized (this) {
                if (js == null) {
                    js = load(uiConfiguration.getJsLocation().orElseThrow(IllegalArgumentException::new));
                }
            }
        }
        response.getWriter().write(js);
    }

    private String load(final String url) {
        try (final InputStream stream = new URL(url).openStream()) {
            byte[] buffer = new byte[16384];
            int count;
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (-1 != (count = stream.read(buffer))) {
                byteArrayOutputStream.write(buffer, 0, count);
            }
            return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
