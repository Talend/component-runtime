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
package org.talend.sdk.component.tcketl.app.palette;

import java.lang.System.Logger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.async.Cancellable;
import org.talend.sdk.component.tcketl.app.session.Problems;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.server.ComponentCatalog;

/**
 * The component index only gives a provisional role (a sink is reported as a processor), the real one follows from
 * the flows of the definition. This loads the definitions in the background, in small batches and a couple at a
 * time so that the other calls of the application are not held back, and reports the roles as the batches arrive.
 * The definitions end up in the catalog cache, which is where the canvas finds them later.
 *
 * <p>
 * {@link #resolve} MUST be called on the UI thread, which is the thread the results are delivered on.
 */
public final class RoleResolver {

    static final int BATCH_SIZE = 10;

    static final int CONCURRENCY = 2;

    private static final Logger LOGGER = System.getLogger(RoleResolver.class.getName());

    private final AsyncRunner async;

    public RoleResolver(final AsyncRunner async) {
        this.async = async;
    }

    /**
     * A batch that fails ends the work of its lane (a server that does not answer is not asked for every remaining
     * batch); the roles not reported stay provisional.
     */
    public Cancellable resolve(final ComponentCatalog catalog, final Collection<ConnectorId> ids,
            final Consumer<Map<ConnectorId, ConnectorRole>> onBatch) {
        final Run run = new Run(catalog, batches(ids), onBatch);
        for (int lane = 0; lane < CONCURRENCY; lane++) {
            run.next();
        }
        return run;
    }

    private static Deque<List<ConnectorId>> batches(final Collection<ConnectorId> ids) {
        final Deque<List<ConnectorId>> batches = new ArrayDeque<>();
        final List<ConnectorId> all = List.copyOf(ids);
        for (int from = 0; from < all.size(); from += BATCH_SIZE) {
            batches.add(all.subList(from, Math.min(all.size(), from + BATCH_SIZE)));
        }
        return batches;
    }

    private final class Run implements Cancellable {

        private final ComponentCatalog catalog;

        private final Deque<List<ConnectorId>> queue;

        private final Consumer<Map<ConnectorId, ConnectorRole>> onBatch;

        private final List<Cancellable> requests = new ArrayList<>();

        private boolean cancelled;

        private Run(final ComponentCatalog catalog, final Deque<List<ConnectorId>> queue,
                final Consumer<Map<ConnectorId, ConnectorRole>> onBatch) {
            this.catalog = catalog;
            this.queue = queue;
            this.onBatch = onBatch;
        }

        private void next() {
            if (cancelled || queue.isEmpty()) {
                return;
            }
            final List<ConnectorId> batch = queue.poll();
            requests.add(async.submit(() -> catalog.connectors(batch), definitions -> {
                final Map<ConnectorId, ConnectorRole> roles = new HashMap<>();
                definitions.forEach((id, definition) -> roles.put(id, definition.role()));
                onBatch.accept(roles);
                next();
            }, failure -> LOGGER.log(Logger.Level.WARNING,
                    "Roles not resolved for " + batch.size() + " connectors: " + Problems.describe(failure))));
        }

        @Override
        public void cancel() {
            cancelled = true;
            requests.forEach(Cancellable::cancel);
            queue.clear();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
