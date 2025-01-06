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
package org.talend.sdk.component.runtime.di.schema;

public class JavaTypesManager {

    public static final String DEFAULT_CHAR = "' '"; //$NON-NLS-1$

    public static final String DEFAULT_BOOLEAN = "false"; //$NON-NLS-1$

    public static final String ANOTHER_BOOLEAN = "true"; //$NON-NLS-1$

    public static final String DEFAULT_NUMBER = "0"; //$NON-NLS-1$

    public static final String DEFAULT_STRING = ""; //$NON-NLS-1$

    public static final String NULL = "null"; //$NON-NLS-1$

    public static final String JAVA_PRIMITIVE_CHAR = "char"; //$NON-NLS-1$

    public static final String JAVA_PRIMITIVE_BOOLEAN = "boolean"; //$NON-NLS-1$

    public final JavaType BOOLEAN = new JavaType(Boolean.class, boolean.class);

    public final JavaType BYTE = new JavaType(Byte.class, byte.class);

    public final JavaType BYTE_ARRAY = new JavaType(byte[].class, false, false);

    public final JavaType CHARACTER = new JavaType(Character.class, char.class);

    public final JavaType DATE = new JavaType(java.util.Date.class, true, false);

    public final JavaType FILE = new JavaType(String.class, true, "File"); //$NON-NLS-1$

    public final JavaType DIRECTORY = new JavaType(String.class, true, "Directory"); //$NON-NLS-1$

    public final JavaType VALUE_LIST = new JavaType(String.class, true, "java.util.List Of Value"); //$NON-NLS-1$

    public final JavaType DOUBLE = new JavaType(Double.class, double.class);

    public final JavaType FLOAT = new JavaType(Float.class, float.class);

    public final JavaType INTEGER = new JavaType(Integer.class, int.class);

    public final JavaType LONG = new JavaType(Long.class, long.class);

    public final JavaType SHORT = new JavaType(Short.class, short.class);

    public final JavaType STRING = new JavaType(String.class, false, false);

    public final JavaType OBJECT = new JavaType(Object.class, false, true);

    public final JavaType DYNAMIC = new JavaType(Object.class, false, true);

    public final JavaType LIST = new JavaType(java.util.List.class, false, true);

    public final JavaType BIGDECIMAL = new JavaType(java.math.BigDecimal.class, false, true);

    public final JavaType PASSWORD = new JavaType(String.class, true, "Password"); //$NON-NLS-1$

    public final JavaType[] JAVA_TYPES = new JavaType[] { BOOLEAN, BYTE, BYTE_ARRAY, CHARACTER, DATE, DOUBLE, FLOAT,
            BIGDECIMAL, INTEGER, LONG, OBJECT, SHORT, STRING, LIST };

    public final String[] NUMBERS = new String[] { INTEGER.getId(), FLOAT.getId(), DOUBLE.getId(), LONG.getId(),
            SHORT.getId(), BIGDECIMAL.getId(), BYTE.getId() };

    private final java.util.List<String> JAVA_PRIMITIVE_TYPES = new java.util.ArrayList<String>();

    private final java.util.Set<String> PRIMITIVE_TYPES_SET = new java.util.HashSet<String>(JAVA_PRIMITIVE_TYPES);

    private java.util.Map<String, JavaType> shortNameToJavaType;

    private java.util.Map<String, JavaType> canonicalClassNameToJavaType;

    private java.util.Map<String, JavaType> labelToJavaType;

    private java.util.Map<String, JavaType> idToJavaType;

    private java.util.List<JavaType> javaTypes;

    public JavaTypesManager() {
        init();
    }

    private void init() {

        shortNameToJavaType = new java.util.HashMap<String, JavaType>();
        labelToJavaType = new java.util.HashMap<String, JavaType>();
        idToJavaType = new java.util.HashMap<String, JavaType>();
        canonicalClassNameToJavaType = new java.util.HashMap<String, JavaType>();
        javaTypes = new java.util.ArrayList<JavaType>();

        for (JavaType javaType : JAVA_TYPES) {
            addJavaType(javaType);
        }

        idToJavaType.put(PASSWORD.getId(), PASSWORD);
    }

    public void addJavaType(final JavaType javaType) {
        String primitiveName = null;
        Class primitiveClass = javaType.getPrimitiveClass();
        if (primitiveClass != null) {
            primitiveName = primitiveClass.getSimpleName();
            shortNameToJavaType.put(primitiveName, javaType);
            canonicalClassNameToJavaType.put(primitiveClass.getCanonicalName(), javaType);
            JAVA_PRIMITIVE_TYPES.add(primitiveClass.getSimpleName());
        }

        String nullableName = javaType.getNullableClass().getSimpleName();
        // if java type id exist , don't add it
        if (idToJavaType.keySet().contains(javaType.getId())) {
            return;
        }
        shortNameToJavaType.put(nullableName, javaType);
        canonicalClassNameToJavaType.put(javaType.getNullableClass().getCanonicalName(), javaType);
        labelToJavaType.put(javaType.getLabel(), javaType);
        idToJavaType.put(javaType.getId(), javaType);
        javaTypes.add(javaType);
    }

