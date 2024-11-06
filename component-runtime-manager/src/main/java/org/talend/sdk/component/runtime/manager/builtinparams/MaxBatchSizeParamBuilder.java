/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.builtinparams;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;

import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;

import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

public class MaxBatchSizeParamBuilder {

    private final String name = "$maxBatchSize";

    private final Integer defaultValue;

    private final ParameterMeta root;

    private final String layoutType;

    public MaxBatchSizeParamBuilder(final ParameterMeta root, final String componentSimpleClassName,
            final LocalConfiguration configuration) {
        this.root = root;
        this.layoutType = getLayoutType(root);
        this.defaultValue = !isActive(componentSimpleClassName, configuration) ? -1
                : Integer
                        .parseInt(ofNullable(configuration.get(componentSimpleClassName + "._maxBatchSize.value"))
                                .orElseGet(() -> ofNullable(configuration.get("_maxBatchSize.value")).orElse("1000"))
                                .trim());
    }

    private boolean isActive(final String componentSimpleClassName, final LocalConfiguration configuration) {
        return Boolean
                .parseBoolean(ofNullable(configuration.get(componentSimpleClassName + "._maxBatchSize.active"))
                        .orElseGet(() -> ofNullable(configuration.get("_maxBatchSize.active")).orElse("true"))
                        .trim());
    }

    public ParameterMeta newBulkParameter() {
        return defaultValue <= -3 ? null : new ParameterMeta(new ParameterMeta.Source() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public Class<?> declaringClass() {
                return MaxBatchSizeParamBuilder.class;
            }
        }, Integer.class, ParameterMeta.Type.NUMBER, root.getPath() + "." + name, name,
                concat(Stream.of(MaxBatchSizeParamBuilder.class.getPackage().getName()),
                        Stream.of(ofNullable(root.getI18nPackages()).orElse(new String[0]))).toArray(String[]::new),
                emptyList(), emptyList(), new HashMap<String, String>() {

                    {
                        put("tcomp::ui::defaultvalue::value", String.valueOf(defaultValue));
                        put("tcomp::validation::min", "-3");
                    }
                }, true);
    }

    private String getLayoutType(final ParameterMeta root) {
        if (root == null) {
            return "tcomp::ui::gridlayout::Advanced::value";
        }

        final String rootLayoutType = root
                .getMetadata()
                .keySet()
                .stream()
                .filter(k -> k.contains("tcomp::ui") && (k.contains("layout") || k.contains("optionsorder")))
                .map(k -> k.split("::"))
                .filter(s -> s.length > 2)
                .map(s -> s[2])
                .findFirst()
                .orElse("default");
        switch (rootLayoutType.toLowerCase(Locale.ROOT)) {
        case "verticallayout":
            return "tcomp::ui::verticallayout";
        case "horizontallayout":
            return "tcomp::ui::horizontallayout";
        case "autolayout":
            return "tcomp::ui::autolayout";
        case "optionsorder":
            return "tcomp::ui::optionsorder";
        case "default":
            return null;
        case "gridlayout":
        default:
            return "tcomp::ui::gridlayout::Advanced::value";
        }
    }

    public String getLayoutType() {
        return layoutType;
    }
}
