/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service.connection;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;

@ActionType(value = "create_connection", expectedReturnedType = Object.class)
@Target(METHOD)
@Retention(RUNTIME)
@Documentation("Mark an action works for creating runtime connection, returning a runtime connection object like jdbc connection if database family. "
        + "Its parameter MUST be a datastore. Datastore is configuration type annotated with @DataStore. "
        + "The functionality is for the Studio only, studio will use the runtime connection object when use existed connection, and no effect for cloud platform.")
public @interface CreateConnection {

    /**
     * @return the value of the component family this action relates to.
     */
    String family() default "";

    /**
     * @return the value of the action.
     */
    String value() default "default";
}
