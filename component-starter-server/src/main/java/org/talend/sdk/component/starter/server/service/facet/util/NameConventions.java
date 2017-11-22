/*
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *   <p>
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   <p>
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.talend.sdk.component.starter.server.service.facet.util;

import java.util.function.BiConsumer;

import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;

import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.capitalize;

public final class NameConventions {

    private NameConventions() {
    }

    public static String toJavaName(final String name) {
        return capitalize(name.replace("-", "_").replace(" ", "_"));
    }

    public static String sanitizeConnectionName(final String name) {
        return "MAIN".equals(name) ? "__default__" : name.replace("_", "").replace("#", "").replace(" ", "");
    }

    public static String toJavaConfigType(final String root, final String pack, final ProjectRequest.Entry entry,
            final BiConsumer<String, ProjectRequest.DataStructure> nestedGenerator) {
        final String type = entry.getType();
        if (type == null || type.isEmpty()) {
            if (entry.getNestedType() != null) {
                final String name = (root == null ? "" : root) + capitalize(entry.getName()) + "Configuration";
                nestedGenerator.accept(pack + '.' + name, entry.getNestedType());
                return name;
            }
            return "String";
        }
        switch (type.toLowerCase(ENGLISH)) {
        case "boolean":
            return "boolean";
        case "double":
            return "double";
        case "int":
        case "integer":
            return "int";
        case "uri": // todo: import
            return "java.net.URI";
        case "url": // todo: import
            return "java.net.URL";
        case "file": // todo: import
            return "java.io.File";
        case "string":
        default:
            return "String";
        }
    }
}