    public JavaType getJavaTypeFromName(final String typeName) {
        return shortNameToJavaType.get(typeName);
    }

    public String getShortNameFromJavaType(final JavaType javaType) {
        Class primitiveClass = javaType.getPrimitiveClass();
        if (primitiveClass != null) {
            return primitiveClass.getSimpleName();
        }
        return javaType.getNullableClass().getSimpleName();
    }

    public JavaType getJavaTypeFromLabel(final String label) {
        return labelToJavaType.get(label);
    }

    public JavaType getJavaTypeFromId(final String id) {
        JavaType javaTypeFromId = idToJavaType.get(id);
        if (javaTypeFromId == null) {
            throw new IllegalArgumentException("Unknown java id type : '" + id + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return javaTypeFromId;
    }

    public JavaType[] getJavaTypes() {
        return javaTypes.toArray(new JavaType[0]);
    }

    public JavaType getJavaTypeFromCanonicalName(final String canonicalName) {
        return canonicalClassNameToJavaType.get(canonicalName);
    }

    public String getTypeToGenerate(final String idType, final boolean nullable) {
        JavaType javaTypeFromId = getJavaTypeFromId(idType);
        return getTypeToGenerate(javaTypeFromId, nullable);
    }

    public String getTypeToGenerate(final JavaType javaType, final boolean nullable) {
        if (javaType == null) {
            return null;
        }
        Class primitiveClass = javaType.getPrimitiveClass();
        Class nullableClass = javaType.getNullableClass();
        if (nullable) {
            if (javaType.isGenerateWithCanonicalName()) {
                return nullableClass.getCanonicalName();
            } else {
                return nullableClass.getSimpleName();
            }
        } else {
            if (primitiveClass != null) {
                return javaType.getPrimitiveClass().getSimpleName();
            } else {
                if (javaType.isGenerateWithCanonicalName()) {
                    return nullableClass.getCanonicalName();
                } else {
                    return nullableClass.getSimpleName();
                }
            }
        }
    }

    public boolean isJavaPrimitiveType(final String type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        return PRIMITIVE_TYPES_SET.contains(type);
    }

    public boolean isJavaPrimitiveType(final String idType, final boolean nullable) {
        String typeToGenerate = getTypeToGenerate(idType, nullable);
        return isJavaPrimitiveType(typeToGenerate);
    }

    public boolean isJavaPrimitiveType(final JavaType javaType, final boolean nullable) {
        String typeToGenerate = getTypeToGenerate(javaType, nullable);
        return isJavaPrimitiveType(typeToGenerate);
    }

    public boolean isNumberType(final JavaType javaType) {
        return Number.class.isAssignableFrom(javaType.getNullableClass());
    }

    public boolean isNumberIntType(final JavaType javaType) {
        return javaType == BYTE || javaType == INTEGER || javaType == SHORT || javaType == LONG;
    }

    public String getDefaultValueFromJavaType(final String type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        if (isJavaPrimitiveType(type)) {
            if (type.equals(JAVA_PRIMITIVE_CHAR)) {
                return DEFAULT_CHAR;
            } else if (type.equals(JAVA_PRIMITIVE_BOOLEAN)) {
                return DEFAULT_BOOLEAN;
            } else {
                return DEFAULT_NUMBER;
            }
        } else {
            return NULL;
        }
    }

    public String getDefaultValueFromJavaType(final String type, final String defaultValue) {
        if (defaultValue != null && defaultValue.length() > 0) {
            return defaultValue;
        } else {
            return getDefaultValueFromJavaType(type);
        }

    }

    public String getDefaultValueFromJavaIdType(final String idType, final boolean nullable) {
        String typeToGenerate = getTypeToGenerate(idType, nullable);
        return getDefaultValueFromJavaType(typeToGenerate);
    }

    public String getDefaultValueFromJavaIdType(final String idType, final boolean nullable,
            final String defaultValue) {
        String typeToGenerate = getTypeToGenerate(idType, nullable);
        return getDefaultValueFromJavaType(typeToGenerate, defaultValue);
    }

    public JavaType getDefaultJavaType() {
        return STRING;
    }

    public boolean isString(final String type) {
        return STRING.getId().equals(type);
    }

    public boolean isBoolean(final String type) {
        return BOOLEAN.getId().equals(type);
    }
}
