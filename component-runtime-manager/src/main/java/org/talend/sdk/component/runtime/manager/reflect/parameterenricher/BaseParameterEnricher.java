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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.function.Supplier;

import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

import lombok.Data;

public abstract class BaseParameterEnricher implements ParameterExtensionEnricher {

    private final ThreadLocal<Context> context = new ThreadLocal<>();

    public <T> T withContext(final Context context, final Supplier<T> task) {
        this.context.set(context);
        try {
            return task.get();
        } finally {
            this.context.remove();
        }
    }

    protected Optional<Context> getContext() {
        final Context value = context.get();
        if (value == null) {
            context.remove();
        }
        return ofNullable(value);
    }

    @Data
    public static class Context {

        private final LocalConfiguration configuration;
    }
}
