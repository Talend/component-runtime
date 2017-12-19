/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.ui.guessschema.jet;

/**
 * DOC cmeng class global comment. Detailled comment
 */
@SuppressWarnings("nls")
class TaCoKitGuessSchema {

    private org.talend.sdk.component.runtime.manager.ComponentManager componentManager;

    private java.util.Map<String, String> configuration;

    private java.util.Map<Class, JavaType> class2JavaTypeMap;

    private String plugin;

    private String family;

    private String componentName;

    private String action;

    private String type;

    private String tempFilePath;

    private String encoding;

    private final char separator = ';';

    private final char quotechar = '"';

    private final char escapechar = '"';

    private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());

    private JavaTypesManager javaTypesManager;

    private JavaDataTypeHelper javaDataTypeHelper;

    public TaCoKitGuessSchema(final java.util.Map<String, String> configuration, final String plugin,
            final String family, final String componentName, final String action, final String type,
            final String tempFilePath, final String encoding) {
        this.componentManager = org.talend.sdk.component.runtime.manager.ComponentManager.instance();
        this.configuration = configuration;
        this.plugin = plugin;
        this.family = family;
        this.componentName = componentName;
        this.action = action;
        this.type = type;
        this.tempFilePath = tempFilePath;
        this.encoding = encoding;
        javaTypesManager = new JavaTypesManager();
        javaDataTypeHelper = new JavaDataTypeHelper();
        initClass2JavaTypeMap();
    }

    private void initClass2JavaTypeMap() {
        class2JavaTypeMap = new java.util.HashMap<>();
        JavaType javaTypes[] = javaTypesManager.getJavaTypes();
        for (JavaType javaType : javaTypes) {
            class2JavaTypeMap.put(javaType.getNullableClass(), javaType);
        }
    }

    public void guessSchema(final StringBuffer strBuff) throws Exception {
        boolean succeed = false;
        try {
            if (guessSchemaThroughAction(strBuff)) {
                succeed = true;
            }
            if (guessSchemaThroughResult(strBuff)) {
                succeed = true;
            }
        } finally {
            java.io.File outputFile = new java.io.File(tempFilePath);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            java.io.OutputStream outStream = null;
            java.io.OutputStreamWriter writer = null;
            try {
                outStream = new java.io.FileOutputStream(outputFile);
                writer = new java.io.OutputStreamWriter(outStream, encoding);
                writer.write(strBuff.toString());
                writer.flush();
            } finally {
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
        if (!succeed) {
            throw new Exception("guess schema failed");
        }
    }

    private boolean guessSchemaThroughAction(final StringBuffer strBuff) throws Exception {
        final java.util.concurrent.atomic.AtomicBoolean noSchemaAction =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Object schemaResult = null;
        try {
            schemaResult = componentManager.findPlugin(plugin).orElseThrow(() -> {
                noSchemaAction.set(true);
                throw new IllegalArgumentException("No component " + plugin);
            })
                    .get(org.talend.sdk.component.runtime.manager.ContainerComponentRegistry.class)
                    .getServices()
                    .stream()
                    .flatMap(s -> s.getActions().stream())
                    .filter(a -> a.getFamily().equals(family) && a.getAction().equals(action)
                            && a.getType().equals(type))
                    .findFirst()
                    .orElseThrow(() -> {
                        noSchemaAction.set(true);
                        throw new IllegalArgumentException("No action " + family + "#" + type + "#" + action);
                    })
                    .getInvoker()
                    .apply(configuration);
        } catch (Throwable e) {
            if (!noSchemaAction.get()) {
                // maybe just ignore it and continue to use the generic way
                log.error("Meet problem when calling built-in guess schema action.", e);
            }
        }
        if (schemaResult instanceof org.talend.sdk.component.api.service.schema.Schema) {
            org.talend.sdk.component.api.service.schema.Schema compSchema =
                    org.talend.sdk.component.api.service.schema.Schema.class.cast(schemaResult);
            java.util.Collection<org.talend.sdk.component.api.service.schema.Schema.Entry> entries =
                    compSchema.getEntries();
            if (entries != null && !entries.isEmpty()) {
                for (org.talend.sdk.component.api.service.schema.Schema.Entry entry : entries) {
                    String name = entry.getName();
                    org.talend.sdk.component.api.service.schema.Type entryType = entry.getType();
                    String typeName = "";
                    if (entryType == null) {
                        entryType = org.talend.sdk.component.api.service.schema.Type.STRING;
                    }
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
                    if (name == null) {
                        name = "";
                    }
                    if (typeName == null) {
                        typeName = "";
                    }
                    ColumnInfo ci = new ColumnInfo();
                    ci.setName(name);
                    ci.setType(typeName);
                    write(strBuff, ci);
                }
                return true;
            } else {
                log.info("No column found by guess schema action");
                return false;
            }
        } else {
            log.error("Result of built-in guess schema action is not an instance of TaCoKit Schema");
            return false;
        }
    }

    private boolean guessSchemaThroughResult(final StringBuffer strBuff) throws Exception {
        org.talend.sdk.component.runtime.input.Mapper mapper =
                componentManager.findMapper(plugin, componentName, 1, configuration).orElseThrow(
                        () -> new IllegalArgumentException("Can't find " + plugin + "#" + componentName));
        org.talend.sdk.component.runtime.input.Input input = null;
        try {
            mapper.start();
            input = mapper.create();
            input.start();
            Object rowObject = input.next();
            if (rowObject == null) {
                return false;
            }
            if (rowObject instanceof java.util.Map) {
                return guessSchemaThroughResultMap(strBuff, input, (java.util.Map) rowObject);
            } else if (rowObject instanceof java.util.Collections) {
                throw new Exception("Can't guess schema from a list");
            } else {
                return guessSchemaThroughResultClass(strBuff, rowObject);
            }
        } finally {
            try {
                if (input != null) {
                    input.stop();
                }
            } catch (Exception e) {
                log.error(e);
            }
            try {
                if (mapper != null) {
                    mapper.stop();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private boolean guessSchemaThroughResultClass(final StringBuffer strBuff, final Object rowObject) throws Exception {
        Class<? extends Object> rowClass = rowObject.getClass();
        java.util.Map<String, java.util.Set<String>> nameTypeMap = new java.util.HashMap<>();
        int i = 0;
        final int originalSize = strBuff.length();
        java.lang.reflect.Method[] methods = rowClass.getMethods();
        if (methods != null && 0 < methods.length) {
            for (java.lang.reflect.Method method : methods) {
                int modifiers = method.getModifiers();
                if (!java.lang.reflect.Modifier.isPublic(modifiers)) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == void.class) {
                    continue;
                }
                String name = method.getName();
                name = name.replaceFirst("(([Gg][Ee][Tt])|([Ii][Ss])|([Tt][Oo]))", "");
                String typeName = getTalendType(returnType);
                String key = name.toLowerCase();
                java.util.Set<String> mapTypes = nameTypeMap.get(key);
                if (mapTypes == null) {
                    mapTypes = new java.util.HashSet<>();
                    mapTypes.add(typeName);
                    nameTypeMap.put(key, mapTypes);
                } else {
                    if (mapTypes.contains(typeName)) {
                        continue;
                    } else {
                        mapTypes.add(typeName);
                        name = name + "_" + i++;
                    }
                }
                ColumnInfo ci = new ColumnInfo();
                ci.setName(name);
                ci.setType(typeName);
                write(strBuff, ci);
            }
        }
        java.lang.reflect.Field[] fields = rowClass.getFields();
        if (fields != null && 0 < fields.length) {
            for (java.lang.reflect.Field field : fields) {
                int modifiers = field.getModifiers();
                if (!java.lang.reflect.Modifier.isPublic(modifiers)) {
                    continue;
                }
                String name = field.getName();
                String typeName = getTalendType(field.getType());
                String key = name.toLowerCase();
                java.util.Set<String> mapTypes = nameTypeMap.get(key);
                if (mapTypes == null) {
                    mapTypes = new java.util.HashSet<>();
                    mapTypes.add(typeName);
                    nameTypeMap.put(key, mapTypes);
                } else {
                    if (mapTypes.contains(typeName)) {
                        continue;
                    } else {
                        mapTypes.add(typeName);
                        name = name + "_" + i++;
                    }
                }
                ColumnInfo ci = new ColumnInfo();
                ci.setName(name);
                ci.setType(typeName);
                write(strBuff, ci);
            }
        }
        return originalSize != strBuff.length();
    }

    private boolean guessSchemaThroughResultMap(final StringBuffer strBuff,
            final org.talend.sdk.component.runtime.input.Input input, final java.util.Map rowObject) throws Exception {
        java.util.Map<String, ColumnInfo> columnMap = new java.util.HashMap<>();
        java.util.Map<Object, Object> rowMap = rowObject;
        JavaType objectJavaType = javaTypesManager.OBJECT;
        final int limit = 50;
        for (int i = 0; i < limit; ++i) {
            boolean allConfirmed = true;
            for (java.util.Map.Entry entry : rowMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key == null) {
                    continue;
                }
                String name = key.toString();
                ColumnInfo storedCi = columnMap.get(name);
                if (storedCi != null) {
                    if (objectJavaType.getId().equals(storedCi.getName())) {
                        continue;
                    }
                } else {
                    storedCi = new ColumnInfo();
                    storedCi.setName(name);
                    columnMap.put(name, storedCi);
                }
                if (value == null) {
                    allConfirmed = false;
                    storedCi.setNullable(true);
                    continue;
                }
                final Class curValueType = value.getClass();
                String curType = getTalendType(curValueType);
                if (objectJavaType.getId().equals(curType)) {
                    storedCi.setType(curType);
                    continue;
                }
                String storedType = storedCi.getType();
                String commonType = javaDataTypeHelper.getCommonType(curType, storedType);
                storedCi.setType(commonType);
            }
            if (allConfirmed) {
                break;
            }
            rowMap = (java.util.Map<Object, Object>) input.next();
            if (rowMap == null) {
                break;
            }
        }
        if (columnMap.isEmpty()) {
            log.info("No column found from result map.");
            return false;
        }
        for (ColumnInfo ci : columnMap.values()) {
            write(strBuff, ci);
        }
        return true;
    }

    private void write(final StringBuffer strBuff, final ColumnInfo ci) {
        writeByCsvFormat(strBuff, ci);
    }

    private void writeByCsvFormat(final StringBuffer strBuff, final ColumnInfo ci) {
        strBuff.append(quotechar).append(ci.getName()).append(quotechar).append(separator);
        strBuff.append(quotechar).append(ci.getType()).append(quotechar).append(separator);
        strBuff.append(quotechar).append(ci.getScale()).append(quotechar).append(separator);
        strBuff.append(quotechar).append(ci.getPrecision()).append(quotechar).append(separator);
        strBuff.append(quotechar).append(ci.isNullable()).append(quotechar).append(separator);
        strBuff.append("\n");
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

    public void close() {
        try {
            // nothing to do
        } catch (Exception e) {
            log.error("Close failed: " + this.getClass().getName(), e);
        }
    }

    class ColumnInfo {

        private String name;

        private String type;

        private int scale = 0;

        private int precision = 0;

        private boolean nullable = true;

        public String getName() {
            return this.name;
        }

        public void setName(final String name) {
            this.name = name;
            if (this.name != null) {
                this.name = this.name.replaceAll("[\\W]", "_");
            }
        }

        public String getType() {
            return this.type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public void setType(final Class type) {
            this.type = getTalendType(type);
        }

        public int getScale() {
            return this.scale;
        }

        public void setScale(final int scale) {
            this.scale = scale;
        }

        public int getPrecision() {
            return this.precision;
        }

        public void setPrecision(final int precision) {
            this.precision = precision;
        }

        public boolean isNullable() {
            return this.nullable;
        }

        public void setNullable(final boolean nullable) {
            this.nullable = nullable;
        }

    }

    class JavaDataTypeHelper {

        /**
         * Constant to define the min and the max of type Int Float Double.
         */
        private static final int INT_MIN = -2147483648;

        private static final int INT_MAX = 2147483647;

        private static final float FLOAT_MIN = -1.4E-45f;

        private static final float FLOAT_MAX = 3.4E38f;

        private static final double DOUBLE_MIN = 4.9E-324d;

        private static final double DOUBLE_MAX = 1.7E308d;

        private final EJavaTypePriority STRING = new EJavaTypePriority("id_String", 10, false); //$NON-NLS-1$

        private final EJavaTypePriority BOOLEAN = new EJavaTypePriority("id_Boolean", 9, false); //$NON-NLS-1$

        private final EJavaTypePriority INTEGER = new EJavaTypePriority("id_Integer", 1, true); //$NON-NLS-1$

        private final EJavaTypePriority LONG = new EJavaTypePriority("id_Long", 2, true); //$NON-NLS-1$

        private final EJavaTypePriority CHARACTER = new EJavaTypePriority("id_Character", 1, false); //$NON-NLS-1$

        private final EJavaTypePriority DATE = new EJavaTypePriority("id_Date", 9, false); //$NON-NLS-1$

        private final EJavaTypePriority FLOAT = new EJavaTypePriority("id_Float", 3, true); //$NON-NLS-1$

        private final EJavaTypePriority DOUBLE = new EJavaTypePriority("id_Double", 4, true); //$NON-NLS-1$

        private final EJavaTypePriority BIGDECIMAL = new EJavaTypePriority("id_BigDecimal", 5, true); //$NON-NLS-1$

        private java.util.List<EJavaTypePriority> javaTypePriorityList;

        public JavaDataTypeHelper() {
            javaTypePriorityList = new java.util.ArrayList<>();
            javaTypePriorityList.add(BIGDECIMAL);
            javaTypePriorityList.add(BOOLEAN);
            javaTypePriorityList.add(CHARACTER);
            javaTypePriorityList.add(DATE);
            javaTypePriorityList.add(DOUBLE);
            javaTypePriorityList.add(INTEGER);
            javaTypePriorityList.add(FLOAT);
            javaTypePriorityList.add(LONG);
            javaTypePriorityList.add(STRING);
        }

        public String getTalendTypeOfValue(final String value) {
            String javaType = getJavaTypeOfValue(value);
            if (javaType == null) {
                return javaTypesManager.STRING.getId();
            }
            return javaType;
        }

        public String getJavaTypeOfValue(final String value) {

            // empty value => type is undef
            if (value.equals("")) { //$NON-NLS-1$
                return null;
            }

            // only 1 char => type is char or int
            if (value.length() == 1) {
                try {
                    // * int Entier -2 147 483 648 � 2 147 483 647
                    Integer nbr = Integer.parseInt(value);
                    if ((nbr >= INT_MIN) && (nbr <= INT_MAX)) {
                        return javaTypesManager.INTEGER.getId();
                    }
                } catch (Exception e) {
                    //
                }
                return javaTypesManager.CHARACTER.getId();
            }

            // see the bug "6680",qli comment.

            if (value.equals(javaTypesManager.DEFAULT_BOOLEAN) || value.equals(javaTypesManager.ANOTHER_BOOLEAN)) {
                return javaTypesManager.BOOLEAN.getId();
            }

            if (isDateOne(value) || isDateTwo(value)) {
                return javaTypesManager.DATE.getId();
            }

            // SPECIFIQUE USE CASE (integer begin by 0; multiple dot use ; use of char E in scientific notation)
            //
            if (!isNumber(value)) {
                // Warning : 1.7E38 is interpreted like a float !
                return javaTypesManager.STRING.getId();
            }
            //
            // first char is 0 and no dot just after => force type String needed
            if (value.substring(0, 1).equals("0") && (!value.substring(1, 2).equals("."))) { //$NON-NLS-1$ //$NON-NLS-2$
                return javaTypesManager.STRING.getId();
            }
            //
            // content more one dot => String
            if (value.contains(".")) { //$NON-NLS-1$
                if (value.substring(value.indexOf(".") + 1, value.length()).contains(".")) { //$NON-NLS-1$ //$NON-NLS-2$
                    return javaTypesManager.STRING.getId();
                }
            }
            // END SPECIFIQUE USE CASE

            // content only a dot => float or double
            if (value.contains(".")) { //$NON-NLS-1$
                try {
                    Float nbrFloat = Float.parseFloat(value);
                    if ((!nbrFloat.toString().equals("Infinity")) && (!nbrFloat.toString().equals("-Infinity"))) { //$NON-NLS-1$ //$NON-NLS-2$
                        if ((nbrFloat >= FLOAT_MIN) && (nbrFloat <= FLOAT_MAX)) {
                            return javaTypesManager.FLOAT.getId();
                        }
                    }

                    try {
                        // * double flottant double 4.9*10 -324 � 1.7*10 308
                        Double nbrDouble = Double.parseDouble(value);
                        if ((!nbrDouble.toString().equals("Infinity")) && (!nbrDouble.toString().equals("-Infinity"))) { //$NON-NLS-1$ //$NON-NLS-2$
                            if ((nbrDouble >= DOUBLE_MIN) && (nbrDouble <= DOUBLE_MAX)) {
                                return javaTypesManager.DOUBLE.getId();
                            }
                        }
                    } catch (Exception e) {
                        //
                    }

                } catch (Exception e) {
                    //
                }
            }

            // is not a char, not a float : parseMethod is usable
            try {
                Integer.parseInt(value);
                return javaTypesManager.INTEGER.getId();
            } catch (Exception e) {
                //
            }
            try {
                Long.parseLong(value);
                return javaTypesManager.LONG.getId();
            } catch (Exception e) {
                //
            }
            try {
                Double.parseDouble(value);
                return javaTypesManager.DOUBLE.getId();
            } catch (Exception e) {
                //
            }

            // by default the type is string
            return javaTypesManager.STRING.getId();
        }

        private boolean isDateOne(final String inputValue) {
            String value = inputValue;
            if (value != null) {
                try {
                    String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS Z";//$NON-NLS-1$
                    value = value.replace("Z", " UTC");//$NON-NLS-1$//$NON-NLS-2$
                    java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat(dateFormat);

                    java.util.Date dt = simpleDateFormat.parse(value);
                    simpleDateFormat.format(dt);
                    return true;
                } catch (java.text.ParseException pe) {
                    return false;
                }
            } else {
                return false;
            }
        }

        private boolean isDateTwo(final String inputValue) {
            String value = inputValue;
            if (value != null) {
                try {
                    String dateFormat = "yyyy-MM-dd";//$NON-NLS-1$
                    java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat(dateFormat);

                    java.util.Date dt = simpleDateFormat.parse(value);
                    simpleDateFormat.format(dt);
                    return true;
                } catch (java.text.ParseException pe) {
                    return false;
                }
            } else {
                return false;
            }
        }

        public String getCommonType(final String type1, final String type2) {
            return getTypeByPriority(type1, type2);
        }

        private JavaType getCommonType(final String type1, final String type2, final JavaType testType) {
            if ((type1 == testType.getLabel()) || (type2 == testType.getLabel()) || (type1 == testType.getId())
                    || (type2 == testType.getId())) {
                return testType;
            }
            return null;
        }

        public boolean isNumber(final String value) {

            String strValidRealPattern = "^([-]|[.]|[-.]|[0-9])[0-9]*[.]*[0-9]+$"; //$NON-NLS-1$
            String strValidIntegerPattern = "^([-]|[0-9])[0-9]*$"; //$NON-NLS-1$
            String regex = "(" + strValidRealPattern + ")|(" + strValidIntegerPattern + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            try {
                return java.util.regex.Pattern.matches(regex, value);
            } catch (Exception e) {
                // do nothing
            }
            return false;
        }

        public String getTypeByPriority(final String type1, final String type2) {
            EJavaTypePriority priority1 = getJavaTypePriority(type1);
            EJavaTypePriority priority2 = getJavaTypePriority(type2);
            if (priority1.isNumberType() != priority2.isNumberType()) {
                return STRING.getIdName();
            }
            if (priority1.getPriority() > priority2.getPriority()) {
                return priority1.getIdName();
            } else {
                return priority2.getIdName();
            }
        }

        private EJavaTypePriority getJavaTypePriority(final String idName) {
            for (EJavaTypePriority type : javaTypePriorityList) {
                if (type.getIdName().equals(idName)) {
                    return type;
                }
            }
            return STRING;
        }

        class EJavaTypePriority {

            private String idName;

            private int priority;

            private boolean numberType;

            public EJavaTypePriority(final String name, final int priority, final boolean numberType) {
                this.idName = name;
                this.priority = priority;
                this.numberType = numberType;
            }

            public String getIdName() {
                return this.idName;
            }

            public void setIdName(final String idName) {
                this.idName = idName;
            }

            public int getPriority() {
                return this.priority;
            }

            public void setPriority(final int priority) {
                this.priority = priority;
            }

            public boolean isNumberType() {
                return this.numberType;
            }

            public void setNumberType(final boolean numberType) {
                this.numberType = numberType;
            }
        };

    }

    class JavaType {

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

        private String createId(final String value) {
            return "id_" + value; //$NON-NLS-1$
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

    class JavaTypesManager {

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

        private java.util.Map<String, JavaType> shortNameToJavaType;

        private java.util.Map<String, JavaType> canonicalClassNameToJavaType;

        private java.util.Map<String, JavaType> labelToJavaType;

        private java.util.Map<String, JavaType> idToJavaType;

        private java.util.List<JavaType> javaTypes;

        private final java.util.List<String> JAVA_PRIMITIVE_TYPES = new java.util.ArrayList<String>();

        public JavaTypesManager() {
            init();
        }

        private final java.util.Set<String> PRIMITIVE_TYPES_SET = new java.util.HashSet<String>(JAVA_PRIMITIVE_TYPES);

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

}
