/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toMap;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.talend.sdk.component.api.service.schema.Schema;
import org.talend.sdk.component.api.service.schema.Type;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaCoKitGuessSchema {

    private ComponentManager componentManager;

    private Map<String, String> configuration;

    private Map<Class, JavaType> class2JavaTypeMap;

    private int lineLimit;

    private String plugin;

    private String family;

    private String componentName;

    private String action;

    private final String type = "schema";

    private JavaTypesManager javaTypesManager;

    public TaCoKitGuessSchema(final Map<String, String> configuration, final String plugin, final String family,
            final String componentName, final String action) {
        this.lineLimit = 50;
        this.componentManager = ComponentManager.instance();
        this.configuration = configuration;
        this.plugin = plugin;
        this.family = family;
        this.componentName = componentName;
        this.action = action;
        javaTypesManager = new JavaTypesManager();
        initClass2JavaTypeMap();
    }

    private void initClass2JavaTypeMap() {
        class2JavaTypeMap = new HashMap<>();
        JavaType javaTypes[] = javaTypesManager.getJavaTypes();
        for (JavaType javaType : javaTypes) {
            Class nullableClass = javaType.getNullableClass();
            if (nullableClass != null) {
                class2JavaTypeMap.put(nullableClass, javaType);
            }
            Class primitiveClass = javaType.getPrimitiveClass();
            if (primitiveClass != null) {
                class2JavaTypeMap.put(primitiveClass, javaType);
            }
        }
    }

    public void guessSchema(final PrintStream out) throws Exception {
        final StringBuilder str = new StringBuilder();
        try {
            if (guessSchemaThroughAction(str)) {
                return;
            }
            if (guessSchemaThroughResult(str)) {
                return;
            }
        } finally {
            if (str.length() != 0) {
                out.println(str.toString());
            }
        }
        throw new Exception("There is no available schema found.");
    }

    private Map<String, String> buildActionConfig(final ServiceMeta.ActionMeta action,
            final Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return configuration; // no-mapping
        }

        final ParameterMeta dataSet = action
                .getParameters()
                .stream()
                .filter(param -> param.getMetadata().containsKey("tcomp::configurationtype::type")
                        && "dataset".equals(param.getMetadata().get("tcomp::configurationtype::type")))
                .findFirst()
                .orElse(null);

        if (dataSet == null) { // no mapping to do
            return configuration;
        }

        final String prefix = dataSet.getPath(); // action configuration prefix.
        final int dotIndex = configuration.keySet().iterator().next().indexOf(".");
        return configuration.entrySet().stream().collect(toMap(
                e -> prefix + "." + e.getKey().substring(dotIndex + 1, e.getKey().length()), Map.Entry::getValue));
    }

    private boolean guessSchemaThroughAction(final StringBuilder str) {
        if (action == null || action.isEmpty()) {
            return false;
        }

        final ServiceMeta.ActionMeta actionRef = componentManager
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException("No component " + plugin))
                .get(ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .flatMap(s -> s.getActions().stream())
                .filter(a -> a.getFamily().equals(family) && a.getAction().equals(action) && a.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No action " + family + "#" + type + "#" + action));

        final Object schemaResult = actionRef.getInvoker().apply(buildActionConfig(actionRef, configuration));

        if (schemaResult instanceof Schema) {
            Collection<Schema.Entry> entries = Schema.class.cast(schemaResult).getEntries();
            if (entries == null || entries.isEmpty()) {
                log.info("No column found by guess schema action");
                return false;
            }

            for (Schema.Entry entry : entries) {
                String name = entry.getName();
                Type entryType = entry.getType();
                if (entryType == null) {
                    entryType = Type.STRING;
                }
                String typeName;
                switch (entryType) {
                case BOOLEAN:
                    typeName = javaTypesManager.BOOLEAN.getId();
                    break;
                case DOUBLE:
                    typeName = javaTypesManager.DOUBLE.getId();
                    break;
                case INT:
                    typeName = javaTypesManager.INTEGER.getId();
                    break;
                default:
                    typeName = javaTypesManager.STRING.getId();
                    break;
                }

                append(str, name, typeName);
            }
            return true;

        } else {
            log.error("Result of built-in guess schema action is not an instance of TaCoKit Schema");
            return false;
        }
    }

    private void append(final StringBuilder str, final String name, final String type) {
        str.append(name == null ? "" : name).append(';').append(type == null ? "" : type).append("\n");
    }

    private boolean guessSchemaThroughResult(final StringBuilder strBuff) throws Exception {
        Mapper mapper = componentManager.findMapper(family, componentName, 1, configuration).orElseThrow(
                () -> new IllegalArgumentException("Can't find " + family + "#" + componentName));
        Input input = null;
        try {
            mapper.start();
            final ChainedMapper chainedMapper = new ChainedMapper(mapper, mapper.split(mapper.assess()).iterator());
            chainedMapper.start();
            input = chainedMapper.create();
            input.start();
            Object rowObject = input.next();
            if (rowObject == null) {
                return false;
            }
            if (JsonObject.class.isInstance(rowObject)) {
                return guessSchemaThroughResultMap(strBuff, input, JsonObject.class.cast(rowObject));
            } else if (rowObject instanceof java.util.Map) {
                return guessSchemaThroughResultMap(strBuff, input, (java.util.Map) rowObject);
            } else if (rowObject instanceof java.util.Collection) {
                throw new Exception("Can't guess schema from a Collection");
            } else {
                return guessSchemaThroughResultClass(strBuff, rowObject);
            }
        } finally {
            if (input != null) {
                try {
                    input.stop();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            try {
                mapper.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private boolean guessSchemaThroughResultClass(final StringBuilder str, final Object rowObject) {
        Class<?> rowClass = rowObject.getClass();
        final int originalSize = str.length();
        Field[] fields = rowClass.getDeclaredFields();
        if (fields != null && 0 < fields.length) {
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (isStatic(modifiers)) {
                    continue;
                }

                append(str, field.getName(), getTalendType(field.getType()));
            }
        }
        return originalSize != str.length();
    }

    // todo: rewrite this part, there are too much copy/paste
    private boolean guessSchemaThroughResultMap(final StringBuilder str, final Input input,
            final JsonObject rowObject) {
        Set<String> keys = rowObject.keySet();
        if (keys.isEmpty()) {
            return false;
        }
        final int originalSize = str.length();
        Set<String> keysNoTypeYet = new HashSet<>(keys);
        JsonObject row = rowObject;
        for (int line = 0; row != null && !keysNoTypeYet.isEmpty() && line < lineLimit; line++, row =
                (javax.json.JsonObject) input.next()) {
            Iterator<String> iter = keysNoTypeYet.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                javax.json.JsonValue result = row.get(key);
                if (result == null || result == javax.json.JsonValue.NULL) {
                    continue;
                }
                append(str, key, getTalendType(result));
                iter.remove();
            }
        }
        if (!keysNoTypeYet.isEmpty()) {
            for (String key : keysNoTypeYet) {
                append(str, key, getTalendType(Object.class));
            }
        }
        return originalSize != str.length();
    }

    private boolean guessSchemaThroughResultMap(final StringBuilder str, final Input input,
            final Map<String, Object> rowObject) {
        Set<String> keys = rowObject.keySet();
        if (keys.isEmpty()) {
            return false;
        }
        final int originalSize = str.length();
        Set<String> keysNoTypeYet = new java.util.HashSet<>(keys);
        Map row = rowObject;
        for (int line = 0; row != null && !keysNoTypeYet.isEmpty() && line < lineLimit; line++, row =
                (java.util.Map) input.next()) {
            java.util.Iterator<String> iter = keysNoTypeYet.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                Object result = row.get(key);
                if (result == null) {
                    continue;
                }

                append(str, key, getTalendType(result.getClass()));

                iter.remove();
            }
        }
        if (!keysNoTypeYet.isEmpty()) {
            for (String key : keysNoTypeYet) {
                append(str, key, getTalendType(Object.class));
            }
        }
        return originalSize != str.length();
    }

    private String getTalendType(final JsonValue value) {
        switch (value.getValueType()) {
        case TRUE:
        case FALSE:
            return javaTypesManager.BOOLEAN.getId();
        case NUMBER:
            return javaTypesManager.DOUBLE.getId();
        case STRING:
            return javaTypesManager.STRING.getId();
        case OBJECT:
        default:
            return javaTypesManager.OBJECT.getId();
        }
    }

    private String getTalendType(final Class type) {
        if (type == null) {
            return javaTypesManager.OBJECT.getId();
        }
        JavaType javaType = class2JavaTypeMap.get(type);
        if (javaType != null) {
            return javaType.getId();
        }
        return javaTypesManager.OBJECT.getId();
    }
}
