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
package org.talend.sdk.component.runtime.output;

import static lombok.AccessLevel.PRIVATE;

import java.util.stream.Stream;

import org.talend.sdk.component.api.processor.Output;

import lombok.NoArgsConstructor;

/**
 * Utilities to read the branches declared by {@link Output} annotated parameters.
 */
@NoArgsConstructor(access = PRIVATE)
public class OutputBranches {

    /**
     * Resolves the branches declared by an {@code @Output} parameter, whichever flavor was used:
     * {@link Output#branches()} for a {@code MultiOutputIterator} parameter, {@link Output#value()} otherwise.
     *
     * @param output the annotation to read.
     * @return the declared branch names.
     */
    public static Stream<String> of(final Output output) {
        return output.branches().length > 0 ? Stream.of(output.branches()) : Stream.of(output.value());
    }
}
