/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import static java.util.Collections.singletonMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class CharConstraintEnricher extends BaseParameterEnricher {

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        if (parameterType == char.class) {
            final Map<String, String> constraints = new HashMap<>();
            constraints.put("tcomp::validation::maxLength", "1");
            constraints.put("tcomp::validation::minLength", "1");
            return constraints;
        }
        if (parameterType == Character.class) {
            return singletonMap("tcomp::validation::maxLength", "1");
        }
        return emptyMap();
    }
}
