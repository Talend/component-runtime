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

import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_LENGTH;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_PATTERN;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_PRECISION;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Convert TCK Schema to Map of Colums for studio and vice-et-versa.
 */
@Slf4j
@RequiredArgsConstructor
public class Converter {

    private static final String STRING_ESCAPE = "\"";

    private final JavaTypesManager javaTypesManager;

    /**
     * From TCK Schema to Studio.
     * 
     * @param schema : tck schema.
     * @return studio.
     */
    public Map<String, Column> toColumns(final Schema schema) {
        if (schema == null || schema.getType() != Schema.Type.RECORD) {
            return null;
        }
        final Collection<Schema.Entry> entries = schema.getEntries();
        if (entries == null || entries.isEmpty()) {
            log.info("No column found by guess schema action");
            return null;
        }

        final Map<String, Column> columns = new LinkedHashMap<>();
        for (Schema.Entry entry : entries) {
            final Column column = this.entryToColumn(entry);
            columns.put(column.getLabel(), column);
        }
        return columns;
    }

    /**
     * From studio columns to TCK Schema.
     * 
     * @param factory : TCK Schema/Record factory.
     * @param columns : Studio columns.
     * @return TCK Schema.
     */
    public Schema toSchema(
            final RecordBuilderFactory factory,
            final Map<String, Column> columns) {
        final Schema.Builder builder = factory.newSchemaBuilder(Schema.Type.RECORD);
        columns.values()
                .stream()
                .map((Column c) -> this.columnToEntry(factory, c))
                .forEach(builder::withEntry);
        return builder.build();
    }

    private Column entryToColumn(final Schema.Entry entry) {
        final Column column = new Column();
        column.setLabel(entry.getName());
        column.setOriginalDbColumnName(entry.getOriginalFieldName());
        column.setNullable(entry.isNullable());
        column.setComment(entry.getComment());

        Schema.Type entryType = entry.getType();
        if (entryType == null) {
            entryType = Schema.Type.STRING;
        }

        switch (entryType) {
        case BOOLEAN:
            column.setTalendType(javaTypesManager.BOOLEAN.getId());
            break;
        case DOUBLE:
            column.setTalendType(javaTypesManager.DOUBLE.getId());
            this.setIntValue(column::setLength, entry.getProp(STUDIO_LENGTH));
            this.setIntValue(column::setPrecision, entry.getProp(STUDIO_PRECISION));
            break;
        case INT:
            column.setTalendType(javaTypesManager.INTEGER.getId());
            break;
        case LONG:
            column.setTalendType(javaTypesManager.LONG.getId());
            break;
        case FLOAT:
            column.setTalendType(javaTypesManager.FLOAT.getId());
            this.setIntValue(column::setLength, entry.getProp(STUDIO_LENGTH));
            this.setIntValue(column::setPrecision, entry.getProp(STUDIO_PRECISION));
            break;
        case BYTES:
            column.setTalendType(javaTypesManager.BYTE_ARRAY.getId());
            break;
        case DATETIME:
            column.setTalendType(javaTypesManager.DATE.getId());
            String pattern = entry.getProp(STUDIO_PATTERN);
            if (pattern != null) {
                column.setPattern(STRING_ESCAPE + pattern + STRING_ESCAPE);
            } else {
                // studio default pattern
                column.setPattern(STRING_ESCAPE + "dd-MM-yyyy" + STRING_ESCAPE);
            }
            break;
        case RECORD:
            column.setTalendType(javaTypesManager.OBJECT.getId());
            break;
        case ARRAY:
            column.setTalendType(javaTypesManager.LIST.getId());
            break;
        default:
            column.setTalendType(javaTypesManager.STRING.getId());
            break;
        }

        if (entry.getDefaultValue() != null) {
            try {
                column.setDefault(entry.getDefaultValue().toString());
            } catch (Exception e) {
                // nevermind as it's almost useless...
            }
        }
        return column;
    }

    private void setIntValue(final Consumer<Integer> setter, final String value) {
        try {
            setter.accept(Integer.valueOf(value));
        } catch (NumberFormatException e) {
            // let default values if props are trash...
        }
    }

    private Schema.Entry columnToEntry(final RecordBuilderFactory factory,
            final Column column) {
        Schema.Entry.Builder entryBuilder = factory.newEntryBuilder();
        entryBuilder.withName(column.getLabel())
                .withNullable(column.getNullable())
                .withComment(column.getComment());
        String talendType = column.getTalendType();
        if (javaTypesManager.BOOLEAN.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.BOOLEAN);
        } else if (javaTypesManager.DOUBLE.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.DOUBLE);
            entryBuilder.withProp(STUDIO_LENGTH, column.getLength().toString());
            entryBuilder.withProp(STUDIO_PRECISION, column.getPrecision().toString());
        } else if (javaTypesManager.INTEGER.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.INT);
        } else if (javaTypesManager.LONG.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.LONG);
        } else if (javaTypesManager.FLOAT.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.FLOAT);
            entryBuilder.withProp(STUDIO_LENGTH, column.getLength().toString());
            entryBuilder.withProp(STUDIO_PRECISION, column.getPrecision().toString());
        } else if (javaTypesManager.BYTE_ARRAY.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.BYTES);
        } else if (javaTypesManager.DATE.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.DATETIME);
            String pattern = column.getPattern();
            if (pattern != null && pattern.startsWith("\"") && pattern.endsWith("\"")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            }
            entryBuilder.withProp(STUDIO_PATTERN, pattern);
        } else if (javaTypesManager.OBJECT.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.RECORD);
        } else if (javaTypesManager.LIST.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.ARRAY);
        } else if (javaTypesManager.STRING.getId().equals(talendType)) {
            entryBuilder.withType(Schema.Type.STRING);
        }
        return entryBuilder.build();
    }

}
