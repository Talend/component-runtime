/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.record.dynamic;

import java.util.Collection;

import org.talend.sdk.component.api.record.Schema;

public class DynamicHelper {

    /**
     * Marker used for tagging a dynamic column's name
     */
    public static final String DYNAMIC_MARKER = "___TCK_DYNAMIC_COLUMN___";

    /**
     * Checks if coloumn is marked as a dynamic columns holder
     *
     * @param column to check
     *
     * @return true if column's name contains marker
     */
    public static boolean isDynamicColumn(final String column) {
        return column != null && column.endsWith(DYNAMIC_MARKER);
    }

    /**
     * Checks if one column of the provided collection is marked as a dynamic columns holder
     *
     * @param columns to check
     *
     * @return true if one column is marked as a dynamic columns holder
     */
    public static boolean hasDynamicColumn(final Collection<? extends Object> columns) {
        return columns != null && columns.stream().map(String::valueOf).anyMatch(DynamicHelper::isDynamicColumn);
    }

    /**
     * Returns a column name w/o the dynamic marker if present
     *
     * @param column to be removed from its marker
     *
     * @return a column name w/o the dynamic marker if present
     */
    public static String getRealColumnName(final String column) {
        return column == null ? null
                : column.endsWith(DYNAMIC_MARKER) ? column.substring(0, column.length() - DYNAMIC_MARKER.length())
                        : column;
    }

    /**
     * Returns the original column name of the provided collection if it is marked as a dynamic columns holder
     *
     * @param columns to check
     *
     * @return the real column name (w/o the marker) otherwise returns null
     */
    public static String getDynamicRealColumnName(final Collection<? extends Object> columns) {
        return columns == null ? null
                : columns
                        .stream()
                        .map(String::valueOf)
                        .filter(DynamicHelper::isDynamicColumn)
                        .map(DynamicHelper::getRealColumnName)
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Returns the column name of the provided collection if it is marked as a dynamic columns holder
     *
     * @param columns to check
     *
     * @return the column name (w/ the marker) otherwise returns null
     */
    public static String getDynamicColumnName(final Collection<? extends Object> columns) {
        return columns == null ? null
                : columns.stream().map(String::valueOf).filter(DynamicHelper::isDynamicColumn).findFirst().orElse(null);
    }

    /**
     * Returns the entry name of the provided schema if it is marked as a dynamic columns holder
     *
     * @param schema to check
     *
     * @return the column name (w/ the marker) otherwise returns null
     */
    public static String getDynamicColumnName(final Schema schema) {
        return schema == null ? null
                : schema
                        .getEntries()
                        .stream()
                        .map(entry -> entry.getName())
                        .filter(DynamicHelper::isDynamicColumn)
                        .findFirst()
                        .orElse(null);
    }

}
