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

/** Work that can be abandoned: once cancelled, its outcome is never delivered. */
public interface Cancellable {

    /** Something to hold before any work has been started. */
    Cancellable NONE = new Cancellable() {

        @Override
        public void cancel() {
            // nothing to abandon
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    /** Abandons the work; a blocking call that is already running is interrupted. Safe to call more than once. */
    void cancel();

    boolean isCancelled();
}
