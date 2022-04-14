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
package org.talend.sdk.component.runtime.record;

import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaRelationChecker implements Serializable {

    /**
     * Check record schemas
     * Equal or includes
     * 
     * @param outer the one that may include another
     * @param inner the one that test for inclusion
     * @return true outer equals to inner or outer has wider schema then inner
     */
    public static boolean include(final Schema outer, final Schema inner) {
        if (outer == null || inner == null || outer.getType() != Type.RECORD || inner.getType() != Type.RECORD) {
            return false;
        }

        final Set<String> outerNames = outer.getAllEntries().map(Entry::getName).collect(toSet());
        final Set<String> innerNames = inner.getAllEntries().map(Entry::getName).collect(toSet());
        if (!outerNames.containsAll(innerNames)) {
            // inner has more names => outer isn't include it
            return false;
        }

        return inner.getAllEntries().allMatch(innerEntry -> {
            final Entry outerEntry = outer.getEntry(innerEntry.getName());
            if (Objects.equals(innerEntry, outerEntry)) {
                return true;
            } else if (Objects.equals(innerEntry.getType(), outerEntry.getType())
                    && Objects.equals(innerEntry.getElementSchema(), outerEntry.getElementSchema())) {
                // strange but ok
                log.warn("Schema entries are different but their schema are the same. Name: {}", innerEntry.getName());
                return true;
            } else {
                // names ok, but let's check types
                final Schema outerElementSchema = outerEntry.getElementSchema();
                return innerEntry.getElementSchema()
                        .getAllEntries()
                        .allMatch(in -> {
                            final Entry outEntry = outerElementSchema.getEntry(in.getName());
                            return Objects.equals(in, outEntry)
                                    || include(outerEntry.getElementSchema(), in.getElementSchema());
                        });
            }
        });
    }
}
