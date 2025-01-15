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
package org.talend.sdk.component.api.service.http;

/**
 * The service you can use to create http clients.
 */
public interface HttpClientFactory {

    /**
     * @param api your proxy definition, {@link Request}.
     * @param base the base url for the requests.
     * @param <T> the proxy type.
     * @return a http client ready to be used.
     */
    <T> T create(Class<T> api, String base);
}
