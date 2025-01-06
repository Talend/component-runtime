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
package org.talend.sdk.component.junit.http.junit5;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.talend.sdk.component.junit.http.api.ResponseLocator;
import org.talend.sdk.component.junit.http.internal.junit5.JUnit5HttpApi;

@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(JUnit5HttpApi.class)
public @interface HttpApi {

    /**
     * @return the port to use for the server.
     */
    int port() default 0;

    /**
     * @return should the JVM be configured with the server as a proxy.
     */
    boolean globalProxyConfiguration() default true;

    /**
     * @return the log level for the network data.
     */
    String logLevel() default "DEBUG";

    /**
     * @return the response locator to use.
     */
    Class<? extends ResponseLocator> responseLocator() default ResponseLocator.class;

    /**
     * @return the header filter to use.
     */
    Class<? extends Predicate> headerFilter() default Predicate.class;

    /**
     * @return the executor to use.
     */
    Class<? extends Executor> executor() default Executor.class;

    /**
     * @return the SSLContext supplier to use.
     */
    Class<? extends Supplier> sslContext() default Supplier.class;

    /**
     * @return true if a default sslContext should be created for the test.
     */
    boolean useSsl() default false;

    /**
     * @return true if the proxy shouldn't add meta headers (X-Talend) at all.
     */
    boolean skipProxyHeaders() default false;
}
