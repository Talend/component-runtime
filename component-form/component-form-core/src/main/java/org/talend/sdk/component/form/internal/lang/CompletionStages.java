/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.lang;

import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.talend.sdk.component.form.internal.converter.PropertyContext;

import lombok.NoArgsConstructor;

/**
 * Generics hack, allows
 *
 * <code>.map(CompletionStages::toStage)</code>
 *
 * to work with our API where
 *
 * <code>.map(CompletableFuture::completedFuture)</code>
 *
 * would fail due to the double generic usage.
 */
@NoArgsConstructor(access = PRIVATE)
public final class CompletionStages {

    public static <T> T get(final CompletableFuture<T> it) {
        try {
            return it.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    public static CompletionStage<PropertyContext<?>> toStage(final PropertyContext<?> context) {
        return CompletableFuture.completedFuture(context);
    }
}
