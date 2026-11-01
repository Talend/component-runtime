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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.form.ValidationIssue.Kind;
import org.talend.sdk.component.tcketl.domain.property.Constraints;

/**
 * Checks the values of a form against the constraints of the connector.
 *
 * <p>
 * The same rules as the runtime applies, so that what passes here is accepted there:
 * <ul>
 * <li>{@code required}: a text, a number or a choice is not empty, a list or a map has an entry; a boolean always has
 * a value, and 0 is a value;</li>
 * <li>a number parses and respects {@code min} and {@code max};</li>
 * <li>a text respects {@code minLength}, {@code maxLength} and {@code pattern}, which, like in a browser, only has to
 * match somewhere in the text unless it is anchored;</li>
 * <li>a choice is one of the {@code enumValues};</li>
 * <li>a list respects {@code minItems}, {@code maxItems} and {@code uniqueItems}.</li>
 * </ul>
 * An empty value is a missing one: only {@code required} applies to it. What the user does not see is not checked:
 * the properties that are not displayed ({@link PropertyNode#isDisplayed()}) and those a {@link Visibility} hides,
 * with everything below them.
 */
public final class Validator {

    private Validator() {
        // static helpers only
    }

    public static List<ValidationIssue> validate(final FormValues values) {
        return validate(values, Visibility.ALL);
    }

    /** The issues of the visible values, in the order of the properties, then of the entries of lists. */
    public static List<ValidationIssue> validate(final FormValues values, final Visibility visibility) {
        Preconditions.requireNonNull(values, "values");
        Preconditions.requireNonNull(visibility, "visibility");
        final Check check = new Check(values.effective(), visibility);
        values.tree().roots().forEach(root -> check.walk(root, root.path()));
        return List.copyOf(check.issues);
    }

    private static final class Check extends ValueWalker {

        private final ConfigurationValues values;

        private final Visibility visibility;

        private final List<ValidationIssue> issues = new ArrayList<>();

        private final Map<String, Optional<Pattern>> patterns = new HashMap<>();

        private Check(final ConfigurationValues values, final Visibility visibility) {
            this.values = values;
            this.visibility = visibility;
        }

        @Override
        int size(final PropertyNode collection, final String path) {
            return countEntries(values, collection, path);
        }

        @Override
        boolean enter(final PropertyNode node, final String path) {
            return node.isDisplayed() && visibility.isVisible(path);
        }

        @Override
        void collection(final PropertyNode node, final String path, final int size) {
            final Constraints constraints = node.definition().constraints();
            if (constraints.required() && size == 0) {
                issue(path, Kind.REQUIRED, "A value is required.");
            }
            if (constraints.minItems().isPresent() && size < constraints.minItems().getAsInt()) {
                issue(path, Kind.MIN_ITEMS, "Must have at least " + constraints.minItems().getAsInt() + " items.");
            }
            if (constraints.maxItems().isPresent() && size > constraints.maxItems().getAsInt()) {
                issue(path, Kind.MAX_ITEMS, "Must have at most " + constraints.maxItems().getAsInt() + " items.");
            }
            if (constraints.uniqueItems() && node.kind().isArray() && hasDuplicates(node, path, size)) {
                issue(path, Kind.UNIQUE_ITEMS, "Items must be unique.");
            }
        }

        @Override
        void scalar(final PropertyNode node, final String path) {
            if (node.kind() == PropertyKind.BOOLEAN) {
                return;
            }
            final Constraints constraints = node.definition().constraints();
            final String value = values.get(path);
            if (value.isEmpty()) {
                if (constraints.required()) {
                    issue(path, Kind.REQUIRED, "A value is required.");
                }
                return;
            }
            switch (node.kind()) {
                case NUMBER:
                    checkNumber(path, value, constraints);
                    break;
                case ENUM:
                    checkChoice(path, value, constraints);
                    break;
                default:
                    checkText(path, value, constraints);
                    break;
            }
        }

        private void checkNumber(final String path, final String value, final Constraints constraints) {
            final BigDecimal number;
            try {
                number = new BigDecimal(value);
            } catch (final NumberFormatException e) {
                issue(path, Kind.NUMBER_FORMAT, "'" + value + "' is not a number.");
                return;
            }
            if (constraints.min().isPresent()
                    && number.compareTo(BigDecimal.valueOf(constraints.min().getAsInt())) < 0) {
                issue(path, Kind.MIN, "Must be at least " + constraints.min().getAsInt() + ".");
            }
            if (constraints.max().isPresent()
                    && number.compareTo(BigDecimal.valueOf(constraints.max().getAsInt())) > 0) {
                issue(path, Kind.MAX, "Must be at most " + constraints.max().getAsInt() + ".");
            }
        }

        private void checkChoice(final String path, final String value, final Constraints constraints) {
            if (!constraints.enumValues().isEmpty() && !constraints.enumValues().contains(value)) {
                issue(path, Kind.ENUM_VALUE,
                        "'" + value + "' is not one of " + String.join(", ", constraints.enumValues()) + ".");
            }
        }

        private void checkText(final String path, final String value, final Constraints constraints) {
            if (constraints.minLength().isPresent() && value.length() < constraints.minLength().getAsInt()) {
                issue(path, Kind.MIN_LENGTH,
                        "Must have at least " + constraints.minLength().getAsInt() + " characters.");
            }
            if (constraints.maxLength().isPresent() && value.length() > constraints.maxLength().getAsInt()) {
                issue(path, Kind.MAX_LENGTH,
                        "Must have at most " + constraints.maxLength().getAsInt() + " characters.");
            }
            if (!constraints.pattern().isEmpty()
                    && !pattern(constraints.pattern()).map(regex -> regex.matcher(value).find()).orElse(true)) {
                issue(path, Kind.PATTERN, "Must match " + constraints.pattern() + ".");
            }
        }

        /** An expression the platform cannot compile is not enforced: the runtime is the authority on it. */
        private Optional<Pattern> pattern(final String expression) {
            return patterns.computeIfAbsent(expression, key -> {
                try {
                    return Optional.of(Pattern.compile(key));
                } catch (final PatternSyntaxException e) {
                    return Optional.empty();
                }
            });
        }

        private boolean hasDuplicates(final PropertyNode array, final String path, final int size) {
            final PropertyNode element = array.element().orElseThrow();
            final Set<Object> seen = new HashSet<>();
            for (int position = 0; position < size; position++) {
                final String elementPath = element.concretePath(path, position);
                if (!seen.add(element.kind().isScalar() ? values.get(elementPath) : content(elementPath))) {
                    return true;
                }
            }
            return false;
        }

        /** What an element holds, independent of its position. */
        private Map<String, String> content(final String elementPath) {
            final Map<String, String> content = new TreeMap<>();
            values.subtree(elementPath)
                    .forEach((key, value) -> content.put(key.substring(elementPath.length()), value));
            return content;
        }

        private void issue(final String path, final Kind kind, final String message) {
            issues.add(new ValidationIssue(path, kind, message));
        }
    }
}
