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

import java.util.function.Consumer;

import org.talend.sdk.component.runtime.input.Input;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ChainedInput implements Input {

    private final ChainedMapper parent;

    private Input delegate = null;

    @Override
    public Object next() {
        while (true) {
            if (delegate == null) {
                return null;
            }
            final Object next = delegate.next();
            if (next != null) {
                return next;
            }
            delegate.stop();
            delegate = null;
        }
    }

    @Override
    public String plugin() {
        return parent.plugin();
    }

    @Override
    public String rootName() {
        return parent.rootName();
    }

    @Override
    public String name() {
        return parent.name();
    }

    @Override
    public void start() {
        initDelegate();
        delegate.start();
    }

    public void start(final Consumer<Object> checkpoint) {
        initDelegate();
        delegate.start(checkpoint);
    }

    /**
     * @return
     */
    @Override
    public Object getCheckpoint() {
        return delegate.getCheckpoint();
    }

    /**
     * @return
     */
    @Override
    public Boolean isCheckpointReady() {
        return delegate.isCheckpointReady();
    }

    @Override
    public void stop() {
        if (delegate != null) {
            delegate.stop();
        }
    }

    private void initDelegate() {
        delegate = parent.getIterator().hasNext() ? parent.getIterator().next().create() : null;
    }
}
