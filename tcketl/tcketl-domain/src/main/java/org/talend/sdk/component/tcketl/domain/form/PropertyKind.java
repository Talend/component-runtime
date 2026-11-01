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

/** What a node of the property tree is once the shape of its children is known. */
public enum PropertyKind {

    OBJECT,

    /** An object made of the two parallel lists {@code key[]} and {@code value[]}. */
    MAP,

    ARRAY_OF_OBJECT,

    ARRAY_OF_PRIMITIVE,

    STRING,

    NUMBER,

    BOOLEAN,

    ENUM;

    public boolean isArray() {
        return this == ARRAY_OF_OBJECT || this == ARRAY_OF_PRIMITIVE;
    }

    /** Arrays and maps: values that repeat, stored with an index. */
    public boolean isCollection() {
        return isArray() || this == MAP;
    }

    /** A single editable value. */
    public boolean isScalar() {
        return this == STRING || this == NUMBER || this == BOOLEAN || this == ENUM;
    }
}
