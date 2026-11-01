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
package org.talend.sdk.component.tcketl.app.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.app.testing.ManualExecutor;

class AsyncRunnerTest {

    private final ManualExecutor io = new ManualExecutor();

    private final ManualExecutor ui = new ManualExecutor();

    private final AsyncRunner runner = AsyncRunner.of(io, ui);

    private final List<Object> events = new ArrayList<>();

    @Test
    void worksOnTheIoExecutorAndDeliversOnTheUiExecutor() {
        runner.submit(() -> {
            events.add("work");
            return "result";
        }, result -> events.add("success " + result), failure -> events.add("failure"));
        assertEquals(List.of(), events);

        io.runNext();
        assertEquals(List.of("work"), events);

        ui.runNext();
        assertEquals(List.of("work", "success result"), events);
    }

    @Test
    void reportsAFailureOfTheWork() {
        final IllegalStateException failure = new IllegalStateException("boom");
        runner.submit(() -> {
            throw failure;
        }, result -> events.add("success"), events::add);
        io.runNext();
        ui.runNext();
        assertEquals(List.of(failure), events);
    }

    @Test
    void reportsAMissingClassLikeAnyOtherFailure() {
        final NoClassDefFoundError failure = new NoClassDefFoundError("com/github/weisj/jsvg/SVGDocument");
        runner.submit(() -> {
            throw failure;
        }, result -> events.add("success"), events::add);
        io.runNext();
        ui.runNext();
        assertEquals(List.of(failure), events);
    }

    @Test
    void dropsTheOutcomeOfWorkCancelledWhileItWasDelivered() {
        final Cancellable handle = runner.submit(() -> "result", result -> events.add("success"), events::add);
        io.runNext();
        handle.cancel();
        ui.runNext();
        assertEquals(List.of(), events);
        assertTrue(handle.isCancelled());
    }

    @Test
    void doesNotStartWorkCancelledBeforeItRan() {
        final Cancellable handle = runner.submit(() -> {
            events.add("work");
            return "result";
        }, result -> events.add("success"), events::add);
        handle.cancel();
        io.runNext();
        assertEquals(0, ui.pending());
        assertEquals(List.of(), events);
    }

    @Test
    void submittingToAnExecutorThatWasShutDownIsHarmless() {
        final AsyncRunner closed = AsyncRunner.of(task -> {
            throw new RejectedExecutionException("shut down");
        }, ui);
        assertTrue(closed.submit(() -> "result", result -> events.add("success"), events::add).isCancelled());
        assertEquals(List.of(), events);
    }
}
