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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.service.AutoCleanLocalCacheService.Scheduler;
import org.talend.sdk.component.runtime.manager.service.AutoCleanLocalCacheService.ThreadScheduler;

import mockit.Mock;
import mockit.MockUp;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class AutoCleanLocalCacheServiceTest {

    @Test
    public void test() throws InterruptedException {

        final Scheduler scheduler = new ThreadScheduler() {

            protected ScheduledFuture<?> buildTask(ScheduledExecutorService ses, Runnable r) {
                return ses.scheduleAtFixedRate(r, 30, 30, MILLISECONDS);
            }
        };

        AutoCleanLocalCacheService<String> cache = new AutoCleanLocalCacheService<>("tests", scheduler);
        final boolean[] toRemove = new boolean[] { false, false, false };

        Assertions.assertEquals("chaine0", cache.computeIfAbsent("s0", (String s) -> toRemove[0], () -> "chaine0"));
        Assertions.assertEquals("chaine1", cache.computeIfAbsent("s1", (String s) -> toRemove[1], () -> "chaine1"));
        Assertions.assertEquals("chaine2", cache.computeIfAbsent("s2", (String s) -> toRemove[2], () -> "chaine2"));

        String res = cache.computeIfAbsent("s0", (String s) -> toRemove[0], () -> "chaineNext");
        Assertions.assertEquals("chaine0", res);

        toRemove[0] = true;
        // AutoCleanLocalCacheServiceTest.delay = 40_000L;

        Assertions
                .assertEquals("chaineNext", cache.computeIfAbsent("s0", (String s) -> toRemove[0], () -> "chaineNext"));

        toRemove[0] = true;
        toRemove[1] = true;
        toRemove[2] = true;

        Thread.sleep(450L);
        // Assertions.assertEquals("chaineN0",
        // cache.computeIfAbsent("s0", (String s) -> toRemove[0], () -> "chaineN0"));
        Thread.sleep(35L);
    }

}