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
import java.util.Optional;
import java.util.function.IntFunction;

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.form.FormLayout;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.ButtonCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Cell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.CollectionCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.FieldCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.GroupCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Row;
import org.talend.sdk.component.tcketl.domain.form.PropertyKind;
import org.talend.sdk.component.tcketl.domain.form.PropertyNode;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;

/**
 * Walks the layout of a form and builds it with a {@link FormToolkit}: tabs, rows, conditional display, groups,
 * editors, buttons that call services and the editors of lists. It is the same for every UI technology.
 *
 * @param <N> the type of a widget of the technology.
 */
public final class FormAssembler<N> {

    private final FormViewModel model;

    private final FormBindings bindings;

    private final ActionService service;

    private final FormToolkit<N> toolkit;

    public FormAssembler(final FormViewModel model, final FormBindings bindings, final ActionService service,
            final FormToolkit<N> toolkit) {
        this.model = model;
        this.bindings = bindings;
        this.service = service;
        this.toolkit = toolkit;
    }

    /**
     * Where a cell is built: the concrete path of the object around it, and the position of the entry when it is in a
     * list or a map.
     */
    private record Context(String parent, int index, boolean compact) {

        static final Context ROOT = new Context(null, -1, false);

        Context inside(final String parentPath) {
            return new Context(parentPath, index, false);
        }
    }

    public N build() {
        final List<FormLayout.Tab> tabs = model.layout().tabs();
        if (tabs.isEmpty() || tabs.stream().allMatch(tab -> tab.rows().isEmpty())) {
            return toolkit.empty("Nothing to configure");
        }
        if (!model.layout().isTabbed()) {
            return toolkit.scroll(rows(tabs.get(0).rows(), Context.ROOT, bindings.root()));
        }
        return toolkit
                .tabs(tabs.stream().map(FormLayout.Tab::name).toList(),
                        index -> toolkit.scroll(rows(tabs.get(index).rows(), Context.ROOT, bindings.root())));
    }

    private N rows(final List<Row> rows, final Context context, final FormBindings.Scope scope) {
        final List<List<N>> built = new ArrayList<>();
        for (final Row row : rows) {
            final List<N> cells = new ArrayList<>();
            for (final Cell cell : row.cells()) {
                cells.add(cell(cell, context, scope));
            }
            if (!cells.isEmpty()) {
                built.add(cells);
            }
        }
        return toolkit.rows(built);
    }

    private N cell(final Cell cell, final Context context, final FormBindings.Scope scope) {
        if (cell instanceof FieldCell field) {
            return conditional(field(field.node(), context, scope), pathOf(field.node(), context), scope);
        } else if (cell instanceof GroupCell group) {
            return conditional(group(group, context, scope), pathOf(group.node(), context), scope);
        } else if (cell instanceof CollectionCell collection) {
            return conditional(collection(collection, context, scope), pathOf(collection.node(), context), scope);
        } else if (cell instanceof ButtonCell button) {
            return button(button, context);
        }
        throw new IllegalArgumentException("Unknown cell " + cell);
    }

    private N conditional(final N node, final String path, final FormBindings.Scope scope) {
        scope.onRefresh(() -> toolkit.visible(node, model.isVisible(path)));
        return node;
    }

    /** A button is laid out inside its owner, so the context already points at the owner's concrete path. */
    private static String buttonPath(final ButtonCell cell, final Context context) {
        final PropertyNode owner = cell.owner();
        final PropertyNode target = cell.target();
        if (context.parent() == null || !target.path().startsWith(owner.path())) {
            return pathOf(target, context);
        }
        return context.parent() + target.path().substring(owner.path().length());
    }

    private static String pathOf(final PropertyNode node, final Context context) {
        final String parent = context.parent() != null ? context.parent()
                : node.parent().map(PropertyNode::path).orElse(null);
        return node.concretePath(parent, context.index());
    }

    private N field(final PropertyNode node, final Context context, final FormBindings.Scope scope) {
        final String path = pathOf(node, context);
        return toolkit
                .field(FieldSpec.of(node, path, context.index()), scalarKind(node), context.compact(), scope,
                        () -> validateRemotely(node, path));
    }

    /** An asynchronous validation runs when the user leaves the field; a KO shows the comment until the next edit. */
    private void validateRemotely(final PropertyNode node, final String path) {
        final Optional<ActionBinding> binding = node.definition().action(ActionTypes.VALIDATION);
        if (binding.isEmpty() || !service.supports(binding.get())) {
            return;
        }
        service
                .request(binding.get(), path)
                .ifPresent(request -> service.run(path + '#' + ActionTypes.VALIDATION, request,
                        outcome -> model.setRemoteMessage(path,
                                outcome instanceof ActionOutcome.Checked checked && !checked.ok()
                                        ? (checked.comment().isEmpty() ? "Rejected by the server" : checked.comment())
                                        : null)));
    }

