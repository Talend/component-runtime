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
package org.talend.components.starter.server.service.template;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

@ApplicationScoped
public class TemplateRenderer {
    private final ConcurrentMap<String, Template> templates = new ConcurrentHashMap<>();

    public String render(final String template, final Object model) {
        return templates.computeIfAbsent(template, t -> {
            try (final BufferedReader is = new BufferedReader(new InputStreamReader(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream(template)))) {
                final String content = is.lines().collect(joining("\n"));
                return Mustache.compiler()
                               .escapeHTML(false)
                               .compile(content);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }).execute(model);
    }
}
