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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * The widgets of one UI technology. {@link FormAssembler} decides what a form contains and how it behaves; a toolkit
 * only creates and arranges the widgets, and binds the editable ones to the model. A toolkit lives on the UI thread
 * of its form.
 *
 * @param <N> the type of a widget of the technology.
 */
public interface FormToolkit<N> {

    /** The content of a form that has nothing to configure. */
    N empty(String text);

    /** Tabs; {@code content} builds the content of a tab, when it is first shown. The first tab is built at once. */
    N tabs(List<String> names, IntFunction<N> content);

    /** A scrollable area around the content of a tab or of a form without tabs. */
    N scroll(N content);

    /** Rows of cells, laid out in as many equal columns as a row has cells. */
    N rows(List<List<N>> rows);

    /** Shows or hides a widget; a hidden widget takes no room. */
    void visible(N node, boolean visible);

    /** A titled section; the title is {@code null} for an inline group. */
    N group(String title, N content);

    /**
     * An editor of a scalar property, with its title, its message and its documentation.
     *
     * @param spec what it edits.
     * @param kind the kind of editor, never a group, a list or a map.
     * @param compact whether it is an entry of a list or a map: no title, only the message.
     * @param scope where the editor registers what it refreshes.
     * @param onBlur to run when the user leaves the editor.
     */
    N field(FieldSpec spec, WidgetKind kind, boolean compact, FormBindings.Scope scope, Runnable onBlur);

    /**
     * A button that calls a service.
     *
     * @param text its label.
     * @param visible whether it is shown at all.
     * @param disabledReason why it cannot be used, or {@code null} when it can.
     * @param onClick called on a click; it reports the progress and the result to the given button.
     */
    N button(String text, boolean visible, String disabledReason, Consumer<ButtonResult> onClick);

    /** An editor of a list or a map, with its title, an add button and its message. */
    ListEditor<N> collection(String title, String documentation, boolean readOnly, Runnable onAdd);

    /** What a button shows about its call. */
    interface ButtonResult {

        /** Shows the feedback; a running call disables the button, any other state enables it again. */
        void show(Feedback feedback);
    }

    /** The entries of a list or of a map. */
    interface ListEditor<N> {

        N node();

        void message(String textOrNull);

        void entries(List<Entry<N>> entries);
    }

    /**
     * One entry of a collection.
     *
     * @param body its editor.
     * @param header the title of an entry made of several fields, or {@code null} for a single value.
     * @param reorderable whether the entry can be moved.
     * @param canUp whether it can move up.
     * @param canDown whether it can move down.
     * @param canRemove whether it can be removed.
     * @param up moves it up.
     * @param down moves it down.
     * @param remove removes it.
     */
    record Entry<N>(
            N body,
            String header,
            boolean reorderable,
            boolean canUp,
            boolean canDown,
            boolean canRemove,
            Runnable up,
            Runnable down,
            Runnable remove
    ) {
    }
}
