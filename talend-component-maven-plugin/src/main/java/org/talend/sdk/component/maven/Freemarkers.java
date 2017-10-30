/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.maven;

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
