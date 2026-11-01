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
package org.talend.sdk.component.tcketl.domain.form;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Turns a path written in a property's metadata (a condition target, an action parameter) into an absolute path. It
 * behaves like the resolver of the reference form renderer: a bare name is a sibling, {@code ..} goes up one level,
 * {@code .} and {@code ./x} are relative to the property itself, and anything else is already absolute.
 *
 * <p>
 * Both definition paths ({@code list[].b}) and concrete paths ({@code list[2].b}) can be resolved; the result keeps
 * the same form as the input.
 */
public final class PathResolver {

    private static final String PARENT = "..";

    private PathResolver() {
        // utility
    }

    public static String resolve(final String propertyPath, final String reference) {
        Preconditions.requireNonNull(propertyPath, "propertyPath");
        Preconditions.requireNonNull(reference, "reference");
        final String normalized = reference.contains(".") ? reference : "../" + reference;
        if (".".equals(normalized)) {
            return propertyPath;
        }
        if (normalized.startsWith(PARENT)) {
            return goUp(propertyPath, normalized);
        }
        if (normalized.startsWith(".")) {
            return propertyPath + '.' + normalized.replaceFirst("\\./?", "").replace('/', '.');
        }
        return normalized;
    }

    private static String goUp(final String propertyPath, final String reference) {
        String current = propertyPath;
        String remaining = reference;
        while (remaining.startsWith(PARENT)) {
            current = current.substring(0, Math.max(0, current.lastIndexOf('.')));
            remaining = remaining.substring(PARENT.length());
            if (remaining.startsWith("/")) {
                remaining = remaining.substring(1);
            }
            if (current.isEmpty()) {
                break;
            }
        }
        return Stream.of(current, remaining.replace('/', '.'))
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining("."));
    }
}
