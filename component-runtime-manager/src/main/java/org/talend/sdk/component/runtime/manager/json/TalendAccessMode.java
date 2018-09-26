/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.json;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;

// javabean makes the name of a property starting with 2 uppercase not matching the field name
// in the studio it leads to duplicates so cleaning these kind of names
public class TalendAccessMode extends FieldAndMethodAccessMode {

    public TalendAccessMode() {
        super(true, true, false);
    }

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        final Map<String, Reader> reader = super.doFindReaders(clazz);
        return reader.entrySet().stream().filter(it -> {
            final String key = it.getKey();
            if (isBrokenJavaBeanName(key)) {
                final String propName = Character.toLowerCase(key.charAt(0)) + key.substring(1);
                return !reader.containsKey(propName); // if the field is here we drop the method
            }
            return true;
        }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isBrokenJavaBeanName(final String key) {
        return key.length() >= 2 && Character.isUpperCase(key.charAt(0)) && Character.isUpperCase(key.charAt(1));
    }
}
