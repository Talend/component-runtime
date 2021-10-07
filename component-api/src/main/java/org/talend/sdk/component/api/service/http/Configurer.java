/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
 * Callback to configure the connection more deeply. Typically used to configure timeouts.
 */
public interface Configurer {

    /**
     * @param connection the current connection to customize.
     * @param configuration the configuration of the invocation if any.
     */
    void configure(Connection connection, ConfigurerConfiguration configuration);

    /**
     * Represents actions doable on a connection.
     */
    interface Connection {

        /**
         * @return the method of current connection.
         */
        String getMethod();

        /**
         * @return the url of current connection.
         */
        String getUrl();

        /**
         * @return headers already set.
         */
        Map<String, List<String>> getHeaders();

        /**
         * @return payload of the request or null.
         */
        byte[] getPayload();

        /**
         * Adds a header to the request.
         *
         * @param name header name.
         * @param value header value.
         * @return current connection.
         */
        Connection withHeader(String name, String value);

        /**
         * Sets the read timeout of the connection.
         *
         * @param timeout timeout value in milliseconds.
         * @return current connection.
         */
        Connection withReadTimeout(int timeout);

        /**
         * Sets the connection timeout of the connection.
         *
         * @param timeout timeout value in milliseconds.
         * @return current connection.
         */
        Connection withConnectionTimeout(int timeout);

        /**
         * Prevents the client to follow redirections.
         * 
         * @return connection.
         */
        Connection withoutFollowRedirects();
    }

    /**
     * Represents the potential {@link ConfigurerOption} parameters of the invocation.
     */
    interface ConfigurerConfiguration {

        /**
         * @return all options at once.
         */
        Object[] configuration();

        /**
         * @param name the option name.
         * @param type the expected type of the option.
         * @param <T> the type of the option.
         * @return the option value or null if missing.
         */
        <T> T get(String name, Class<T> type);
    }
}
