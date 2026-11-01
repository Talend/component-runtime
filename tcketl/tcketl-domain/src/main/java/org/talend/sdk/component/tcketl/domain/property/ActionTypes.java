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
package org.talend.sdk.component.tcketl.domain.property;

/**
 * Action types a property can reference. They are the type names of the component server; the set is open, so action
 * types stay plain strings and unknown ones are carried through.
 */
public final class ActionTypes {

    public static final String HEALTHCHECK = "healthcheck";

    public static final String SUGGESTIONS = "suggestions";

    public static final String DYNAMIC_VALUES = "dynamic_values";

    public static final String VALIDATION = "validation";

    public static final String UPDATE = "update";

    public static final String SCHEMA = "schema";

    /** Resolved by the designer itself: the server cannot execute it. */
    public static final String BUILT_IN_SUGGESTABLE = "built_in_suggestable";

    private ActionTypes() {
        // constants only
    }
}
