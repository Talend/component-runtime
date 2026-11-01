/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.app.testing;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;

/** An {@link AsyncRunner} whose blocking work and whose deliveries only happen when the test says so. */
public final class TestAsync {

    private final ManualExecutor io = new ManualExecutor();

    private final ManualExecutor ui = new ManualExecutor();

    private final AsyncRunner runner = AsyncRunner.of(io, ui);

    public AsyncRunner runner() {
        return runner;
    }

    /** The blocking work that was submitted and did not run yet. */
    public ManualExecutor io() {
        return io;
    }

    /** The outcomes that are waiting to be delivered. */
    public ManualExecutor ui() {
        return ui;
    }

    /** Runs the work and delivers the outcomes, including the work they trigger, until nothing is left. */
    public void drain() {
        boolean progress = true;
        while (progress) {
            progress = io.runNext() || ui.runNext();
        }
    }
}
