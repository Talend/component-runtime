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
package org.talend.sdk.component.form.model.jsonschema;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.talend.sdk.component.form.model.jsonschema.JsonSchema.jsonSchema;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

import lombok.Data;

@Data
class PojoJsonSchemaBuilder {

    private final Map<Class<?>, JsonSchema> schemas = new HashMap<>();

    public JsonSchema.Builder create(final Class<?> pojo) {
        final JsonSchema.Builder builder = jsonSchema().withTitle(pojo.getSimpleName()).withType("object");
        Class<?> current = pojo;
        final Collection<String> excludes = new HashSet<>();
        while (current != Object.class && current != null) {
            Stream.of(current.getDeclaredFields()).filter(f -> !f.getName().equals("$jacocoData")).filter(f -> {
                final int modifiers = f.getModifiers();
                return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers);
            })
                    .filter(f -> !f.isAnnotationPresent(JsonSchemaIgnore.class))
                    .filter(f -> excludes.add(f.getName()))
                    .sorted(comparing(Field::getName))
                    .forEach(field -> {
                        final String name = ofNullable(field.getAnnotation(JsonSchemaProperty.class))
                                .map(JsonSchemaProperty::value)
                                .orElseGet(field::getName);
                        builder.withProperty(name, buildSchema(field));
                    });
            current = current.getSuperclass();
        }
        return builder;
    }

    private JsonSchema buildSchema(final Field field) {
        final Type genericType = field.getGenericType();

        if ((Class.class.isInstance(genericType) && CharSequence.class.isAssignableFrom(Class.class.cast(genericType)))
                || genericType == char.class || genericType == Character.class) {
            return schemas.computeIfAbsent(Class.class.cast(genericType), k -> jsonSchema().withType("string").build());
        } else if (genericType == long.class || genericType == Long.class || genericType == int.class
                || genericType == Integer.class || genericType == byte.class || genericType == Byte.class
                || genericType == short.class || genericType == Short.class || genericType == double.class
                || genericType == Double.class || genericType == float.class || genericType == Float.class
                || genericType == BigDecimal.class || genericType == BigInteger.class) {
            return schemas.computeIfAbsent(Class.class.cast(genericType), k -> jsonSchema().withType("number").build());
        } else if (genericType == boolean.class || genericType == Boolean.class) {
            return schemas
                    .computeIfAbsent(Class.class.cast(genericType), k -> jsonSchema().withType("boolean").build());
        } else if (Class.class.isInstance(genericType)) {
            final Class<?> clazz = Class.class.cast(genericType);
            return ofNullable(schemas.get(clazz)).orElseGet(() -> {
                final JsonSchema jsonSchema = create(clazz).build();
                schemas.put(clazz, jsonSchema);
                return jsonSchema;
            });
        } else if (ParameterizedType.class.isInstance(genericType)) {
            final ParameterizedType pt = ParameterizedType.class.cast(genericType);
            final Type rawType = pt.getRawType();
            if (!Class.class.isInstance(rawType)) {
                throw new IllegalArgumentException("Unsupported raw type: " + pt + ", this must be a Class");
            }
            final Class<?> rawClazz = Class.class.cast(rawType);
            if (Collection.class.isAssignableFrom(rawClazz) && pt.getActualTypeArguments().length == 1) {
                final Type itemType = pt.getActualTypeArguments()[0];
                if (!Class.class.isInstance(itemType)) {
                    throw new IllegalArgumentException(
                            "Unsupported generic type for item type: " + pt + ", this must be a Class");
                }
                final Class itemClass = Class.class.cast(itemType);
                final JsonSchema nested = ofNullable(schemas.get(itemType)).orElseGet(() -> {
                    final JsonSchema jsonSchema = create(itemClass).build();
                    schemas.put(itemClass, jsonSchema);
                    return jsonSchema;
                });
                return jsonSchema().withType("array").withItems(nested).build();
            } else if (Map.class.isAssignableFrom(rawClazz) && pt.getActualTypeArguments().length == 2) {
                final Type keyType = pt.getActualTypeArguments()[0];
                final Type valueType = pt.getActualTypeArguments()[1];
                if (!Class.class.isInstance(keyType) || !Class.class.isInstance(valueType)) {
                    throw new IllegalArgumentException("Unsupported generic type for key or value type: " + pt
                            + ", these must be Class instances");
                }
                return jsonSchema().withType("object").build();
            } else {
                throw new IllegalArgumentException(
                        "Unsupported raw type: " + pt + ", this must be a Collection or Map");
            }
        }
        throw new IllegalArgumentException("Unsupported generic type: " + genericType);
    }
}
