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
package org.talend.sdk.component.api;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface DecryptedServer {

    /**
     * @return the settings.xml serverId to match.
     */
    String value();

    /**
     * @return the list of conditions to meet to activate this decrypting.
     */
    Conditions conditions() default @Conditions({ @Condition(forSystemProperty = "talend.junit.http.passthrough"),
            @Condition(forSystemProperty = "talend.maven.decrypter.active") });

    /**
     * @return true to always try to read the server whatever the condition state
     * and fallback on the mock values if not found and conditions are met.
     */
    boolean alwaysTryLookup() default true;

    /**
     * @return the username when forSystemProperty test fails.
     */
    String defaultUsername() default "username";

    /**
     * @return the username when forSystemProperty test fails.
     */
    String defaultPassword() default "password";

    @Target(ANNOTATION_TYPE)
    @Retention(RUNTIME)
    @interface Conditions {

        /**
         * @return the list of conditions to meet to use an actual server.
         */
        Condition[] value();

        /**
         * @return the way to combine the conditions.
         */
        Combination combination() default Combination.OR;
    }

    enum Combination {
        AND,
        OR
    }

    @Target(ANNOTATION_TYPE)
    @Retention(RUNTIME)
    @interface Condition {

        /**
         * @return a system property name to use for the test.
         */
        String forSystemProperty();

        /**
         * @return the expected system property to activate the condition.
         */
        String expectedValue() default "true";

        /**
         * @return true if the system property can be null.
         */
        boolean supportsNull() default false;
    }
}
