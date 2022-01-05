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
package org.talend.sdk.component.junit.environment;

import java.lang.annotation.Annotation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DecoratingEnvironmentProvider implements EnvironmentProvider {

    private final EnvironmentProvider provider;

    @Override
    public AutoCloseable start(final Class<?> clazz, final Annotation[] annotations) {
        return provider.start(clazz, annotations);
    }

    public String getName() {
        return (BaseEnvironmentProvider.class.isInstance(provider)
                ? BaseEnvironmentProvider.class.cast(provider).getName()
                : provider.getClass().getSimpleName()).replace("Environment", "");
    }

    public boolean isActive() {
        return !Boolean.getBoolean(getName() + ".skip");
    }
}
