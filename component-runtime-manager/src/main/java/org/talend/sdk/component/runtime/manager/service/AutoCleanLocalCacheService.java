/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Local cache with cleaner thread that remove elements to be removed.
 * 
 * @param <T> : cached elements class.
 */
public class AutoCleanLocalCacheService<T> extends LocalCacheService<T> {

    public interface Scheduler {

        void start(Runnable r);

        void stop();
    }

    public static class ThreadScheduler implements Scheduler {

        // scheduler we use to evict tokens
        private volatile ScheduledExecutorService threadService;

        // the eviction task if active
        private volatile ScheduledFuture<?> task;

        public ThreadScheduler() {
            threadService = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
                final Thread thread = new Thread(r, this.getClass().getName() + "-eviction-" + hashCode());
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            });
        }

        @Override
        public void start(Runnable r) {
            task = this.buildTask(this.threadService, r);
        }

        @Override
        public void stop() {
            if (task != null) {
                task.cancel(true);
                threadService.shutdownNow();
                try {
                    threadService.awaitTermination(2, SECONDS); // not a big deal if it does not end completely here
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                task = null;
            }
        }

        protected ScheduledFuture<?> buildTask(ScheduledExecutorService ses, Runnable r) {
            return ses.scheduleAtFixedRate(r, 30, 30, SECONDS);
        }
    }

    // evition planner
    private volatile Scheduler scheduler;

    // here to avoid to stop eviction while creating a value.
    private final AtomicInteger pendingRequests = new AtomicInteger();

    public AutoCleanLocalCacheService(String plugin) {
        this(plugin, new ThreadScheduler());
    }

    public AutoCleanLocalCacheService(String plugin, Scheduler scheduler) {
        super(plugin);
        this.scheduler = scheduler;
    }

    @Override
    public T computeIfAbsent(String key, Predicate<T> toRemove, Supplier<T> value) {
        pendingRequests.incrementAndGet();
        try {
            this.scheduler.start(this::evict);// startEviction();
            return super.computeIfAbsent(key, toRemove, value);
        } finally {
            pendingRequests.decrementAndGet();
        }
    }

    private void evict() {
        this.clean();
        if (this.isEmpty() && pendingRequests.get() == 0) { // try to stop the useless thread
            synchronized (this) {
                if (this.isEmpty() && pendingRequests.get() == 0) {
                    this.scheduler.stop();
                }
            }
        }
    }
}
