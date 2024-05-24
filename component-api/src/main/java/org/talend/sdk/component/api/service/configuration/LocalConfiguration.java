/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service.configuration;

import java.util.Set;

/**
 * Provide an abstraction to read the local - environment - configuration.
 */
public interface LocalConfiguration {

    /**
     * Access a configuration entry.
     *
     * @param key the configuration key.
     * @return the value corresponding to the key in the configuration repository for the calling component/family.
     */
    String get(String key);

    /**
     * @return all keys for the current family namespace.
     */
    Set<String> keys();
}
