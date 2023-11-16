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
package org.talend.sdk.component.api.service.http;

import java.lang.reflect.Type;

/**
 * Allows to read in a custom manner a response payload.
 */
public interface Decoder {

    /**
     * @param value the payload content.
     * @param expectedType the user type.
     * @return the instantiated payload respecting expectedType.
     */
    Object decode(byte[] value, Type expectedType);
}
