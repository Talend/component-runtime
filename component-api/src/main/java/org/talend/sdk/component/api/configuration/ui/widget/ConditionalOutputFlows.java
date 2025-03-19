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
package org.talend.sdk.component.api.configuration.ui.widget;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.configuration.ui.meta.Ui;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.outputs.AvailableOutputFlows;

@Ui
@ActionRef(AvailableOutputFlows.class) // Check that the value references a AvailableOutputFlows service
@Documentation("Mark the decorated processor as supporting dynamic outputs flow list.")
@Target(TYPE)
@Retention(RUNTIME)
public @interface ConditionalOutputFlows {
    /**
     * @return value of @{@link AvailableOutputFlows} value method.
     */
    String value();
}
