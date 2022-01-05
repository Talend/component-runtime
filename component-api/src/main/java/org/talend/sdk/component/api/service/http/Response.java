/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import java.util.List;
import java.util.Map;

/**
 * Represents a Http response and can be used as returned type of a {@link Request} method.
 *
 * @param <T> the type of the payload.
 */
public interface Response<T> {

    /**
     * @return the http status of the response.
     */
    int status();

    /**
     * @return the response headers.
     */
    Map<String, List<String>> headers();

    /**
     * @return the payload.
     */
    T body();

    <E> E error(Class<E> type);
}
