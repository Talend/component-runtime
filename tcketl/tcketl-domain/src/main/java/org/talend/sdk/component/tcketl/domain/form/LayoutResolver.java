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
package org.talend.sdk.component.tcketl.domain.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.ButtonCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.ButtonKind;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Cell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.CollectionCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.FieldCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.GroupCell;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Row;
import org.talend.sdk.component.tcketl.domain.form.FormLayout.Tab;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.UiHints.StructureDirection;

/**
 * Arranges the properties of a connector into the tabs, rows and cells of a form.
 *
 * <p>
 * An object lays out its children in <em>panes</em>, the named grids ({@code ui::gridlayout::<name>}) the server
 * declares: {@code "login,password|healthcheckOk"} is two rows, the first with two cells. The form is made of
 * <em>tabs</em>, which are the pane names found on the root object and on the objects nested in it, {@code Main}
 * first; a tab shows, for every object, the pane that has its name.
 *
 * <p>
 * Where the layout is silent, the engine does not lose properties:
 * <ul>
 * <li>an object lacking the pane of a tab shows its other panes there, merged, for whatever is not already shown in
 * a previous tab;</li>
 * <li>an object without any grid shows its children in the order of {@code ui::optionsorder}, then by path;</li>
 * <li>a child that no grid mentions is added to the first pane of its object (see
 * {@link FormOptions#appendUnlistedToFirstPane()});</li>
 * <li>the technical options of the root ({@code $maxRecords}...) that no grid mentions go to an {@code Advanced}
 * pane, created when needed.</li>
 * </ul>
 * The runtime state ({@code Checkpoint} pane, {@code $checkpoint} options) and hidden properties are never laid out.
 */
public final class LayoutResolver {

    private static final String MAIN = "Main";

    private static final String ADVANCED = "Advanced";

    private static final String CHECKPOINT = "Checkpoint";

    private static final String TECHNICAL_PREFIX = "$";

    private static final String DEFAULT_SCHEMA_ACTION = "default";

    private LayoutResolver() {
        // static helpers only
    }

    public static FormLayout resolve(final PropertyTree tree) {
        return resolve(tree, FormOptions.DEFAULT);
    }

    public static FormLayout resolve(final PropertyTree tree, final FormOptions options) {
        Preconditions.requireNonNull(tree, "tree");
        Preconditions.requireNonNull(options, "options");
        return new Build(tree, options).resolve();
    }

    /** One pane of an object: rows of child names. The rows are mutable while the panes are being computed. */
    private record Pane(String name, List<List<String>> rows) {
    }

    private record Panes(List<Pane> list, boolean explicit) {
    }

    private record ShownButton(PropertyNode owner, ButtonKind kind) {
    }

    private static final class Build {

        private final PropertyTree tree;

        private final FormOptions options;

        private final Map<PropertyNode, Panes> panes = new IdentityHashMap<>();

        /** The fields and collections already laid out in a tab, so that each one appears once in the form. */
        private final Set<PropertyNode> placed = newScope();

        private final Set<ShownButton> buttons = new HashSet<>();

        private Build(final PropertyTree tree, final FormOptions options) {
            this.tree = tree;
            this.options = options;
        }

        private FormLayout resolve() {
            final List<Tab> tabs = new ArrayList<>();
            for (final String name : tabNames()) {
                final List<Row> rows = new ArrayList<>();
                for (final PropertyNode root : tree.roots()) {
                    rows.addAll(rootRows(root, name));
                }
                if (!rows.isEmpty()) {
                    tabs.add(new Tab(name, rows));
                }
            }
            return new FormLayout(tabs);
        }

        private List<Row> rootRows(final PropertyNode root, final String tab) {
            if (isExcluded(root)) {
                return List.of();
            }
            if (root.kind() == PropertyKind.OBJECT) {
                return objectRows(root, tab, placed);
            }
            if (!MAIN.equalsIgnoreCase(tab)) {
                return List.of();
            }
            return placeNode(root, tab, placed).map(cell -> List.of(new Row(List.of(cell)))).orElse(List.of());
        }

        private List<String> tabNames() {
            final Map<String, String> names = new LinkedHashMap<>();
            tree.roots().forEach(root -> collectNames(root, names));
            final List<String> ordered = new ArrayList<>(names.values());
            ordered.stream().filter(MAIN::equalsIgnoreCase).findFirst().ifPresent(main -> {
                ordered.remove(main);
                ordered.add(0, main);
            });
            return ordered;
        }

        /** Only the root and the objects that declare grids name tabs; collections are laid out as a whole. */
        private void collectNames(final PropertyNode node, final Map<String, String> names) {
            final boolean root = node.parent().isEmpty();
            if (isExcluded(node)) {
                return;
            }
            if (node.kind() != PropertyKind.OBJECT) {
                if (root) {
                    names.putIfAbsent(MAIN.toLowerCase(Locale.ROOT), MAIN);
                }
                return;
            }
            final Panes own = panes(node);
            if (root || own.explicit()) {
                own.list().forEach(pane -> names.putIfAbsent(pane.name().toLowerCase(Locale.ROOT), pane.name()));
            }
            node.children().forEach(child -> collectNames(child, names));
        }

