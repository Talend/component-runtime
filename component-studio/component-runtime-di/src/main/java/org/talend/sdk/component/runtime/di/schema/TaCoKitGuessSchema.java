/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_KEY;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_LENGTH;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_PATTERN;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_PRECISION;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_TYPE;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaCoKitGuessSchema {

    public static final String STRING_ESCAPE = "\"";

    public static final String NO_COMPONENT = "No component ";

    public static final String TCOMP_CONFIGURATIONTYPE_TYPE = "tcomp::configurationtype::type";

    public static final String DATASET = "dataset";

    private ComponentManager componentManager;

    private JavaTypesManager javaTypesManager;

    private PrintStream out;

    private Map<String, Column> columns;

    private Map<String, String> configuration;

    private Map<Class, JavaType> class2JavaTypeMap;

    private Set<String> keysNoTypeYet;

    private final int lineLimit;

    private int lineCount;

    private String plugin;

    private String family;

    private String componentName;

    private String action;

    private final Integer version;

    private static final String SCHEMA_TYPE = "schema";

    private static final String SCHEMA_EXTENDED_TYPE = "schema_extended";

    private static final String EMPTY = ""; //$NON-NLS-1$

    public TaCoKitGuessSchema(final PrintStream out, final Map<String, String> configuration, final String plugin,
            final String family, final String componentName, final String action, final String version) {
        this.out = out;
        this.lineLimit = 50;
        this.lineCount = -1;
        this.componentManager = ComponentManager.instance();
        this.componentManager.autoDiscoverPlugins(false, true);
        this.configuration = configuration;
        this.plugin = plugin;
        this.family = family;
        this.componentName = componentName;
        this.action = action;
        this.columns = new LinkedHashMap<>();
        this.keysNoTypeYet = new HashSet<>();
        this.javaTypesManager = new JavaTypesManager();
        this.version = Optional.ofNullable(version).map(Integer::parseInt).orElse(null);
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

    public void guessComponentSchema(final Schema incomingSchema, final String outgoingBranch)
            throws Exception {
        try {
            final Collection<ServiceMeta> services = componentManager
                    .findPlugin(plugin)
                    .orElseThrow(() -> new IllegalArgumentException(NO_COMPONENT + plugin))
                    .get(ContainerComponentRegistry.class)
                    .getServices();
            ServiceMeta.ActionMeta actionRef = services
                    .stream()
                    .flatMap(s -> s.getActions().stream())
                    .filter(a -> a.getFamily().equals(family) &&
                            a.getType().equals(SCHEMA_EXTENDED_TYPE) &&
                            componentName.equals(a.getAction()))
                    .findFirst()
                    .orElse(null);
            // did not find action named like componentName, trying to find one matching action...
            if (actionRef == null) {
                actionRef = services
                        .stream()
                        .flatMap(s -> s.getActions().stream())
                        .filter(a -> a.getFamily().equals(family) && a.getType().equals(SCHEMA_EXTENDED_TYPE))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No action " + family + "#" + SCHEMA_EXTENDED_TYPE));
            }
            final Object schemaResult = actionRef.getInvoker()
                    .apply(buildActionConfig(actionRef, configuration, incomingSchema, outgoingBranch));
            if (schemaResult instanceof Schema && fromSchema(Schema.class.cast(schemaResult))) {
                return;
            }
        } catch (Exception e) {
            log.error("Can't guess processor schema through action.", e);
        }

        log.error("Result of built-in guess schema action is not an instance of Talend Component Kit Schema.");
        throw new Exception("There is no available schema found.");
    }

    private Map<String, String> buildActionConfig(final ServiceMeta.ActionMeta action,
            final Map<String, String> configuration, final Schema schema, final String branch) {
        final String schemaPath = action.getParameters()
                .get()
                .stream()
                .filter(p -> Schema.class.isAssignableFrom((Class) p.getJavaType()))
                .map(p -> p.getPath())
                .findFirst()
                .orElse("");
        final String branchPath = action.getParameters()
                .get()
                .stream()
                .filter(p -> String.class.isAssignableFrom((Class) p.getJavaType()))
                .map(ParameterMeta::getPath)
                .findFirst()
                .orElse("");

        final Map<String, String> mapped = new HashMap<>();
        if (!schemaPath.isEmpty()) {
            try (final Jsonb jsonb = JsonbBuilder.create()) {
                mapped.put(schemaPath, jsonb.toJson(schema));
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
        if (!branchPath.isEmpty()) {
            mapped.put(branchPath, branch);
        }
        if (configuration == null || configuration.isEmpty()) {
            return mapped;
        }
        final String prefix = action
                .getParameters()
                .get()
                .stream()
                .filter(s -> !s.getPath().equals(schemaPath) && !s.getPath().equals(branchPath))
                .map(ParameterMeta::getPath)
                .findFirst()
                .orElse(null);
        if (prefix == null) {
            return mapped;
        }
        mapped.putAll(configuration
                .entrySet()
                .stream()
                .filter(e -> isChildParameter(e.getKey(), prefix) || prefix.equals(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return mapped;
    }

    private Map<String, String> buildActionConfig(final ServiceMeta.ActionMeta action,
            final Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return configuration; // no-mapping
        }

        final String prefix = action
                .getParameters()
                .get()
                .stream()
                .filter(param -> param.getMetadata().containsKey(TCOMP_CONFIGURATIONTYPE_TYPE)
                        && DATASET.equals(param.getMetadata().get(TCOMP_CONFIGURATIONTYPE_TYPE)))
                .findFirst()
                .map(ParameterMeta::getPath)
                .orElse(null);

        if (prefix == null) { // no mapping to do
            return configuration;
        }

        final ParameterMeta dataSet = findDataset(action)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found for " + action.getAction()));

        final String dataSetPath = dataSet.getPath();
        return configuration
                .entrySet()
                .stream()
                .filter(e -> isChildParameter(e.getKey(), dataSetPath))
                .collect(toMap(e -> prefix + e.getKey().substring(dataSetPath.length()), Map.Entry::getValue));
    }

    private boolean isChildParameter(final String path, final String parentPath) {
        return path.startsWith(parentPath) && path.substring(parentPath.length()).startsWith(".");
    }

    private Optional<ParameterMeta> findDataset(final ServiceMeta.ActionMeta action) {
        final ComponentFamilyMeta familyMeta = findFamily();
        final ComponentFamilyMeta.BaseMeta<?> componentMeta = findComponent(familyMeta);

        // dataset name should be the same as DiscoverSchema action name
        final Collection<ParameterMeta> metas = toStream(componentMeta.getParameterMetas().get()).collect(toList());
        return ofNullable(metas
                .stream()
                .filter(p -> DATASET.equals(p.getMetadata().get(TCOMP_CONFIGURATIONTYPE_TYPE))
                        && action.getAction().equals(p.getMetadata().get("tcomp::configurationtype::name")))
                .findFirst()
                .orElseGet(() -> {
                    // find and use single dataset
                    final Iterator<ParameterMeta> iterator = metas
                            .stream()
                            .filter(p -> DATASET.equals(p.getMetadata().get(TCOMP_CONFIGURATIONTYPE_TYPE)))
                            .iterator();
                    if (iterator.hasNext()) {
                        final ParameterMeta value = iterator.next();
                        if (!iterator.hasNext()) {
                            return value;
                        }
                        log
                                .warn("Multiple potential datasets for {}:{}, ignoring parameters", action.getType(),
                                        action.getAction());
                    }
                    return null;
                }));
    }

    private ComponentFamilyMeta.BaseMeta<?> findComponent(final ComponentFamilyMeta familyMeta) {
        return Stream
                .concat(familyMeta.getPartitionMappers().entrySet().stream(),
                        familyMeta.getProcessors().entrySet().stream())
                .filter(e -> e.getKey().equals(componentName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(NO_COMPONENT + componentName));
    }

    private ComponentFamilyMeta findFamily() {
        return componentManager
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException("No component family " + plugin))
                .get(ContainerComponentRegistry.class)
                .getComponents()
                .get(family);
    }

    private Stream<ParameterMeta> toStream(final Collection<ParameterMeta> parameterMetas) {
        return Stream
                .concat(parameterMetas.stream(),
                        parameterMetas
                                .stream()
                                .map(ParameterMeta::getNestedParameters)
                                .filter(Objects::nonNull)
                                .flatMap(this::toStream));
    }

    private Optional<String> findFirstComponentDataSetName() {
        final ComponentFamilyMeta familyMeta = findFamily();
        final ComponentFamilyMeta.BaseMeta<?> componentMeta = findComponent(familyMeta);
        return toStream(componentMeta.getParameterMetas().get())
                .filter(p -> DATASET.equals(p.getMetadata().get(TCOMP_CONFIGURATIONTYPE_TYPE)))
                .findFirst()
                .map(p -> p.getMetadata().get("tcomp::configurationtype::name"));
    }

    public boolean guessSchemaThroughAction() {
        final Collection<ServiceMeta> services = componentManager
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException(NO_COMPONENT + plugin))
                .get(ContainerComponentRegistry.class)
                .getServices();

        final ServiceMeta.ActionMeta actionRef;
        if (action == null || action.isEmpty()) {
            // dataset name should be the same as DiscoverSchema action name so let's try to guess from the component
            actionRef = findFirstComponentDataSetName()
                    .flatMap(datasetName -> services
                            .stream()
                            .flatMap(s -> s.getActions().stream())
                            .filter(a -> a.getFamily().equals(family) && a.getType().equals(SCHEMA_TYPE))
                            .filter(a -> a.getAction().equals(datasetName))
                            .findFirst())
                    .orElse(null);
            if (actionRef == null) {
                return false;
            }
        } else {
            actionRef = services
                    .stream()
                    .flatMap(s -> s.getActions().stream())
                    .filter(a -> a.getFamily().equals(family) && a.getAction().equals(action)
                            && a.getType().equals(SCHEMA_TYPE))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No action " + family + "#" + SCHEMA_TYPE + "#" + action));
        }
        final Object schemaResult = actionRef.getInvoker().apply(buildActionConfig(actionRef, configuration));

        if (schemaResult instanceof Schema) {
            return fromSchema(Schema.class.cast(schemaResult));

        } else {
            log.error("Result of built-in guess schema action is not an instance of Talend Component Kit Schema");
            return false;
        }
    }

    private boolean fromSchema(final Schema schema) {
        final Collection<Schema.Entry> entries = schema.getEntries();
        if (entries == null || entries.isEmpty()) {
            log.info("No column found by guess schema action");
            return false;
        }
        for (Schema.Entry entry : entries) {
            String name = entry.getName();
            Schema.Type entryType = entry.getType();
            String dbName = entry.getOriginalFieldName();
            String pattern = entry.getProps().getOrDefault(STUDIO_PATTERN, null);
            String length = entry.getProps().getOrDefault(STUDIO_LENGTH, null);
            String precision = entry.getProps().getOrDefault(STUDIO_PRECISION, null);
            String isKey = entry.getProps().getOrDefault(STUDIO_KEY, null);
            String talendType = entry.getProps().getOrDefault(STUDIO_TYPE, "");
            boolean isDateTime = false;
            if (entryType == null) {
                entryType = Schema.Type.STRING;
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
                if (talendType.equals(javaTypesManager.SHORT.getId())) {
                    typeName = javaTypesManager.SHORT.getId();
                } else {
                    typeName = javaTypesManager.INTEGER.getId();
                }
                break;
            case LONG:
                typeName = javaTypesManager.LONG.getId();
                break;
            case FLOAT:
                typeName = javaTypesManager.FLOAT.getId();
                break;
            case BYTES:
                typeName = javaTypesManager.BYTE_ARRAY.getId();
                break;
            case DATETIME:
                typeName = javaTypesManager.DATE.getId();
                isDateTime = true;
                break;
            case RECORD:
                typeName = javaTypesManager.OBJECT.getId();
                break;
            case ARRAY:
                typeName = javaTypesManager.LIST.getId();
                break;
            case DECIMAL:
                typeName = javaTypesManager.BIGDECIMAL.getId();
                break;
            default:
                if (talendType.equals(javaTypesManager.CHARACTER.getId())) {
                    typeName = javaTypesManager.CHARACTER.getId();
                } else if (talendType.equals(javaTypesManager.BYTE.getId())) {
                    typeName = javaTypesManager.BYTE.getId();
                } else {
                    typeName = javaTypesManager.STRING.getId();
                }
                break;
            }

            final Column column = new Column();
            column.setLabel(name);
            column.setOriginalDbColumnName(dbName);
            column.setTalendType(typeName);
            column.setNullable(entry.isNullable());
            column.setComment(entry.getComment());
            if (length != null) {
                try {
                    column.setLength(Integer.valueOf(length));
                } catch (NumberFormatException e) {
                    // let default values if props are trash...
                }
            }
            if (precision != null) {
                try {
                    column.setPrecision(Integer.valueOf(precision));
                } catch (NumberFormatException e) {
                    // let default values if props are trash...
                }
            }
            if (isDateTime) {
                if (pattern != null) {
                    column.setPattern(STRING_ESCAPE + pattern + STRING_ESCAPE);
                } else {
                    // studio default pattern
                    column.setPattern(STRING_ESCAPE + "dd-MM-yyyy" + STRING_ESCAPE);
                }
            }
            if (isKey != null) {
                column.setKey(Boolean.parseBoolean(isKey));
            }
            if (entry.getDefaultValue() != null) {
                try {
                    column.setDefault(entry.getDefaultValue().toString());
                } catch (Exception e) {
                    // nevermind as it's almost useless...
                }
            }
            columns.put(name, column);
        }
        return true;
    }

    private boolean guessInputComponentSchemaThroughResult() throws Exception {
        // migration handler will be triggered and version of component will be passed
        // It is a Studio part. As fallback idea is to have Integer.MAX_VALUE in case if the version is null.
        // MAX_VALUE because we think that all appropriate migrations were already executed at the time when user can
        // invoke guess schema.
        final Integer version = ofNullable(this.version).orElse(Integer.MAX_VALUE);
        final Mapper mapper = componentManager
                .findMapper(family, componentName, version, configuration)
                .orElseThrow(() -> new IllegalArgumentException("Can't find " + family + "#" + componentName));
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
            if (rowObject instanceof Record) {
                return fromSchema(Record.class.cast(rowObject).getSchema());
            } else if (rowObject instanceof java.util.Map) {
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
        } else if (rowObject instanceof Schema) {
            return fromSchema(Schema.class.cast(rowObject));
        } else if (rowObject instanceof Record) {
            return fromSchema(Record.class.cast(rowObject).getSchema());
        } else if (rowObject instanceof java.util.Collection) {
            throw new Exception("Can't guess schema from a Collection");
        } else {
            return guessSchemaThroughResultClass(rowObject.getClass());
        }
    }

    private boolean guessSchemaThroughResultClass(final Class<?> rowClass) {
        final int originalSize = columns.size();
        for (final Field field : rowClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (isStatic(modifiers)) {
                continue;
            }

            final String name = field.getName();
            final Column column = new Column();
            column.setLabel(name);
            column.setOriginalDbColumnName(name);
            column.setTalendType(getTalendType(field.getType()));
            column.setNullable(!field.getType().isPrimitive());
            columns.put(name, column);
        }
        return originalSize != columns.size();
    }

    private boolean guessInputSchemaThroughResults(final Input input, final Map<String, ?> rowObject) {
        keysNoTypeYet.clear();
        final int originalSize = columns.size();

        Map<String, ?> row = rowObject;
        while (!guessSchemaThroughResult(row)) {
            row = (Map<String, ?>) input.next();
            if (row == null) {
                break;
            }
        }
        for (final String key : keysNoTypeYet) {
            final Column column = new Column();
            column.setLabel(key);
            column.setOriginalDbColumnName(key);
            column.setTalendType(getTalendType(Object.class));
            column.setNullable(true);
            columns.put(key, column);
        }

        return originalSize != columns.size();
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
            for (final String key : keysNoTypeYet) {
                final Column column = new Column();
                column.setLabel(key);
                column.setOriginalDbColumnName(key);
                column.setTalendType(getTalendType(Object.class));
                column.setNullable(true);
                columns.put(key, column);
            }
            keysNoTypeYet.clear();
            return true;
        }
        ++lineCount;
        java.util.Iterator<String> iter = keysNoTypeYet.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            Object result = rowObject.get(key);
            if (result == null) {
                continue;
            }
            final String type;
            if (Record.class.isInstance(rowObject)) {
                type = getTalendType(Object.class);
            } else if (JsonObject.class.isInstance(rowObject)) {
                // can't judge by the result variable, since common map may contains JsonValue
                type = getTalendType((JsonValue) result);
            } else {
                type = getTalendType(result.getClass());
            }
            if (type == null || type.trim().isEmpty()) {
                continue;
            }

            final Column column = new Column();
            column.setLabel(key);
            column.setOriginalDbColumnName(key);
            column.setTalendType(type);
            column.setNullable(true);
            columns.put(key, column);

            iter.remove();
        }
        return keysNoTypeYet.isEmpty();
    }

    public synchronized void close() {
        if (!columns.isEmpty()) {
            try (final Jsonb jsonb = JsonbBuilder.create()) {
                jsonb.toJson(columns.values(), out);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
            out.flush();
            columns = new LinkedHashMap<>();
        }
    }

    protected String getTalendType(final JsonValue value) {
        switch (value.getValueType()) {
        case TRUE:
        case FALSE:
            return javaTypesManager.BOOLEAN.getId();
        case NUMBER:
            final Number number = JsonNumber.class.cast(value).numberValue();
            if (Long.class.isInstance(number)) {
                return javaTypesManager.LONG.getId();
            }
            if (BigDecimal.class.isInstance(number)) {
                return javaTypesManager.BIGDECIMAL.getId();
            } else {
                return javaTypesManager.DOUBLE.getId();
            }
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
