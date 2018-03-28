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
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.service.schema.Schema;
import org.talend.sdk.component.api.service.schema.Type;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaCoKitGuessSchema {

    private ComponentManager componentManager;

    private JavaTypesManager javaTypesManager;

    private PrintStream out;

    private StringBuilder str;

    private Map<String, String> configuration;

    private Map<Class, JavaType> class2JavaTypeMap;

    private Map<String, Object> globalMap = null;

    private Set<String> keysNoTypeYet;

    private final int lineLimit;

    private int lineCount;

    private String plugin;

    private String family;

    private String componentName;

    private String action;

    private final String type = "schema";

    private static final String EMPTY = ""; //$NON-NLS-1$

    public TaCoKitGuessSchema(final PrintStream out, final Map<String, Object> globalMap,
            final Map<String, String> configuration, final String plugin, final String family,
            final String componentName, final String action) {
        this.out = out;
        this.lineLimit = 50;
        this.lineCount = -1;
        this.componentManager = ComponentManager.instance();
        this.globalMap = globalMap;
        this.configuration = configuration;
        this.plugin = plugin;
        this.family = family;
        this.componentName = componentName;
        this.action = action;
        str = new StringBuilder();
        keysNoTypeYet = new HashSet<>();
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

    public void fromOutputEmitterPojo(final Processor processor, final String outBranchName) {
        Object o = processor;
        while (Delegated.class.isInstance(o)) {
            o = Delegated.class.cast(o).getDelegate();
        }
        final ClassLoader classLoader = o.getClass().getClassLoader();
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            final Optional<java.lang.reflect.Type> type = Stream
                    .of(o.getClass().getMethods())
                    .filter(m -> m.isAnnotationPresent(ElementListener.class))
                    .flatMap(m -> IntStream
                            .range(0, m.getParameterCount())
                            .filter(i -> m.getParameters()[i].isAnnotationPresent(Output.class)
                                    && outBranchName.equals(m.getParameters()[i].getAnnotation(Output.class).value()))
                            .mapToObj(i -> m.getGenericParameterTypes()[i])
                            .filter(t -> ParameterizedType.class.isInstance(t)
                                    && ParameterizedType.class.cast(t).getRawType() == OutputEmitter.class
                                    && ParameterizedType.class.cast(t).getActualTypeArguments().length == 1)
                            .map(p -> ParameterizedType.class.cast(p).getActualTypeArguments()[0]))
                    .findFirst();
            if (type.isPresent() && Class.class.isInstance(type.get())) {
                final Class<?> clazz = Class.class.cast(type.get());
                if (clazz != JsonObject.class) {
                    guessSchemaThroughResultClass(clazz);
                }
            }
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public void guessInputComponentSchema() throws Exception {
        try {
            if (guessSchemaThroughAction()) {
                return;
            }
        } catch (Exception e) {
            log.error("Can't guess schema through action.", e);
        }
        if (guessInputComponentSchemaThroughResult()) {
            return;
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

    public boolean guessSchemaThroughAction() {
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

    private boolean guessInputComponentSchemaThroughResult() throws Exception {
        final Mapper mapper = componentManager.findMapper(family, componentName, 1, configuration).orElseThrow(
                () -> new IllegalArgumentException("Can't find " + family + "#" + componentName));
        if (JobStateAware.class.isInstance(mapper)) {
            JobStateAware.class.cast(mapper).setState(new JobStateAware.State());
        }
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
            if (rowObject instanceof java.util.Map) {
                return guessInputSchemaThroughResults(input, (java.util.Map) rowObject);
            } else if (rowObject instanceof java.util.Collection) {
                throw new Exception("Can't guess schema from a Collection");
            } else {
                return guessSchemaThroughResultClass(rowObject.getClass());
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

    /**
     * Guess schema through result row
     *
     * @param rowObject result row
     * @return true if completed; false if one more result row is needed.
     */
    public boolean guessSchemaThroughResult(final Object rowObject) throws Exception {
        if (rowObject instanceof java.util.Map) {
            return guessSchemaThroughResult((java.util.Map) rowObject);
        } else if (rowObject instanceof java.util.Collection) {
            throw new Exception("Can't guess schema from a Collection");
        } else {
            return guessSchemaThroughResultClass(rowObject.getClass());
        }
    }

    private boolean guessSchemaThroughResultClass(final Class<?> rowClass) {
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

    private boolean guessInputSchemaThroughResults(final Input input, final Map<String, ?> rowObject) {
        keysNoTypeYet.clear();
        final int originalSize = str.length();

        Map<String, ?> row = rowObject;
        while (!guessSchemaThroughResult(row)) {
            row = (Map<String, ?>) input.next();
            if (row == null) {
                break;
            }
        }
        if (!keysNoTypeYet.isEmpty()) {
            for (String key : keysNoTypeYet) {
                append(str, key, getTalendType(Object.class));
            }
        }

        return originalSize != str.length();
    }

    /**
     * Guess schema through result row
     *
     * @param rowObject result row
     * @return true if completed; false if one more result row is needed.
     */
    private boolean guessSchemaThroughResult(final Map<String, ?> rowObject) {
        if (rowObject == null) {
            return false;
        }
        if (keysNoTypeYet.isEmpty() && lineCount < 0) {
            keysNoTypeYet.addAll(rowObject.keySet());
            lineCount = 0;
        }
        if (lineLimit <= lineCount) {
            if (!keysNoTypeYet.isEmpty()) {
                for (String key : keysNoTypeYet) {
                    append(str, key, getTalendType(Object.class));
                }
                keysNoTypeYet.clear();
            }
            return true;
        }
        ++lineCount;
        java.util.Iterator<String> iter = keysNoTypeYet.iterator();
        boolean isJsonObject = JsonObject.class.isInstance(rowObject);
        while (iter.hasNext()) {
            String key = iter.next();
            Object result = rowObject.get(key);
            if (result == null) {
                continue;
            }
            String type = null;
            if (isJsonObject) {
                // can't judge by the result variable, since common map may contains JsonValue
                type = getTalendType((JsonValue) result);
            } else {
                type = getTalendType(result.getClass());
            }
            if (type == null || type.trim().isEmpty()) {
                continue;
            }

            append(str, key, type);

            iter.remove();
        }
        return keysNoTypeYet.isEmpty();
    }

    public synchronized void close() {
        if (str.length() != 0) {
            String lines = str.toString();
            out.println(lines);
            out.flush();
            str = new StringBuilder();
        }
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
        case NULL:
            return EMPTY;
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
