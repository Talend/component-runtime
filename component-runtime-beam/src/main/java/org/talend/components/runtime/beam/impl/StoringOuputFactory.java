/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.runtime.beam.impl;

import java.util.ArrayList;
import java.util.List;

import org.talend.component.api.processor.OutputEmitter;
import org.talend.components.runtime.output.Branches;
import org.talend.components.runtime.output.OutputFactory;

import lombok.Data;
import lombok.Getter;

@Data
class StoringOuputFactory implements OutputFactory {

    @Getter
    private List<Object> values;

    @Override
    public OutputEmitter create(final String name) {
        return value -> {
            if (!Branches.DEFAULT_BRANCH.equals(name)) {
                throw new IllegalArgumentException("Branch " + name + " unsupported for now");
            }
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add(value);
        };
    }
}
