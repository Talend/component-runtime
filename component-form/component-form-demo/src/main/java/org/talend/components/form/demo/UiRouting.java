// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.form.demo;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@WebFilter(urlPatterns = "/detail/*")
public class UiRouting implements Filter {

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(new HttpServletRequestWrapper(HttpServletRequest.class.cast(servletRequest)) {

            @Override
            public String getPathInfo() {
                return "";
            }
        }, servletResponse);
    }
}
