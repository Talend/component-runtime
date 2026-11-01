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
import java.util.List;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/**
 * Evaluates the {@code @ActiveIf} conditions of a form on a set of values. A property is visible when its own
 * condition holds <em>and</em> every ancestor is visible, so hiding an object hides its whole subtree. Conditions are
 * evaluated on concrete paths: a condition of {@code list[2].b} reads its targets relative to {@code list[2]}.
 *
 * <p>
 * For each clause, {@code result = negate != any(value matches)}; the clauses are combined with the operator of the
 * condition. A clause whose strategy is not supported never hides anything.
 */
public final class VisibilityEngine implements Visibility {

    private final PropertyTree tree;

    private final ConfigurationValues values;

    public VisibilityEngine(final PropertyTree tree, final ConfigurationValues values) {
        this.tree = Preconditions.requireNonNull(tree, "tree");
        this.values = Preconditions.requireNonNull(values, "values");
    }

    @Override
    public boolean isVisible(final String concretePath) {
        String current = concretePath;
        while (current != null) {
            final Optional<PropertyNode> node = tree.find(PropertyTree.definitionPath(current));
            if (node.isPresent() && !holds(node.get().definition().visibility(), current)) {
                return false;
            }
            current = parentOf(current);
        }
        return true;
    }

    private boolean holds(final VisibilityCondition condition, final String path) {
        if (condition.isAlways()) {
            return true;
        }
        final boolean all = condition.operator() == VisibilityCondition.Operator.AND;
        for (final ConditionClause clause : condition.clauses()) {
            final boolean result = holds(clause, path);
            if (all && !result) {
                return false;
            }
            if (!all && result) {
                return true;
            }
        }
        return all;
    }

    private boolean holds(final ConditionClause clause, final String path) {
        final String target = PathResolver.resolve(path, clause.target());
        final boolean hit;
        switch (clause.strategy()) {
            case DEFAULT:
                final String actual = scalar(target);
                hit = clause.values().stream().anyMatch(actual::equals);
                break;
            case LENGTH:
                final int length = length(target);
                hit = clause.values().stream().anyMatch(expected -> String.valueOf(length).equals(expected.trim()));
                break;
            case CONTAINS:
                final List<String> elements = elements(target);
                hit = clause.values()
                        .stream()
                        .anyMatch(expected -> elements.stream().anyMatch(item -> item.contains(expected)));
                break;
            default:
                return true;
        }
        return clause.negate() != hit;
    }

    private String scalar(final String target) {
        final String value = values.get(target);
        if (value == null || value.isEmpty()) {
            final boolean isBoolean = tree.find(PropertyTree.definitionPath(target))
                    .map(node -> node.kind() == PropertyKind.BOOLEAN)
                    .orElse(false);
            return isBoolean ? "false" : "";
        }
        return value;
    }

    private int length(final String target) {
        if (isCollection(target)) {
            return elements(target).size();
        }
        final String value = values.get(target);
        return value == null ? 0 : value.length();
    }

    private List<String> elements(final String target) {
        if (!isCollection(target)) {
            final String value = values.get(target);
            return List.of(value == null ? "" : value);
        }
        final List<String> found = new ArrayList<>();
        for (int i = 0; values.contains(target + "[" + i + "]"); i++) {
            found.add(values.get(target + "[" + i + "]"));
        }
        return found;
    }

    private boolean isCollection(final String target) {
        return tree.find(PropertyTree.definitionPath(target))
                .map(node -> node.kind() == PropertyKind.ARRAY_OF_PRIMITIVE)
                .orElse(false);
    }

    private static String parentOf(final String path) {
        String base = path;
        if (base.endsWith("]")) {
            base = base.substring(0, base.lastIndexOf('['));
        }
        final int dot = base.lastIndexOf('.');
        return dot < 0 ? null : base.substring(0, dot);
    }
}
