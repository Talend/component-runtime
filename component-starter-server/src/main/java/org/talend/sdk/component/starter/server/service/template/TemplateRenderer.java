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
package org.talend.sdk.component.starter.server.service.template;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.DecoratedCollection;
import com.github.mustachejava.util.Wrapper;

@ApplicationScoped
public class TemplateRenderer {

    private final ConcurrentMap<String, Mustache> templates = new ConcurrentHashMap<>();

    private final DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory() {

        {
            setObjectHandler(new ReflectionObjectHandler() {

                @Override
                public Wrapper find(final String name, final List<Object> scopes) {
                    final Wrapper wrapper = super.find(name, scopes);
                    return s -> {
                        final Object call = wrapper.call(s);
                        if (Collection.class.isInstance(call) && !DecoratedCollection.class.isInstance(call)) {
                            return new DecoratedCollection<>(Collection.class.cast(call));
                        }
                        return call;
                    };
                }

                @Override // java 11 support
                protected AccessibleObject findMember(final Class sClass, final String name) {
                    if (sClass == String.class && "value".equals(name)) {
                        return null;
                    }
                    return super.findMember(sClass, name);
                }
            });
        }

        @Override
        public void encode(final String value, final Writer writer) {
            try {
                writer.write(value);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    public String render(final String template, final Object model) {
        final StringWriter writer = new StringWriter();
        templates.computeIfAbsent(template, t -> mustacheFactory.compile(template)).execute(writer, model);
        return writer.toString().replace("\r", "");
    }
}
