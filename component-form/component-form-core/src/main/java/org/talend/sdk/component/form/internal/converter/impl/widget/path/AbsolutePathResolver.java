/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.converter.impl.widget.path;

import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

public class AbsolutePathResolver {

    // ensure it is aligned with org.talend.sdk.component.studio.model.parameter.SettingsCreator.computeTargetPath()
    public String resolveProperty(final String propPath, final String paramRef) {
        return doResolveProperty(propPath, normalizeParamRef(paramRef));
    }

    private String normalizeParamRef(final String paramRef) {
        return (!paramRef.contains(".") ? "../" : "") + paramRef;
    }

    private String doResolveProperty(final String propPath, final String paramRef) {
        if (".".equals(paramRef)) {
            return propPath;
        }
        if (paramRef.startsWith("..")) {
            String current = propPath;
            String ref = paramRef;
            while (ref.startsWith("..")) {
                int lastDot = current.lastIndexOf('.');
                if (lastDot < 0) {
                    lastDot = 0;
                }
                current = current.substring(0, lastDot);
                ref = ref.substring("..".length(), ref.length());
                if (ref.startsWith("/")) {
                    ref = ref.substring(1);
                }
                if (current.isEmpty()) {
                    break;
                }
            }
            return Stream.of(current, ref.replace('/', '.')).filter(it -> !it.isEmpty()).collect(joining("."));
        }
        if (paramRef.startsWith(".") || paramRef.startsWith("./")) {
            return propPath + '.' + paramRef.replaceFirst("\\./?", "").replace('/', '.');
        }
        return paramRef;
    }
}
