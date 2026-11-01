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
package org.talend.sdk.component.tcketl.forms;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;

/**
 * The refreshers of the controls of one form. They all run, in registration order, when the revision of the view model
 * changes, so that a parent is refreshed before the controls it created. A {@link Scope} groups the refreshers of a
 * part of the form that can be rebuilt, such as the entries of a list: closing it unregisters them.
 */
final class FormBindings implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(FormBindings.class.getName());

    private final FormViewModel model;

    private final ChangeListener<Number> listener = (value, before, after) -> refresh();

    private final List<Entry> entries = new ArrayList<>();

    private final Scope root = new Scope(null);

    private boolean closed;

    FormBindings(final FormViewModel model) {
        this.model = model;
        model.revisionProperty().addListener(listener);
    }

    Scope root() {
        return root;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            model.revisionProperty().removeListener(listener);
            entries.clear();
        }
    }

    private void refresh() {
        for (final Entry entry : List.copyOf(entries)) {
            if (entry.scope.isClosed()) {
                continue;
            }
            try {
                entry.refresher.run();
            } catch (final RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "A form control could not be refreshed", e);
            }
        }
        entries.removeIf(entry -> entry.scope.isClosed());
    }

    private record Entry(Scope scope, Runnable refresher) {
    }

    final class Scope {

        private final Scope parent;

        private boolean closed;

        private Scope(final Scope parent) {
            this.parent = parent;
        }

        /** Registers a refresher and runs it once now. */
        void onRefresh(final Runnable refresher) {
            if (closed || FormBindings.this.closed) {
                return;
            }
            entries.add(new Entry(this, refresher));
            refresher.run();
        }

        Scope child() {
            return new Scope(this);
        }

        void close() {
            closed = true;
        }

        boolean isClosed() {
            return closed || (parent != null && parent.isClosed());
        }
    }
}
