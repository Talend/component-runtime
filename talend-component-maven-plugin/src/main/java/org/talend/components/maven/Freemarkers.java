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
package org.talend.components.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class Freemarkers {

    private final Map<String, Template> templates = new HashMap<>();

    private final String base;

    public Freemarkers(final String base) {
        this.base = base;
    }

    public String templatize(final String template, final Map<String, Object> context) {
        final Template tpl = templates.computeIfAbsent(template, tplName -> {
            final Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setTemplateLoader(new ClassTemplateLoader(LegacyComponentBridgeMojo.class.getClassLoader(), base));
            try {
                return cfg.getTemplate(tplName + ".ftlh");
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        });

        final StringWriter writer = new StringWriter();
        try {
            tpl.process(context, writer);
        } catch (final TemplateException | IOException e) {
            throw new IllegalStateException(e);
        }
        writer.flush();
        return writer.toString();
    }
}
