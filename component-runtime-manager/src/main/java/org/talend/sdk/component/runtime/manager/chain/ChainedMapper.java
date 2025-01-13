/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.chain;

import java.util.Iterator;
import java.util.List;

import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ChainedMapper implements Mapper {

    private final Mapper root;

    private final Iterator<Mapper> iterator;

    @Override
    public long assess() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Mapper> split(final long desiredSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Input create() {
        return new ChainedInput(this);
    }

    @Override
    public boolean isStream() {
        return false;
    }

    @Override
    public String plugin() {
        return root.plugin();
    }

    @Override
    public String rootName() {
        return root.rootName();
    }

    @Override
    public String name() {
        return root.name();
    }

    @Override
    public void start() {
        // no-op: already done for the split
    }

    @Override
    public void stop() {
        // no-op: must be handled outside this
    }

    Iterator<Mapper> getIterator() {
        return iterator;
    }
}
