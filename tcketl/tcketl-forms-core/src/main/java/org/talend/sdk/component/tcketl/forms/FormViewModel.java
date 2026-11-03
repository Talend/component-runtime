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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.form.FormLayout;
import org.talend.sdk.component.tcketl.domain.form.FormValues;
import org.talend.sdk.component.tcketl.domain.form.LayoutResolver;
import org.talend.sdk.component.tcketl.domain.form.PropertyTree;
import org.talend.sdk.component.tcketl.domain.form.ValidationIssue;
import org.talend.sdk.component.tcketl.domain.form.Validator;
import org.talend.sdk.component.tcketl.domain.form.Visibility;
import org.talend.sdk.component.tcketl.domain.form.VisibilityEngine;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;

/**
 * The state of one open form: what the connector says about it ({@link #tree()}, {@link #layout()}), the values being
 * edited and what is wrong with them. It uses only JavaFX properties, so it works without a toolkit, and like them it
 * belongs to one thread, the JavaFX application thread.
 *
 * <p>
 * Every edit replaces the {@link FormValues} and bumps the {@link #revisionProperty() revision}, once the values and
 * the issues agree again: the controls pull their state when the revision changes. Opening a form changes nothing, so
 * the {@link #valuesProperty() values} only change for what the user did.
 *
 * <p>
 * A message is shown next to a field when it has been left ({@link #touch}), or when it holds a value that is wrong,
 * so that a form that was just opened is not covered with "required" messages.
 */
public final class FormViewModel {

    private final PropertyTree tree;

    private final FormLayout layout;

    private final ReadOnlyObjectWrapper<FormValues> values = new ReadOnlyObjectWrapper<>(this, "values");

    private final ReadOnlyLongWrapper revision = new ReadOnlyLongWrapper(this, "revision");

    private final Set<String> touched = new HashSet<>();

    private final Map<String, String> remote = new HashMap<>();

    private Visibility visibility = Visibility.ALL;

    private List<ValidationIssue> issues = List.of();

    private Map<String, ValidationIssue> firstIssues = Map.of();

    private FormViewModel(final PropertyTree tree, final FormLayout layout, final FormValues initial) {
        this.tree = tree;
        this.layout = layout;
        this.values.set(initial);
        revalidate(initial);
    }

    /** Opens the form of the properties of a connector on the configuration stored in the node. */
    public static FormViewModel of(final Collection<PropertyDefinition> properties, final Map<String, String> stored) {
        Preconditions.requireNonNull(properties, "properties");
        Preconditions.requireNonNull(stored, "stored");
        final PropertyTree tree = PropertyTree.of(properties);
        return new FormViewModel(tree, LayoutResolver.resolve(tree), FormValues.of(tree, stored));
    }

    public PropertyTree tree() {
        return tree;
    }

    public FormLayout layout() {
        return layout;
    }

    public ReadOnlyObjectProperty<FormValues> valuesProperty() {
        return values.getReadOnlyProperty();
    }

    public FormValues values() {
        return values.get();
    }

    /** Changes each time the values or what is shown about them change. */
    public ReadOnlyLongProperty revisionProperty() {
        return revision.getReadOnlyProperty();
    }

    /** The value at a concrete path, or an empty string when there is none. */
    public String get(final String path) {
        return values.get().get(path);
    }

    /** The number of entries of the list or map at a concrete path. */
    public int count(final String path) {
        return values.get().count(path);
    }

    public void set(final String path, final String value) {
        Preconditions.requireNonNull(value, "value");
        if (!value.equals(values.get().get(path))) {
            remote.remove(path);
            update(values.get().set(path, value));
        }
    }

    /** Replaces everything at or below a path by what a service answered, see {@link FormValues#replaceSubtree}. */
    public void replaceSubtree(final String path, final Map<String, String> suffixes) {
        remote.keySet().removeIf(key -> key.equals(path) || key.startsWith(path + ".") || key.startsWith(path + "["));
        update(values.get().replaceSubtree(path, suffixes));
    }

    /** Shows the verdict of a validation service next to a field until its value changes; empty clears it. */
    public void setRemoteMessage(final String path, final String message) {
        final String before = message == null || message.isBlank() ? remote.remove(path) : remote.put(path, message);
        if (before == null ? message != null && !message.isBlank() : !before.equals(message)) {
            bump();
        }
    }

    public void append(final String path) {
        touched.add(path);
        update(values.get().append(path));
    }

    public void remove(final String path, final int index) {
        touched.add(path);
        update(values.get().remove(path, index));
    }

    public void move(final String path, final int from, final int to) {
        touched.add(path);
        update(values.get().move(path, from, to));
    }

    /** Records that the user went through a field, which makes its message, if any, visible. */
    public void touch(final String path) {
        if (touched.add(path) && firstIssues.containsKey(path)) {
            bump();
        }
    }

    /** Whether the {@code @ActiveIf} conditions show the property at a concrete path with the current values. */
    public boolean isVisible(final String path) {
        return visibility.isVisible(path);
    }

    /** Every issue of the values, whether it is shown or not, in the order of the properties. */
    public List<ValidationIssue> issues() {
        return issues;
    }

    public boolean isValid() {
        return issues.isEmpty();
    }

    /** The message to show next to the field or list at a concrete path, if there is one. */
    public Optional<String> message(final String path) {
        if (remote.containsKey(path)) {
            return Optional.of(remote.get(path));
        }
        final ValidationIssue issue = firstIssues.get(path);
        return issue != null && isShown(path) ? Optional.of(issue.message()) : Optional.empty();
    }

    private boolean isShown(final String path) {
        return touched.contains(path) || !values.get().get(path).isEmpty() || values.get().count(path) > 0;
    }

    private void update(final FormValues next) {
        if (!next.equals(values.get())) {
            revalidate(next);
            values.set(next);
            bump();
        }
    }

    private void revalidate(final FormValues current) {
        visibility = new VisibilityEngine(tree, current.effective());
        issues = Validator.validate(current, visibility);
        final Map<String, ValidationIssue> first = new LinkedHashMap<>();
        issues.forEach(issue -> first.putIfAbsent(issue.path(), issue));
        firstIssues = first;
    }

    private void bump() {
        revision.set(revision.get() + 1);
    }
}
