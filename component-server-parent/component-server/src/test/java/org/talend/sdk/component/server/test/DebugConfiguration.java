/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.test;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.meecrowave.testing.MonoBase;

public class DebugConfiguration implements MonoBase.Configuration {

    private ScheduledExecutorService executor;

    private ScheduledFuture<?> logTask;

    @Override
    public void beforeStarts() {
        if (Boolean.getBoolean("talend.component.server.enableDumps")) {
            executor = Executors
                    .newSingleThreadScheduledExecutor(r -> new Thread(r, DebugConfiguration.class.getSimpleName()));
            logTask = executor.scheduleWithFixedDelay(this::log, 1, 1, TimeUnit.MINUTES);
        }
    }

    @Override
    public void afterStops() {
        if (logTask != null) {
            logTask.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void log() {
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Debug dump:");
        Stream.of(ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)).forEach(info -> {
            System.out.println(info.getThreadName() + ':');
            Stream.of(info.getStackTrace()).forEach(elt -> System.out.println("\tat " + elt));
        });
        System.out.println("-----------------------------------------------------------------");
    }
}
