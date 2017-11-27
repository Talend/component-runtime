/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.template;

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
                return Mustache.compiler().escapeHTML(false).compile(content);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }).execute(model);
    }
}
