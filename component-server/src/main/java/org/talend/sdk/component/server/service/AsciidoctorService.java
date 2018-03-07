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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@ApplicationScoped
public class AsciidoctorService {

    private ScriptEngine jsEngine;

    private String adocScript;

    private Object asciidoctor;

    @PostConstruct
    private void loadAsciidoctor() {
        jsEngine = new ScriptEngineManager().getEngineByExtension("js");
        try {
            asciidoctor = jsEngine.eval("load('classpath:talend/server/asciidoctor.js');" + "Asciidoctor();");
        } catch (final ScriptException e) {
            throw new IllegalStateException(e);
        }
    }

    public String toHtml(final String input) {
        try {
            final Bindings bindings = jsEngine.createBindings();
            bindings.put("asciidoctorServiceContent", input);
            bindings.put("asciidoctor", asciidoctor);
            return String.valueOf(
                    jsEngine.eval("asciidoctor.convert(asciidoctorServiceContent, {safe: 'server'});", bindings));
        } catch (final ScriptException e) {
            throw new IllegalStateException(e);
        }
    }
}
