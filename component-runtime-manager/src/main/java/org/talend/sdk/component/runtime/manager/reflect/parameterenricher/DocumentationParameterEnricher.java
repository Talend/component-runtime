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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.meta.Documentation;

public class DocumentationParameterEnricher extends BaseParameterEnricher {

    private static final String VALUE = "tcomp::documentation::value";

    private static final String TOOLTIP = "tcomp::documentation::tooltip";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        if (annotation.annotationType() == Documentation.class) {
            HashMap<String, String> parameters = new HashMap<>();
            Documentation doc = Documentation.class.cast(annotation);
            parameters.put(VALUE, doc.value());
            if (doc.tooltip()) {
                parameters.put(TOOLTIP, "true");
            }
            return parameters;
        }
        return emptyMap();
    }
}
