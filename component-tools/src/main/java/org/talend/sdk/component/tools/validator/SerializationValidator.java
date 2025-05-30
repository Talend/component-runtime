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
package org.talend.sdk.component.tools.validator;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;

public class SerializationValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return components
                .stream()
                .filter(this::isNotSerializable)
                .map((Class clazz) -> clazz + " is not Serializable")
                .sorted();
    }

    private boolean isNotSerializable(final Class<?> clazz) {
        return !Serializable.class.isAssignableFrom(clazz);
    }
}
