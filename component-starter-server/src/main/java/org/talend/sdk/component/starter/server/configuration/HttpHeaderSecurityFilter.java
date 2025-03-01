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
package org.talend.sdk.component.starter.server.configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpHeaderSecurityFilter implements Filter {

    // HSTS
    private static final String HSTS_HEADER_NAME = "Strict-Transport-Security";
    private boolean hstsEnabled = true;
    private int hstsMaxAgeSeconds = 0;
    private boolean hstsIncludeSubDomains = false;
    private boolean hstsPreload = false;
    private String hstsHeaderValue;

    // Click-jacking protection
    private static final String ANTI_CLICK_JACKING_HEADER_NAME = "X-Frame-Options";
    private boolean antiClickJackingEnabled = true;
    private XFrameOption antiClickJackingOption = XFrameOption.DENY;
    private URI antiClickJackingUri;
    private String antiClickJackingHeaderValue;

    // Block content sniffing
    private static final String BLOCK_CONTENT_TYPE_SNIFFING_HEADER_NAME = "X-Content-Type-Options";
    private static final String BLOCK_CONTENT_TYPE_SNIFFING_HEADER_VALUE = "nosniff";
    private boolean blockContentTypeSniffingEnabled = true;

    // Cross-site scripting filter protection
    @Deprecated
    private static final String XSS_PROTECTION_HEADER_NAME = "X-XSS-Protection";
    @Deprecated
    private static final String XSS_PROTECTION_HEADER_VALUE = "1; mode=block";
    @Deprecated
    private boolean xssProtectionEnabled = false;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // Build HSTS header value
        StringBuilder hstsValue = new StringBuilder("max-age=");
        hstsValue.append(hstsMaxAgeSeconds);
        if (hstsIncludeSubDomains) {
            hstsValue.append(";includeSubDomains");
        }
        if (hstsPreload) {
            hstsValue.append(";preload");
        }
        hstsHeaderValue = hstsValue.toString();

        // Anti click-jacking
        StringBuilder cjValue = new StringBuilder(antiClickJackingOption.headerValue);
        if (antiClickJackingOption == XFrameOption.ALLOW_FROM) {
            cjValue.append(' ');
            cjValue.append(antiClickJackingUri);
        }
        antiClickJackingHeaderValue = cjValue.toString();
    }


    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if (response.isCommitted()) {
                throw new ServletException("Unable to add HTTP headers since response is already committed on entry to the HTTP header security Filter");
            }

            // HSTS
            if (hstsEnabled && request.isSecure()) {
                httpResponse.setHeader(HSTS_HEADER_NAME, hstsHeaderValue);
            }

            // anti click-jacking
            if (antiClickJackingEnabled) {
                httpResponse.setHeader(ANTI_CLICK_JACKING_HEADER_NAME, antiClickJackingHeaderValue);
            }

            // Block content type sniffing
            if (blockContentTypeSniffingEnabled) {
                httpResponse.setHeader(BLOCK_CONTENT_TYPE_SNIFFING_HEADER_NAME,
                        BLOCK_CONTENT_TYPE_SNIFFING_HEADER_VALUE);
            }

            // cross-site scripting filter protection
            if (xssProtectionEnabled) {
                httpResponse.setHeader(XSS_PROTECTION_HEADER_NAME, XSS_PROTECTION_HEADER_VALUE);
            }
        }

        chain.doFilter(request, response);
    }

    public boolean isHstsEnabled() {
        return hstsEnabled;
    }


    public void setHstsEnabled(final boolean hstsEnabled) {
        this.hstsEnabled = hstsEnabled;
    }


    public int getHstsMaxAgeSeconds() {
        return hstsMaxAgeSeconds;
    }


    public void setHstsMaxAgeSeconds(final int hstsMaxAgeSeconds) {
        if (hstsMaxAgeSeconds < 0) {
            this.hstsMaxAgeSeconds = 0;
        } else {
            this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        }
    }


    public boolean isHstsIncludeSubDomains() {
        return hstsIncludeSubDomains;
    }


    public void setHstsIncludeSubDomains(final boolean hstsIncludeSubDomains) {
        this.hstsIncludeSubDomains = hstsIncludeSubDomains;
    }


    public boolean isHstsPreload() {
        return hstsPreload;
    }


    public void setHstsPreload(final boolean hstsPreload) {
        this.hstsPreload = hstsPreload;
    }


    public boolean isAntiClickJackingEnabled() {
        return antiClickJackingEnabled;
    }


    public void setAntiClickJackingEnabled(final boolean antiClickJackingEnabled) {
        this.antiClickJackingEnabled = antiClickJackingEnabled;
    }


    public String getAntiClickJackingOption() {
        return antiClickJackingOption.toString();
    }


    public void setAntiClickJackingOption(final String antiClickJackingOption) {
        for (XFrameOption option : XFrameOption.values()) {
            if (option.getHeaderValue().equalsIgnoreCase(antiClickJackingOption)) {
                this.antiClickJackingOption = option;
                return;
            }
        }
        throw new IllegalArgumentException(
                "An invalid value [" + antiClickJackingOption + "] was specified for the anti click-jacking header");
    }


    public String getAntiClickJackingUri() {
        return antiClickJackingUri.toString();
    }


    public boolean isBlockContentTypeSniffingEnabled() {
        return blockContentTypeSniffingEnabled;
    }


    public void setBlockContentTypeSniffingEnabled(final boolean blockContentTypeSniffingEnabled) {
        this.blockContentTypeSniffingEnabled = blockContentTypeSniffingEnabled;
    }


    public void setAntiClickJackingUri(final String antiClickJackingUri) {
        URI uri;
        try {
            uri = new URI(antiClickJackingUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        this.antiClickJackingUri = uri;
    }


    @Deprecated
    public boolean isXssProtectionEnabled() {
        return xssProtectionEnabled;
    }


    @Deprecated
    public void setXssProtectionEnabled(final boolean xssProtectionEnabled) {
        this.xssProtectionEnabled = xssProtectionEnabled;
    }


    private enum XFrameOption {
        DENY("DENY"),
        SAME_ORIGIN("SAMEORIGIN"),
        ALLOW_FROM("ALLOW-FROM");


        private final String headerValue;

        XFrameOption(final String headerValue) {
            this.headerValue = headerValue;
        }

        public String getHeaderValue() {
            return headerValue;
        }
    }
}
