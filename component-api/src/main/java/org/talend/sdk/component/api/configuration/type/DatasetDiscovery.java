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
package org.talend.sdk.component.api.configuration.type;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.type.meta.ConfigurationType;
import org.talend.sdk.component.api.meta.Documentation;

@Target(TYPE)
@Retention(RUNTIME)
@ConfigurationType("datasetDiscovery")
@Documentation("Mark a model (complex object) as being a dataset discovery configuration.")
public @interface DatasetDiscovery {

    String value() default "default";
}