    private static WidgetKind scalarKind(final PropertyNode node) {
        final WidgetKind kind = WidgetSelector.select(node);
        return switch (kind) {
            case GROUP, LIST_OF_OBJECTS, LIST_OF_VALUES, MAP -> WidgetKind.TEXT;
            default -> kind;
        };
    }

    private N group(final GroupCell group, final Context context, final FormBindings.Scope scope) {
        final PropertyNode node = group.node();
        final String path = pathOf(node, context);
        final N content = rows(group.rows(), context.inside(path), scope);
        if (node.parent().isEmpty() || !node.definition().configurationType().isPresent()) {
            return toolkit.group(null, content);
        }
        return toolkit.group(Texts.title(node, context.index()), content);
    }

    private N button(final ButtonCell cell, final Context context) {
        final String text = switch (cell.kind()) {
            case HEALTHCHECK -> "Check connection";
            case UPDATE -> "Update";
            case GUESS_SCHEMA -> "Guess schema";
        };
        if (!service.supports(cell.binding())) {
            return toolkit
                    .button(text, cell.kind() != FormLayout.ButtonKind.GUESS_SCHEMA,
                            "The server has no such service for this connector", result -> {
                            });
        }
        final String path = buttonPath(cell, context);
        final String key = path + '#' + cell.binding().type();
        return toolkit
                .button(text, true, null,
                        result -> service.request(cell.binding(), path).ifPresent(request -> {
                            result.show(Feedback.RUNNING);
                            service.run(key, request, outcome -> result.show(Feedback.apply(outcome, path, model)));
                        }));
    }

    private N collection(final CollectionCell cell, final Context context, final FormBindings.Scope scope) {
        final PropertyNode node = cell.node();
        final String path = pathOf(node, context);
        final boolean readOnly = node.definition().ui().readOnly();
        final PropertyKind kind = node.kind();
        final FormToolkit.ListEditor<N> editor = toolkit
                .collection(Texts.title(node, context.index()),
                        Texts.numbered(node.definition().documentation(), context.index()), readOnly,
                        () -> model.append(path));

        final int[] shown = { -1 };
        final FormBindings.Scope[] current = { null };
        scope.onRefresh(() -> {
            editor.message(model.message(path).orElse(null));
            final int count = model.count(path);
            if (count == shown[0]) {
                return;
            }
            shown[0] = count;
            if (current[0] != null) {
                current[0].close();
            }
            final FormBindings.Scope entryScope = scope.child();
            current[0] = entryScope;
            final IntFunction<N> factory = switch (kind) {
                case MAP -> index -> mapEntry(cell, path, index, entryScope);
                case ARRAY_OF_OBJECT -> index -> objectEntry(cell, path, index, entryScope);
                default -> index -> valueEntry(node, path, index, entryScope);
            };
            final List<FormToolkit.Entry<N>> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                entries.add(entry(kind, factory.apply(i), path, i, count, readOnly, node));
            }
            editor.entries(entries);
        });
        return editor.node();
    }

    private FormToolkit.Entry<N> entry(final PropertyKind kind, final N body, final String path, final int index,
            final int count, final boolean readOnly, final PropertyNode node) {
        String header = null;
        if (kind == PropertyKind.ARRAY_OF_OBJECT) {
            final String display = node.definition().displayName();
            header = display.contains("${index}") ? Texts.numbered(display, index) : display + " #" + (index + 1);
        }
        return new FormToolkit.Entry<>(body, header, kind != PropertyKind.MAP, !readOnly && index > 0,
                !readOnly && index < count - 1, !readOnly, () -> model.move(path, index, index - 1),
                () -> model.move(path, index, index + 1), () -> model.remove(path, index));
    }

    private N valueEntry(final PropertyNode list, final String path, final int index, final FormBindings.Scope scope) {
        final Optional<PropertyNode> element = list.element();
        if (element.isEmpty()) {
            return toolkit.empty("?");
        }
        final PropertyNode node = element.get();
        return toolkit
                .field(FieldSpec.of(node, node.concretePath(path, index), index), scalarKind(node), true, scope,
                        () -> {
                        });
    }

    private N objectEntry(final CollectionCell cell, final String path, final int index,
            final FormBindings.Scope scope) {
        final PropertyNode element = cell.node().element().orElse(cell.node());
        return rows(cell.elementRows(), new Context(element.concretePath(path, index), index, false), scope);
    }

    private N mapEntry(final CollectionCell cell, final String path, final int index, final FormBindings.Scope scope) {
        return rows(cell.elementRows(), new Context(path, index, true), scope);
    }
}
