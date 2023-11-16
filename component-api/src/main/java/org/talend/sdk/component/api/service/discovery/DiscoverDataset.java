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
package org.talend.sdk.component.api.service.discovery;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;

/**
 * Mark a method as returning the {@link DiscoverDatasetResult} of a datastore. The only configuration
 * parameter must be annotated with @DataStore.
 */
@ActionType(value = "discoverdataset", expectedReturnedType = DiscoverDatasetResult.class)
@Target(METHOD)
@Retention(RUNTIME)
@Documentation("This class marks an action that explore a connection to retrieve potential datasets.")
public @interface DiscoverDataset {

    /**
     * @return the value of the component family this action relates to.
     */
    String family() default "";

    /**
     * @return the name of this update action.
     */
    String value() default "default";

}
