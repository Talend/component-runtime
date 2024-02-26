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
package org.talend.sdk.component.junit5;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a test class as running under Talend Component Kit context
 * and makes available ComponentsHandler as a test injection.
 */
@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(ComponentExtension.class)
public @interface WithComponents {

    /**
     * @return the package containing the component(s).
     */
    String value();

    /**
     * You can isolate some packages during the test.
     * Note that in the context of a test this can have side effects so ensure to know what you
     * are doing.
     *
     * @return the package laoded from their own classloader.
     */
    String[] isolatedPackages() default {};
}
