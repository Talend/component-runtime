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
package org.talend.sdk.component.runtime.di.schema;

public class JavaType {

    private String label;

    private String id;

    private Class nullableClass;

    private Class primitiveClass;

    private boolean generateWithCanonicalName;

    // only to know for object input/output stream, if should base on readObject or not.
    private boolean objectBased;

    protected JavaType(final Class nullableClass, final Class primitiveClass) {
        super();
        this.nullableClass = nullableClass;
        this.primitiveClass = primitiveClass;
        this.label = primitiveClass.getSimpleName() + " | " + nullableClass.getSimpleName(); //$NON-NLS-1$
        this.id = createId(nullableClass.getSimpleName());
    }

    public JavaType(final Class nullableClass, final boolean generateWithCanonicalName, final boolean objectBased) {
        super();
        this.nullableClass = nullableClass;
        this.label = nullableClass.getSimpleName();
        this.id = createId(nullableClass.getSimpleName());
        this.generateWithCanonicalName = generateWithCanonicalName;
        this.objectBased = objectBased;
    }

    public JavaType(final Class nullableClass, final boolean generateWithCanonicalName, final String label) {
        super();
        this.nullableClass = nullableClass;
        this.label = label;
        this.id = createId(label);
        this.generateWithCanonicalName = generateWithCanonicalName;
    }

    protected JavaType(final String id, final Class nullableClass, final Class primitiveClass, final String label) {
        super();
        this.label = label;
        this.nullableClass = nullableClass;
        this.primitiveClass = primitiveClass;
        this.id = createId(nullableClass.getSimpleName());
    }

    protected JavaType(final String id, final Class nullableClass, final Class primitiveClass) {
        super();
        this.id = id;
        this.nullableClass = nullableClass;
        this.primitiveClass = primitiveClass;
    }

    public JavaType(final String id, final Class nullableClass) {
        super();
        this.id = id;
        this.nullableClass = nullableClass;
    }

    private String createId(final String value) {
        return "id_" + value; //$NON-NLS-1$
    }

    public String getLabel() {
        return this.label;
    }

    public String getId() {
        return this.id;
    }

    public Class getNullableClass() {
        return this.nullableClass;
    }

    public Class getPrimitiveClass() {
        return this.primitiveClass;
    }

    public boolean isGenerateWithCanonicalName() {
        return this.generateWithCanonicalName;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("JavaType["); //$NON-NLS-1$
        buffer.append("label = ").append(label); //$NON-NLS-1$
        buffer.append(" nullableClass = ").append(nullableClass); //$NON-NLS-1$
        buffer.append(" primitiveClass = ").append(primitiveClass); //$NON-NLS-1$
        buffer.append("]"); //$NON-NLS-1$
        return buffer.toString();
    }

    public boolean isPrimitive() {
        return this.primitiveClass != null;
    }

    public boolean isObjectBased() {
        return objectBased;
    }

    public void setObjectBased(final boolean objectBased) {
        this.objectBased = objectBased;
    }
}
