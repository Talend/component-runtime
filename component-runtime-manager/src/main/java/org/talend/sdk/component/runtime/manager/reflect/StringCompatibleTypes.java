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
package org.talend.sdk.component.runtime.manager.reflect;

import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.Type;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class StringCompatibleTypes {

    static boolean isKnown(final Type type, final PropertyEditorRegistry registry) {
        return String.class == type || char.class == type || Character.class == type
                || (Class.class.isInstance(type) && registry.findConverter(Class.class.cast(type)) != null);
    }
}
