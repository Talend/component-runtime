/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.parameter;

import lombok.NoArgsConstructor;

/**
 * Metadata key constants, which are used in SimplePropertyDefinition
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Metadatas {

    public static final String ACTION_DYNAMIC_VALUES = "action::dynamic_values";

    public static final String CONFIG_TYPE = "configurationtype::type";

    public static final String CONFIG_NAME = "configurationtype::name";

    public static final String UI_CODE = "ui::code::value";

    public static final String UI_CREDENTIAL = "ui::credential";

    public static final String UI_OPTIONS_ORDER = "ui::optionsorder::value";

    public static final String UI_TEXTAREA = "ui::textarea";

    public static final String UI_GRIDLAYOUT_MAIN = "ui::gridlayout::Main::value";

    public static final String UI_GRIDLAYOUT_ADVANCED = "ui::gridlayout::Advanced::value";

    /**
     * Value separator for {@link #UI_OPTIONS_ORDER}
     */
    public static final String ORDER_SEPARATOR = ",";

    // Supported Code languages
    public static final String JAVA = "Java";
}
