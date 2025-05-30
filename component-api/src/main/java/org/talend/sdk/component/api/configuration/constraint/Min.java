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
package org.talend.sdk.component.api.configuration.constraint;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;

import org.talend.sdk.component.api.configuration.constraint.meta.Validation;
import org.talend.sdk.component.api.meta.Documentation;

@Validation(expectedTypes = { Number.class, int.class, short.class, byte.class, long.class, double.class, float.class },
        name = "min")
@Validation(expectedTypes = Collection.class, name = "minItems")
@Validation(expectedTypes = CharSequence.class, name = "minLength")
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Documentation("Ensure the decorated option size is validated with a lower bound.")
public @interface Min {

    double value();
}
