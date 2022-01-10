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
package org.talend.sdk.component.runtime.manager.json;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;

public class TalendAccessMode extends FieldAndMethodAccessMode {

    private final AccessMode fields = new FieldAccessMode(false, false);

    public TalendAccessMode() {
        super(true, true, false);
    }

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        if (Stream.of(clazz.getInterfaces()).anyMatch(it -> it.getName().startsWith("routines.system."))) {
            final Map<String, Reader> readers = fields.findReaders(clazz);
            if (!readers.isEmpty()) {
                return readers;
            } // else let's try the methods
        }
        return super.doFindReaders(clazz);
    }
}
