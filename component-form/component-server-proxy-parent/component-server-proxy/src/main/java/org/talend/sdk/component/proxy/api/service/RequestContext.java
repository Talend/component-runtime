/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.api.service;

public interface RequestContext {

    /**
     * @return the current request language.
     */
    String language();

    /**
     * Used to handle placeholders in headers (see config doc).
     *
     * @param attributeName the name of the placeholder to find.
     * @return the value for this placeholder.
     */
    String findPlaceholder(String attributeName);

    /**
     * Return a contextual attribute.
     *
     * @param key the attribute identifier.
     * @return the attribute value or null if missing.
     */
    Object attribute(String key);
}