        private Panes panes(final PropertyNode container) {
            return panes.computeIfAbsent(container, this::computePanes);
        }

        private Panes computePanes(final PropertyNode container) {
            final boolean root = container.parent().isEmpty();
            final List<Pane> result = new ArrayList<>();
            for (final GridLayout grid : container.layout().grids()) {
                if (!CHECKPOINT.equalsIgnoreCase(grid.name()) && findPane(result, grid.name()).isEmpty()) {
                    result.add(new Pane(grid.name(), new ArrayList<>(grid.rows())));
                }
            }
            result.sort(Comparator.comparing(pane -> !MAIN.equalsIgnoreCase(pane.name())));
            final boolean explicit = !result.isEmpty();

            final List<PropertyNode> candidates =
                    container.children().stream().filter(child -> !isExcluded(child)).toList();
            if (!explicit) {
                final List<List<String>> rows = new ArrayList<>();
                implicitOrder(container, candidates, root).forEach(name -> rows.add(List.of(name)));
                if (!rows.isEmpty()) {
                    result.add(new Pane(MAIN, rows));
                }
            }

            final Set<String> listed = new HashSet<>();
            result.forEach(pane -> pane.rows().forEach(listed::addAll));
            final List<String> unlisted = new ArrayList<>();
            final List<String> technical = new ArrayList<>();
            for (final PropertyNode child : candidates) {
                if (listed.contains(child.name())) {
                    continue;
                }
                if (root && isTechnical(child)) {
                    technical.add(child.name());
                } else {
                    unlisted.add(child.name());
                }
            }
            if (options.appendUnlistedToFirstPane() && !result.isEmpty()) {
                unlisted.forEach(name -> result.get(0).rows().add(List.of(name)));
            }
            if (!technical.isEmpty()) {
                final Pane advanced = findPane(result, ADVANCED).orElseGet(() -> {
                    final Pane created = new Pane(ADVANCED, new ArrayList<>());
                    result.add(created);
                    return created;
                });
                technical.forEach(name -> advanced.rows().add(List.of(name)));
            }
            return new Panes(result, explicit);
        }

        /** The children of an object without grid: the declared options order, then the rest by path. */
        private static List<String> implicitOrder(final PropertyNode container, final List<PropertyNode> candidates,
                final boolean root) {
            final List<PropertyNode> usable =
                    candidates.stream().filter(child -> !(root && isTechnical(child))).toList();
            final List<String> names = new ArrayList<>();
            for (final String name : container.layout().optionsOrder()) {
                if (!names.contains(name) && usable.stream().anyMatch(child -> child.name().equals(name))) {
                    names.add(name);
                }
            }
            usable
                    .stream()
                    .sorted(Comparator.comparing(PropertyNode::path))
                    .map(PropertyNode::name)
                    .filter(name -> !names.contains(name))
                    .forEach(names::add);
            return names;
        }

        private static Optional<Pane> findPane(final List<Pane> list, final String name) {
            return list.stream().filter(pane -> pane.name().equalsIgnoreCase(name)).findFirst();
        }

        /** The rows of an object in a tab, with the buttons of its actions. */
        private List<Row> objectRows(final PropertyNode object, final String tab, final Set<PropertyNode> scope) {
            final List<Row> rows = paneRows(object, tab, scope);
            return rows.isEmpty() ? rows : withButtons(object, rows);
        }

        /**
         * The rows of the pane of a container that has the name of the tab, or of all its panes when there is none. A
         * null tab means a self-contained unit, such as the element of a list: all its panes.
         */
        private List<Row> paneRows(final PropertyNode container, final String tab, final Set<PropertyNode> scope) {
            final List<Pane> available = panes(container).list();
            final List<Pane> selected = available
                    .stream()
                    .filter(pane -> pane.name().equalsIgnoreCase(tab))
                    .findFirst()
                    .map(List::of)
                    .orElse(available);
            final List<Row> rows = new ArrayList<>();
            for (final Pane pane : selected) {
                for (final List<String> names : pane.rows()) {
                    final List<Cell> cells = new ArrayList<>();
                    for (final String name : names) {
                        container.child(name).flatMap(child -> placeNode(child, tab, scope)).ifPresent(cells::add);
                    }
                    if (!cells.isEmpty()) {
                        rows.add(new Row(cells));
                    }
                }
            }
            return rows;
        }

        private Optional<Cell> placeNode(final PropertyNode node, final String tab, final Set<PropertyNode> scope) {
            if (isExcluded(node)) {
                return Optional.empty();
            }
            switch (node.kind()) {
                case OBJECT:
                    return group(node, tab, scope);
                case MAP:
                case ARRAY_OF_OBJECT:
                case ARRAY_OF_PRIMITIVE:
                    return scope.add(node) ? Optional.of(new CollectionCell(node, elementRows(node)))
                            : Optional.empty();
                default:
                    return scope.add(node) ? Optional.of(new FieldCell(node)) : Optional.empty();
            }
        }

