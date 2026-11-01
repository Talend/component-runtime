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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/** Keeps the tasks until the test runs them, which makes the interleaving of the callers explicit. */
public final class ManualExecutor implements Executor {

    private final Deque<Runnable> queue = new ArrayDeque<>();

    @Override
    public void execute(final Runnable task) {
        queue.add(task);
    }

    /** Number of tasks waiting. */
    public int pending() {
        return queue.size();
    }

    /** Runs the oldest task, if there is one. */
    public boolean runNext() {
        final Runnable task = queue.poll();
        if (task == null) {
            return false;
        }
        task.run();
        return true;
    }
}
