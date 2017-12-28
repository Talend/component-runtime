/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.lang;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class LocalLockTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    @Test
    public void run() throws IOException, InterruptedException {
        final File lockFile = TEMPORARY_FOLDER.newFile(testName.getMethodName());
        { // ensure it works "empty"
            final Lock lock1 = new LocalLock(lockFile, null);
            lock1.lock();
            lock1.unlock();
        }
        { // now ensure it works concurrently
            final Lock lock1 = new LocalLock(lockFile, null);
            final Lock lock2 = new LocalLock(lockFile, null);
            lock1.lock();

            final CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                lock2.lock();
                latch.countDown();
            }).start();
            assertFalse(latch.await(3, SECONDS));
            lock1.unlock();
            assertTrue(latch.await(1, SECONDS));
            lock2.unlock();
            // ensure we can call unlock N times
            IntStream.range(0, 5).forEach(i -> lock2.unlock());
        }
    }
}
