/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@ApplicationScoped
public class AsciidoctorService {

    private ScriptEngineManager manager;

    private final ThreadLocal<Function<String, String>> engines = ThreadLocal.withInitial(() -> {
        final ScriptEngine jsEngine = manager.getEngineByExtension("js");
        try {
            final Object asciidoctor =
                    jsEngine.eval("load('classpath:talend/server/asciidoctor.js');" + "Asciidoctor();");
            return input -> {
                try {
                    final Bindings bindings = jsEngine.createBindings();
                    bindings.put("asciidoctorServiceContent", input);
                    bindings.put("asciidoctor", asciidoctor);
                    return String.valueOf(jsEngine
                            .eval("asciidoctor.convert(asciidoctorServiceContent, {safe: 'server'});", bindings));
                } catch (final ScriptException e) {
                    throw new IllegalStateException(e);
                }
            };
        } catch (final ScriptException e) {
            throw new IllegalStateException(e);
        }
    });

    @PostConstruct
    private void loadAsciidoctor() {
        manager = new ScriptEngineManager();
    }

    public String toHtml(final String input) {
        return engines.get().apply(input);
    }
}
