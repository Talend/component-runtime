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
package org.talend.sdk.component.api.component;

import java.util.Map;

/**
 * Migrate a tcomp data instance from one version to another one.
 *
 * This uses a flat format to make any conversion very easy and an "untyped" structure to avoid
 * <ul>
 * <li>to require the type to be passed through the layers</li>
 * <li>to require to instantiate unknown objects</li>
 * </ul>
 *
 * This is linked to a component through {@link Version} to ensure cross inputs can be handled.
 */
public interface MigrationHandler {

    /**
     * @param incomingVersion the version of associatedData values.
     * @param incomingData the data sent from the caller. Keys are using the path of the property as in component
     * metadata.
     * @return the set of properties for the current version.
     */
    Map<String, String> migrate(int incomingVersion, Map<String, String> incomingData);
}