        private Optional<Cell> group(final PropertyNode object, final String tab, final Set<PropertyNode> scope) {
            final List<Row> rows = objectRows(object, tab, scope);
            return rows.isEmpty() ? Optional.empty() : Optional.of(new GroupCell(object, rows));
        }

        /** How one entry of a list or map is laid out; the same for every entry, so it has a scope of its own. */
        private List<Row> elementRows(final PropertyNode collection) {
            final Set<PropertyNode> scope = newScope();
            switch (collection.kind()) {
                case ARRAY_OF_OBJECT:
                    final PropertyNode element = collection.element().orElseThrow();
                    if (element.kind() == PropertyKind.OBJECT) {
                        return paneRows(element, null, scope);
                    }
                    return placeNode(element, null, scope).map(cell -> List.of(new Row(List.of(cell))))
                            .orElse(List.of());
                case MAP:
                    final List<Cell> cells = new ArrayList<>();
                    for (final String name : List.of("key[]", "value[]")) {
                        collection.child(name).flatMap(child -> placeNode(child, null, scope)).ifPresent(cells::add);
                    }
                    return cells.isEmpty() ? List.of() : List.of(new Row(cells));
                default:
                    return List.of();
            }
        }

        private List<Row> withButtons(final PropertyNode object, final List<Row> rows) {
            final List<Row> result = new ArrayList<>(rows);
            structureOut(rows)
                    .filter(target -> buttons.add(new ShownButton(object, ButtonKind.GUESS_SCHEMA)))
                    .ifPresent(target -> result
                            .add(button(object, ButtonKind.GUESS_SCHEMA, target, schemaBinding(target))));
            object
                    .definition()
                    .action(ActionTypes.HEALTHCHECK)
                    .filter(binding -> buttons.add(new ShownButton(object, ButtonKind.HEALTHCHECK)))
                    .ifPresent(binding -> result.add(button(object, ButtonKind.HEALTHCHECK, object, binding)));
            object
                    .definition()
                    .action(ActionTypes.UPDATE)
                    .filter(binding -> buttons.add(new ShownButton(object, ButtonKind.UPDATE)))
                    .ifPresent(binding -> insertUpdate(result, button(object, ButtonKind.UPDATE, object, binding),
                            binding.after()));
            return result;
        }

        /** The first output structure among the fields and collections laid out directly in the rows. */
        private static Optional<PropertyNode> structureOut(final List<Row> rows) {
            return rows
                    .stream()
                    .flatMap(row -> row.cells().stream())
                    .filter(cell -> !(cell instanceof GroupCell))
                    .map(Build::directNode)
                    .flatMap(Optional::stream)
                    .filter(node -> node.definition().ui().structure() == StructureDirection.OUT)
                    .findFirst();
        }

        private static ActionBinding schemaBinding(final PropertyNode target) {
            final PropertyDefinition definition = target.definition();
            final ActionBinding declared = definition
                    .action(ActionTypes.SCHEMA)
                    .orElseGet(() -> new ActionBinding(ActionTypes.SCHEMA, definition.ui().discoverSchema()));
            return declared.name().isEmpty()
                    ? new ActionBinding(declared.type(), DEFAULT_SCHEMA_ACTION, declared.parameters(),
                            declared.attributes())
                    : declared;
        }

        /** Puts the button of an update action in its own row after the field it names, else at the end. */
        private static void insertUpdate(final List<Row> rows, final Row row, final String after) {
            for (int index = 0; index < rows.size() && !after.isEmpty(); index++) {
                final boolean found = rows
                        .get(index)
                        .cells()
                        .stream()
                        .map(Build::directNode)
                        .flatMap(Optional::stream)
                        .anyMatch(node -> node.name().equals(after));
                if (found) {
                    rows.add(index + 1, row);
                    return;
                }
            }
            rows.add(row);
        }

        private static Optional<PropertyNode> directNode(final Cell cell) {
            if (cell instanceof FieldCell field) {
                return Optional.of(field.node());
            }
            if (cell instanceof CollectionCell collection) {
                return Optional.of(collection.node());
            }
            if (cell instanceof GroupCell group) {
                return Optional.of(group.node());
            }
            return Optional.empty();
        }

        private static Row button(final PropertyNode owner, final ButtonKind kind, final PropertyNode target,
                final ActionBinding binding) {
            return new Row(List.of(new ButtonCell(owner, kind, target, binding)));
        }

        private static boolean isExcluded(final PropertyNode node) {
            return !node.isDisplayed();
        }

        private static boolean isTechnical(final PropertyNode node) {
            return node.name().startsWith(TECHNICAL_PREFIX);
        }

        private static Set<PropertyNode> newScope() {
            return Collections.newSetFromMap(new IdentityHashMap<>());
        }
    }
}
