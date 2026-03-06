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
package org.talend.sdk.component.api.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documentation("Database types mapping. Studio will use this to generate the database mapping and allow to access to the DB"
        +
        " Column Types. Setting this to <code>custom</code> will allow to use the custom database mapping using an action."
        +
        " Setting this to an empty string will disable the database mapping and will not allow to access to the DB Column"
        +
        " type. The functionality is for the Studio only. ")
public @interface DatabaseMapping {

    /**
     * The database type to map.
     */
    String value() default Mapping.NONE;

    /**
     * The mapper function name to fetch the custom mapping according configuration.
     */
    String mapping() default "";

    public static class Mapping {

        public static final String CUSTOM = "custom"; //

        public static final String NONE = ""; // default, no mappings.

        private Mapping() {
            // nop
        }
    }

}
