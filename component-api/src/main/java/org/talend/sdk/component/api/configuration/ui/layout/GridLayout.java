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
package org.talend.sdk.component.api.configuration.ui.layout;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.ui.meta.Ui;
import org.talend.sdk.component.api.meta.Documentation;

@Ui
@Documentation("Advanced layout to place properties by row, this is exclusive with `@OptionsOrder`.\n"
        + "\nNOTE: the logic to handle forms (gridlayout names) is to use the only layout if there is only one defined, "
        + "else to check if there are `Main` and `Advanced` and if at least `Main` exists, use them, else "
        + "use all available layouts.")
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(GridLayouts.class)
public @interface GridLayout {

    interface FormType {

        String MAIN = "Main";

        String ADVANCED = "Advanced";

        String CHECKPOINT = "Checkpoint";

        @Deprecated // this one means nothing, surely to drop and use main instead
        String CITIZEN = "CitizenUser";
    }

    /**
     * @return the ordered list of rows of the layout.
     */
    Row[] value();

    /**
     * @return the form name associated to this definition.
     */
    String[] names() default FormType.MAIN;

    /**
     * Defines a UI row (list of widgets).
     */
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @interface Row {

        /**
         * @return the ordered list of property/widgets to set on this row.
         */
        String[] value();
    }
}
