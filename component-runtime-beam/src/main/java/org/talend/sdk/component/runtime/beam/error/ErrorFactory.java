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
package org.talend.sdk.component.runtime.beam.error;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class ErrorFactory {

    public static RuntimeException toIllegalArgument(final Exception from) {
        final IllegalArgumentException illegalArgumentException = new IllegalArgumentException(from.getMessage());
        illegalArgumentException.setStackTrace(from.getStackTrace());
        return illegalArgumentException;
    }

    public static RuntimeException toIllegalState(final Exception from) {
        final IllegalStateException illegalStateException = new IllegalStateException(from.getMessage());
        illegalStateException.setStackTrace(from.getStackTrace());
        return illegalStateException;
    }
}
