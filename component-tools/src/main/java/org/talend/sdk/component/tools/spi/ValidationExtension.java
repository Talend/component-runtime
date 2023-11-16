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
package org.talend.sdk.component.tools.spi;

import java.util.Collection;
import java.util.List;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

import lombok.Builder;
import lombok.Data;

public interface ValidationExtension {

    /**
     * IMPORTANT: this method must not fail but rather return the errors through the result instance.
     *
     * @param context the current validated context.
     * @return the validation result, mainly errors.
     */
    ValidationResult validate(ValidationContext context);

    interface ValidationContext {

        /**
         * @return the currently scanned module(s) finder to be able to grab custom API.
         */
        AnnotationFinder finder();

        /**
         * @return currently validated components - using the standard API.
         */
        List<Class<?>> components();

        /**
         * @param component the component type.
         * @return the flattened list of options for that component.
         */
        List<ParameterMeta> parameters(final Class<?> component);
    }

    @Data
    @Builder
    class ValidationResult {

        private Collection<String> errors;
    }
}
