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
package org.talend.sdk.component.runtime.beam.coder;

import static java.util.Collections.emptyList;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.UnboundedSource;

public class NoCheckpointCoder extends Coder<UnboundedSource.CheckpointMark> {

    @Override
    public void encode(final UnboundedSource.CheckpointMark value, final OutputStream outStream) {
        // no-op
    }

    @Override
    public UnboundedSource.CheckpointMark decode(final InputStream inStream) {
        return UnboundedSource.CheckpointMark.NOOP_CHECKPOINT_MARK;
    }

    @Override
    public List<? extends Coder<?>> getCoderArguments() {
        return emptyList();
    }

    @Override
    public void verifyDeterministic() {
        // no-op
    }

    @Override
    public int hashCode() {
        return NoCheckpointCoder.class.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return NoCheckpointCoder.class.isInstance(obj);
    }
}
