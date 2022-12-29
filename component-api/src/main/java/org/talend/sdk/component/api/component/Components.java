/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.component;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Enable to configure the component by module (package).
 */
@Target(PACKAGE)
@Retention(RUNTIME)
public @interface Components {

    /**
     * @return the family value for all classes needing a component in this package or subpackages.
     */
    String family();

    /**
     * The categories of the nested components (in the package).
     * You can use <code>${family}</code> to represent the family in the category.
     * If not present it will be appended at the end, for example <code>Misc</code>
     * will become <code>Misc/${family}</code>.
     *
     * @return the categories to associate to this component. Default to "Misc".
     */
    String categories() default;
}
