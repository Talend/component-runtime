/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import org.talend.sdk.component.api.service.schema.FixedSchema;

public class SchemaParameterEnricher extends BaseParameterEnricher {

    public static final String META_PREFIX = "tcomp::ui::schema::fixed";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        if (FixedSchema.class.equals(annotation.annotationType())) {
            FixedSchema fixed = FixedSchema.class.cast(annotation);
            return new HashMap<String, String>() {

                {
                    put(META_PREFIX, fixed.value());
                }
            };
        }
        return emptyMap();
    }
}
