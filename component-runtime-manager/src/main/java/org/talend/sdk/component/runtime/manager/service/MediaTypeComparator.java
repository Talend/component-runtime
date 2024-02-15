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
package org.talend.sdk.component.runtime.manager.service;

import java.util.Comparator;
import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MediaTypeComparator implements Comparator<String> {

    private final List<String> keys;

    @Override
    public int compare(final String mediaType1, final String mediaType2) {
        // -- same types
        if (mediaType1.equals(mediaType2)) {
            return 0;
        }

        // -- only one of type contains wildcard
        if (mediaType1.contains("*") && !mediaType2.contains("*")) {
            return 1;
        } else if (!mediaType1.contains("*") && mediaType2.contains("*")) {
            return -1;
        }

        // -- both types d'ont have wildcard -> check subtypes
        if (!mediaType1.contains("*") && !mediaType2.contains("*")) {
            final String[] types1 = mediaType1.split("/");
            final String[] types2 = mediaType2.split("/");
            String subType1 = types1[1].contains("+") ? types1[1].split("\\+")[0] : null;
            String subType2 = types2[1].contains("+") ? types2[1].split("\\+")[0] : null;
            if (subType1 != null && subType2 == null) {
                return 1;
            } else if (subType1 == null && subType2 != null) {
                return -1;
            }
            return keys.indexOf(mediaType1) - keys.indexOf(mediaType2);
        }

        // -- both types have wildcards
        final String[] types1 = mediaType1.split("/");
        final String[] types2 = mediaType2.split("/");
        if (types1[0].contains("*") && !types2[0].contains("*")) {
            return 1;
        } else if (!types1[0].contains("*") && types2[0].contains("*")) {
            return -1;
        } else {// compare sub types
            String subType1 = types1[1].contains("+") ? types1[1].split("\\+")[0] : null;
            String subType2 = types2[1].contains("+") ? types2[1].split("\\+")[0] : null;
            if ("*".equals(subType1) && !"*".equals(subType2)) {
                return 1;
            } else if (!"*".equals(subType1) && "*".equals(subType2)) {
                return -1;
            } else if (subType1 != null && subType2 != null) {
                return keys.indexOf(mediaType1) - keys.indexOf(mediaType2);
            } else {
                // no subtypes or both are *
                if ("*".equals(types1[1]) && !"*".equals(types2[1])) {
                    return 1;
                } else if (!"*".equals(types1[1]) && "*".equals(types2[1])) {
                    return -1;
                }

                return keys.indexOf(mediaType1) - keys.indexOf(mediaType2);
            }
        }

    }
}
