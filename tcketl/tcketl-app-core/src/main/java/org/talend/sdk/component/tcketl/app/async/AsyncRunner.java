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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Runs blocking work (the component server calls) away from the UI thread and hands the outcome back on it. The two
 * executors are injected so that the logic built on top of it can be tested without a toolkit: with
 * {@link #direct()} everything runs on the calling thread.
 */
public final class AsyncRunner implements AutoCloseable {

    private static final int IO_THREADS = 4;

    private final Executor io;

    private final Executor ui;

    private final Runnable onClose;

    private AsyncRunner(final Executor io, final Executor ui, final Runnable onClose) {
        this.io = io;
        this.ui = ui;
        this.onClose = onClose;
    }

    /** A pool of daemon threads for the calls; the outcomes are handed to {@code ui}, the thread owning the screen. */
    public static AsyncRunner production(final Executor ui) {
        final AtomicInteger counter = new AtomicInteger();
        final ExecutorService pool = Executors.newFixedThreadPool(IO_THREADS, task -> {
            final Thread thread = new Thread(task, "tcketl-io-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        return new AsyncRunner(pool, ui, pool::shutdownNow);
    }

    public static AsyncRunner of(final Executor io, final Executor ui) {
        return new AsyncRunner(io, ui, () -> {
        });
    }

    /** Everything runs on the calling thread, immediately. */
    public static AsyncRunner direct() {
        return of(Runnable::run, Runnable::run);
    }

    /**
     * Runs {@code work} on the I/O executor, then {@code onSuccess} or {@code onFailure} on the UI executor, unless the
     * returned handle was cancelled in the meantime.
     */
    public <T> Cancellable submit(final Callable<? extends T> work, final Consumer<? super T> onSuccess,
            final Consumer<? super Throwable> onFailure) {
        final Handle handle = new Handle();
        final FutureTask<Void> task = new FutureTask<>(() -> {
            if (handle.isCancelled()) {
                return null;
            }
            try {
                final T result = work.call();
                post(handle, () -> onSuccess.accept(result));
            } catch (final Exception | LinkageError e) {
                post(handle, () -> onFailure.accept(e));
            }
            return null;
        });
        handle.task = task;
        try {
            io.execute(task);
        } catch (final RejectedExecutionException e) {
            handle.cancel();
        }
        return handle;
    }

    @Override
    public void close() {
        onClose.run();
    }

    private void post(final Handle handle, final Runnable delivery) {
        try {
            ui.execute(() -> {
                if (!handle.isCancelled()) {
                    delivery.run();
                }
            });
        } catch (final RejectedExecutionException e) {
            handle.cancel();
        }
    }

    private static final class Handle implements Cancellable {

        private volatile boolean cancelled;

        private volatile FutureTask<Void> task;

        @Override
        public void cancel() {
            cancelled = true;
            final FutureTask<Void> running = task;
            if (running != null) {
                running.cancel(true);
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
