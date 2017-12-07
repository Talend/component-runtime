/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.parameter;

import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_ADVANCED;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_GRIDLAYOUT_MAIN;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.OBJECT;

import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.NoArgsConstructor;

/**
 * Provides methods, which extend {@link SimplePropertyDefinition} functionality
 * TODO Try to use SimplePropertyDefinitionDecorator instead
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SimplePropertyDefinitionUtils {

    private static final String PATH_SEPARATOR = ".";

    private static final String NO_PARENT_ID = "";

    /**
     * Suffix used in id ({@link SimplePropertyDefinition#getPath()}), which denotes Array typed property
     * (which is Table property in Studio)
     */
    private static final String ARRAY_PATH = "[]";

    public static String getParentPath(final SimplePropertyDefinition property) {
        String path = property.getPath();
        if (!path.contains(PATH_SEPARATOR)) {
            return NO_PARENT_ID;
        }
        String parentPath = path.substring(0, path.lastIndexOf("."));
        // following is true, when parent has type=ARRAY
        if (parentPath.endsWith(ARRAY_PATH)) {
            parentPath = parentPath.substring(0, parentPath.lastIndexOf("[]"));
        }
        return parentPath;
    }

    public static boolean isObject(final SimplePropertyDefinition property) {
        return OBJECT.equals(property.getType());
    }

    /**
     * Checks whether specified {@link SimplePropertyDefinition} has {@link Metadatas#UI_GRIDLAYOUT_MAIN} metadata
     * 
     * @param property
     * @return true, is contains main gridlayout metadata
     */
    public static boolean hasMainGridLayout(final SimplePropertyDefinition property) {
        return property.getMetadata().containsKey(UI_GRIDLAYOUT_MAIN);
    }

    /**
     * Checks whether specified {@link SimplePropertyDefinition} has {@link Metadatas#UI_GRIDLAYOUT_ADVANCED} metadata
     * 
     * @param property
     * @return true, is contains advanced gridlayout metadata
     */
    public static boolean hasAdvancedGridLayout(final SimplePropertyDefinition property) {
        return property.getMetadata().containsKey(UI_GRIDLAYOUT_ADVANCED);
    }
}
