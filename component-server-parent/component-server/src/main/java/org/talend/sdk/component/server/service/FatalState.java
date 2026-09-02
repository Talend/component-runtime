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
package org.talend.sdk.component.server.service;

import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

/**
 * Singleton that records whether the JVM has encountered a fatal error (VirtualMachineError)
 * during request processing. Once set, the state is never cleared — the pod must be restarted.
 */
@Slf4j
@ApplicationScoped
public class FatalState {

    private final AtomicReference<String> fatalCause = new AtomicReference<>(null);

    /**
     * Records a fatal error. Subsequent calls are no-ops; the first cause wins.
     *
     * @param cause human-readable description of the error
     */
    public void markFatal(final String cause) {
        if (fatalCause.compareAndSet(null, cause)) {
            log.error("Fatal JVM error recorded — liveness probe will now return DOWN: {}", cause);
        }
    }

    /**
     * @return {@code true} if a fatal error has been recorded
     */
    public boolean hasFatalError() {
        return fatalCause.get() != null;
    }

    /**
     * @return the cause of the fatal error, or {@code null} if none has been recorded
     */
    public String getCause() {
        return fatalCause.get();
    }

    /**
     * Resets the fatal state. Intended for use in tests only — never call in production code.
     */
    public void reset() {
        fatalCause.set(null);
    }
}
