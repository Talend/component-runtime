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

public class DynamicColumnsHelper {

    /**
     * Checks if coloumn is marked as a dynamic columns holder
     *
     * @param column to check
     *
     * @return true if column's name contains marker
     */
    public static boolean isDynamicColumn(final String column) {
        return column != null && column.endsWith(DynamicColumns.DYNAMIC_COLUMN_MARKER);
    }

    /**
     * Checks if one column of the provided collection is marked as a dynamic columns holder
     *
     * @param columns to check
     *
     * @return true if one column is marked as a dynamic columns holder
     */
    public static boolean hasDynamicColumn(final Collection<? extends Object> columns) {
        return columns != null && columns.stream().map(String::valueOf).anyMatch(DynamicColumnsHelper::isDynamicColumn);
    }

    /**
     * Returns a column name w/o the dynamic marker if present
     *
     * @param column to be removed from its marker
     *
     * @return a column name w/o the dynamic marker if present
     */
    public static String getRealColumnName(final String column) {
        return column == null ? null : column.replace(DynamicColumns.DYNAMIC_COLUMN_MARKER, "");
    }

    /**
     * Returns the column name of the provided collection if it is marked as a dynamic columns holder
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
                        .filter(DynamicColumnsHelper::isDynamicColumn)
                        .map(DynamicColumnsHelper::getRealColumnName)
                        .findFirst()
                        .orElse(null);
    }

}
