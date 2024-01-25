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
package org.talend.sdk.component.junit.environment;

import java.lang.annotation.Annotation;

public interface EnvironmentProvider {

    /**
     * Starts the environment represented by this implementation.
     *
     * @param clazz the test class.
     * @param annotations the current annotations which can own environment configuration.
     * @return a closeable to release current environment.
     */
    AutoCloseable start(Class<?> clazz, Annotation[] annotations);
}
