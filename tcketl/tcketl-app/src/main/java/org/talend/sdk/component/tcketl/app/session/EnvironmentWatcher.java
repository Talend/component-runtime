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
package org.talend.sdk.component.tcketl.app.session;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/**
 * Notices that the connected server deployed or removed components since the palette was loaded: {@link #poll()} asks
 * the server for its status and compares the stamp with the one the session kept. Whoever owns a timer calls
 * {@link #poll()} periodically; an unreachable server is not a change. Everything runs on the UI thread.
 */
public final class EnvironmentWatcher {

    private final ServerSession session;

    private final AsyncRunner async;

    private final ReadOnlyBooleanWrapper changed = new ReadOnlyBooleanWrapper();

    private boolean polling;

    public EnvironmentWatcher(final ServerSession session, final AsyncRunner async) {
        this.session = session;
        this.async = async;
        session.serverStatus().addListener((observable, before, now) -> changed.set(false));
        session.connection().addListener((observable, before, now) -> changed.set(false));
    }

    /** True when the server has changed since the palette was loaded: the palette should be refreshed. */
    public ReadOnlyBooleanProperty changed() {
        return changed.getReadOnlyProperty();
    }

    public void poll() {
        final ServerConnection connection = session.connection().get();
        final ServerStatus known = session.serverStatus().get();
        if (polling || connection == null || known == null) {
            return;
        }
        polling = true;
        async.<ServerStatus> submit(() -> connection.catalog().status(), status -> {
            polling = false;
            if (session.connection().get() == connection && !status.stamp().equals(known.stamp())) {
                changed.set(true);
            }
        }, failure -> polling = false);
    }
}
