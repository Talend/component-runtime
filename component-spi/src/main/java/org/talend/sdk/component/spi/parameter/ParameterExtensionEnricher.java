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
package org.talend.sdk.component.spi.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Allow to build extensions of an object parameter.
 */
public interface ParameterExtensionEnricher {

    /**
     * Visit all annotations of an object parameter and return for each annotation
     * the related extensions. Note it is highly recommanded even if not enforced to
     * use a prefix by extension type.
     *
     * @param parameterName
     * the name of the parameter currently visited.
     * @param annotation
     * the currently visited annotation.
     * @return the extensions to add for this parameter.
     */
    Map<String, String> onParameterAnnotation(String parameterName, Type parameterType, Annotation annotation);
}
