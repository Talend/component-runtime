/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.api.service.outputs;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ActionType(value = "available_output", expectedReturnedType = Collection.class)
@Target(METHOD)
@Retention(RUNTIME)
@Documentation("Provide the output flows by some condition")
public @interface AvailableOutputFlows {

    /**
     * @return the value of the component family this action relates to.
     */
    String family() default "";

    /**
     * @return the value of the action.
     */
    String value() default "__default__";
}
