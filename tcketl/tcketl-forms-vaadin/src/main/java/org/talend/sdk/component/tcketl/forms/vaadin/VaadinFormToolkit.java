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
package org.talend.sdk.component.tcketl.forms.vaadin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldBase;

import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.form.PropertyKind;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.forms.ActionService;
import org.talend.sdk.component.tcketl.forms.Feedback;
import org.talend.sdk.component.tcketl.forms.FieldSpec;
import org.talend.sdk.component.tcketl.forms.FormBindings;
import org.talend.sdk.component.tcketl.forms.FormToolkit;
import org.talend.sdk.component.tcketl.forms.FormViewModel;
import org.talend.sdk.component.tcketl.forms.Texts;
import org.talend.sdk.component.tcketl.forms.WidgetKind;

/** The Vaadin widgets of a form. A toolkit belongs to the UI of its form. */
final class VaadinFormToolkit implements FormToolkit<Component> {

    private static final int TOOLTIP_DELAY_MS = 500;

    private static final String ROW_GAP = "10px";

    private static final int CODE_ROWS = 6;

    private static final int TEXT_AREA_ROWS = 4;

    private final FormViewModel model;

    private final ActionService service;

    VaadinFormToolkit(final FormViewModel model, final ActionService service) {
        this.model = model;
        this.service = service;
    }

    @Override
    public Component empty(final String text) {
        final Span span = new Span(text);
        span.addClassName("form-empty");
        return new Div(span);
    }

    @Override
    public Component tabs(final List<String> names, final IntFunction<Component> content) {
        final Tabs tabs = new Tabs();
        final Div body = new Div();
        final List<Component> panels = new ArrayList<>();
        final List<Tab> all = new ArrayList<>();
        for (final String name : names) {
            final Tab tab = new Tab(name);
            all.add(tab);
            tabs.add(tab);
            panels.add(null);
        }
        final Consumer<Integer> show = index -> {
            if (panels.get(index) == null) {
                final Component panel = content.apply(index);
                panels.set(index, panel);
                body.add(panel);
            }
            for (int i = 0; i < panels.size(); i++) {
                if (panels.get(i) != null) {
                    panels.get(i).setVisible(i == index);
                }
            }
        };
        tabs.addSelectedChangeListener(event -> {
            final int index = all.indexOf(event.getSelectedTab());
            if (index >= 0) {
                show.accept(index);
            }
        });
        if (!names.isEmpty()) {
            show.accept(0);
        }
        final Div box = new Div(tabs, body);
        box.addClassName("form-tabs");
        return box;
    }

    @Override
    public Component scroll(final Component content) {
        content.getElement().getClassList().add("form-rows");
        final Div scroll = new Div(content);
        scroll.getStyle().set("padding", "12px 4px");
        return scroll;
    }

    @Override
    public Component rows(final List<List<Component>> rows) {
        final Div box = new Div();
        box.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", ROW_GAP);
        for (final List<Component> cells : rows) {
            if (cells.size() == 1) {
                box.add(cells.get(0));
            } else {
                final Div grid = new Div();
                grid.getStyle()
                        .set("display", "grid")
                        .set("grid-template-columns", "repeat(" + cells.size() + ", minmax(0, 1fr))")
                        .set("gap", "12px")
                        .set("align-items", "start");
                cells.forEach(grid::add);
                box.add(grid);
            }
        }
        return box;
    }

    @Override
    public void visible(final Component node, final boolean visible) {
        node.setVisible(visible);
    }

    @Override
    public Component group(final String title, final Component content) {
        final Div body = new Div(content);
        if (title == null) {
            return body;
        }
        final Details details = new Details(title, body);
        details.setOpened(true);
        details.addClassName("form-group");
        return details;
    }

    @Override
    public Component field(final FieldSpec spec, final WidgetKind kind, final boolean compact,
            final FormBindings.Scope scope, final Runnable onBlur) {
        final Editor editor = create(kind, spec, scope);
        editor.focusable().getElement().addEventListener("focusout", event -> onBlur.run());
        return compact ? withMessage(editor, spec, scope) : full(editor, spec, scope);
    }

    @Override
    public Component button(final String text, final boolean visible, final String disabledReason,
            final Consumer<ButtonResult> onClick) {
        final Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        final Span result = new Span();
        final HorizontalLayout box = new HorizontalLayout(button, result);
        box.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        box.setPadding(false);
        box.setVisible(visible);
        if (disabledReason != null) {
            button.setEnabled(false);
            Tooltip.forComponent(button).withText(disabledReason);
            return box;
        }
        final ButtonResult handle = feedback -> {
            button.setEnabled(feedback.kind() != Feedback.Kind.RUNNING);
            result.setText(feedback.text());
            result.removeClassNames("form-ok", "form-ko");
            if (feedback.kind() == Feedback.Kind.OK) {
                result.addClassName("form-ok");
            } else if (feedback.kind() == Feedback.Kind.KO) {
                result.addClassName("form-ko");
            }
        };
        button.addClickListener(event -> onClick.accept(handle));
        return box;
    }

    @Override
    public ListEditor<Component> collection(final String title, final String documentation, final boolean readOnly,
            final Runnable onAdd) {
        final Span name = new Span(title);
        name.addClassName("form-title");
        documentation(name, documentation);
        final Div entries = new Div();
        entries.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "6px");
        final Button add = new Button("Add", event -> onAdd.run());
        add.addThemeVariants(ButtonVariant.LUMO_SMALL);
        add.setEnabled(!readOnly);
        final Span message = new Span();
        message.addClassName("form-error");
        message.setVisible(false);
        final Div box = new Div(name, entries, add, message);
        return new ListEditor<>() {

            @Override
            public Component node() {
                return box;
            }

            @Override
            public void message(final String textOrNull) {
                message.setText(textOrNull == null ? "" : textOrNull);
                message.setVisible(textOrNull != null);
            }

            @Override
            public void entries(final List<Entry<Component>> values) {
                entries.removeAll();
                values.forEach(entry -> entries.add(wrap(entry)));
            }
        };
    }

    private static Component wrap(final Entry<Component> entry) {
        final Button up = iconButton("\u2191", "Move up", !entry.canUp(), entry.up());
        final Button down = iconButton("\u2193", "Move down", !entry.canDown(), entry.down());
        final Button remove = iconButton("\u2715", "Remove", !entry.canRemove(), entry.remove());
        final Component body = entry.body();
        if (entry.header() != null) {
            final Span name = new Span(entry.header());
            name.addClassName("form-title");
            final Div spacer = new Div();
            final HorizontalLayout header = new HorizontalLayout(name, spacer, up, down, remove);
            header.expand(spacer);
            header.setWidthFull();
            header.setPadding(false);
            final Div box = new Div(header, body);
            box.addClassName("form-entry");
            box.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-20pct)")
                    .set("border-radius", "6px")
                    .set("padding", "8px");
            return box;
        }
        final HorizontalLayout box = entry.reorderable()
                ? new HorizontalLayout(body, up, down, remove)
                : new HorizontalLayout(body, remove);
        box.expand(body);
        box.setWidthFull();
        box.setPadding(false);
        return box;
    }

    private static Button iconButton(final String glyph, final String label, final boolean disabled,
            final Runnable action) {
        final Button button = new Button(glyph, event -> action.run());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        button.setEnabled(!disabled);
        button.setAriaLabel(label);
        Tooltip.forComponent(button).withText(label);
        return button;
    }

    /** The control, and the component that takes the focus when the user enters it. */
    record Editor(Component node, Component focusable) {
    }

    private Editor create(final WidgetKind kind, final FieldSpec spec, final FormBindings.Scope scope) {
        return switch (kind) {
            case CHECKBOX -> checkBox(spec, scope);
            case CHOICE -> choice(spec, scope);
            case SUGGESTION -> suggestion(spec, scope);
            case DYNAMIC_CHOICE -> dynamicChoice(spec, scope);
            case PASSWORD -> text(new PasswordField(), spec, scope);
            case CODE -> area(spec, scope, CODE_ROWS, "form-code");
            case TEXT_AREA -> area(spec, scope, TEXT_AREA_ROWS, "form-area");
            case PATH, TEXT, NUMBER, DATE_TIME -> text(new TextField(), spec, scope);
            default -> throw new IllegalArgumentException("Not a scalar control: " + kind);
        };
    }

    private <T extends TextFieldBase<T, String>> Editor text(final T field, final FieldSpec spec,
            final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        field.setPlaceholder(spec.prompt());
        field.setReadOnly(spec.readOnly());
        field.setWidthFull();
        field.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        field.addValueChangeListener(event -> guard.push(() -> model.set(spec.path(), event.getValue())));
        scope.onRefresh(() -> {
            final String value = model.get(spec.path());
            if (!value.equals(field.getValue())) {
                guard.pull(() -> field.setValue(value));
            }
        });
        return new Editor(field, field);
    }

    private Editor area(final FieldSpec spec, final FormBindings.Scope scope, final int rows, final String style) {
        final TextArea area = new TextArea();
        area.setMinHeight(rows * 1.6 + "em");
        area.addClassName(style);
        return text(area, spec, scope);
    }

    private Editor checkBox(final FieldSpec spec, final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        final Checkbox box = new Checkbox(Texts.title(spec.node(), spec.index()));
        box.setReadOnly(spec.readOnly());
        box.addValueChangeListener(event -> guard.push(() -> model.set(spec.path(), event.getValue().toString())));
        scope.onRefresh(() -> {
            final boolean selected = Boolean.parseBoolean(model.get(spec.path()));
            if (selected != box.getValue()) {
                guard.pull(() -> box.setValue(selected));
            }
        });
        return new Editor(box, box);
    }

    private Editor choice(final FieldSpec spec, final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        final PropertyDefinition definition = spec.node().definition();
        final Map<String, String> labels = definition.proposalLabels();
        final ComboBox<String> combo = new ComboBox<>();
        combo.setItems(definition.constraints().enumValues());
        combo.setItemLabelGenerator(value -> labels.getOrDefault(value, value));
        combo.setPlaceholder(spec.prompt());
        combo.setReadOnly(spec.readOnly());
        combo.setWidthFull();
        bindRestricted(combo, spec, scope, guard);
        return new Editor(combo, combo);
    }

    private void bindRestricted(final ComboBox<String> combo, final FieldSpec spec, final FormBindings.Scope scope,
            final Guard guard) {
        combo.addValueChangeListener(
                event -> guard.push(() -> model.set(spec.path(), event.getValue() == null ? "" : event.getValue())));
        scope.onRefresh(() -> {
            final String stored = model.get(spec.path());
            final String shown = stored.isEmpty() ? null : stored;
            if (shown == null ? combo.getValue() != null : !shown.equals(combo.getValue())) {
                guard.pull(() -> setWhenEmpty(combo, shown));
            }
        });
    }

    /**
     * A combo refuses a value while it has no items, so the value itself becomes its only item until options arrive.
     */
    private static void setWhenEmpty(final ComboBox<String> combo, final String value) {
        try {
            combo.setValue(value);
        } catch (final IllegalStateException noItems) {
            combo.setItems(List.of(value));
            combo.setValue(value);
        }
    }

    /** An editable combo: what the user types is stored, a suggestion stores its id and shows its label. */
    private Editor suggestion(final FieldSpec spec, final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        final Map<String, String> ids = new HashMap<>();
        final Map<String, String> labels = new HashMap<>();
        final ComboBox<String> combo = new ComboBox<>();
        combo.setAllowCustomValue(true);
        combo.setPlaceholder(spec.prompt());
        combo.setReadOnly(spec.readOnly());
        combo.setWidthFull();
        combo.addCustomValueSetListener(event -> combo.setValue(event.getDetail()));
        combo.addValueChangeListener(event -> guard.push(() -> {
            final String shown = event.getValue() == null ? "" : event.getValue();
            model.set(spec.path(), ids.getOrDefault(shown, shown));
        }));
        final Runnable pull = () -> {
            final String stored = model.get(spec.path());
            final String shown = labels.getOrDefault(stored, stored);
            final String current = combo.getValue() == null ? "" : combo.getValue();
            if (!shown.equals(current)) {
                guard.pull(() -> {
                    if (shown.isEmpty()) {
                        combo.setValue(null);
                    } else {
                        setWhenEmpty(combo, shown);
                    }
                });
            }
        };
        scope.onRefresh(pull);
        final Optional<ActionBinding> binding = spec.node().definition().action(ActionTypes.SUGGESTIONS);
        if (binding.isPresent() && service.supports(binding.get())) {
            final Runnable load = () -> service.request(binding.get(), spec.path())
                    .ifPresent(request -> service.choices(spec.path() + '#' + ActionTypes.SUGGESTIONS, request, false,
                            found -> {
                                ids.clear();
                                labels.clear();
                                found.items().forEach(choice -> {
                                    ids.put(choice.label(), choice.id());
                                    labels.put(choice.id(), choice.label());
                                });
                                guard.pull(() -> combo
                                        .setItems(found.items().stream().map(ActionOutcome.Choice::label).toList()));
                                pull.run();
                            }));
            combo.addFocusListener(event -> load.run());
        }
        return new Editor(combo, combo);
    }

    /** A restricted combo whose options the server gives once, when the form is built; the stored value is the id. */
    private Editor dynamicChoice(final FieldSpec spec, final FormBindings.Scope scope) {
        final Guard guard = new Guard();
        final Map<String, String> labels = new HashMap<>();
        final ComboBox<String> combo = new ComboBox<>();
        combo.setItemLabelGenerator(value -> labels.getOrDefault(value, value));
        combo.setPlaceholder(spec.prompt());
        combo.setReadOnly(spec.readOnly());
        combo.setWidthFull();
        bindRestricted(combo, spec, scope, guard);
        spec.node()
                .definition()
                .action(ActionTypes.DYNAMIC_VALUES)
                .flatMap(binding -> service.request(binding, spec.path()))
                .ifPresent(request -> service.choices(spec.path() + '#' + ActionTypes.DYNAMIC_VALUES, request, true,
                        found -> {
                            found.items().forEach(choice -> labels.put(choice.id(), choice.label()));
                            final String stored = model.get(spec.path());
                            guard.pull(() -> {
                                combo.setItems(found.items().stream().map(ActionOutcome.Choice::id).toList());
                                combo.setValue(stored.isEmpty() ? null : stored);
                            });
                        }));
        return new Editor(combo, combo);
    }

    private Component withMessage(final Editor editor, final FieldSpec spec, final FormBindings.Scope scope) {
        final Span message = new Span();
        message.addClassName("form-error");
        message.setVisible(false);
        editor.focusable().getElement().addEventListener("focusout", event -> model.touch(spec.path()));
        scope.onRefresh(() -> {
            final Optional<String> text = model.message(spec.path());
            message.setText(text.orElse(""));
            message.setVisible(text.isPresent());
            if (editor.node() instanceof com.vaadin.flow.component.HasValidation validation) {
                validation.setInvalid(text.isPresent());
            }
        });
        final Div box = new Div(editor.node(), message);
        box.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "2px");
        return box;
    }

    /** A titled field: title, required mark, documentation, control and message. */
    private Component full(final Editor editor, final FieldSpec spec, final FormBindings.Scope scope) {
        final PropertyDefinition definition = spec.node().definition();
        final Div box = new Div();
        box.addClassName("form-field");
        box.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "3px");
        final boolean checkBox = spec.node().kind() == PropertyKind.BOOLEAN;
        if (!checkBox) {
            final Span title = new Span(Texts.title(spec.node(), spec.index()));
            title.addClassName("form-title");
            title.getStyle().set("font-weight", "500");
            final Div row = new Div(title);
            if (definition.constraints().required()) {
                final Span star = new Span(" *");
                star.addClassName("form-required");
                star.getStyle().set("color", "var(--lumo-error-text-color)");
                row.add(star);
            }
            box.add(row);
            documentation(row, Texts.numbered(definition.documentation(), spec.index()));
        } else {
            documentation(editor.node(), Texts.numbered(definition.documentation(), spec.index()));
        }
        box.add(withMessage(editor, spec, scope));
        return box;
    }

    /** Shows the documentation as a tooltip once the pointer rests half a second on the component. */
    static void documentation(final Component component, final String documentation) {
        if (documentation == null || documentation.isEmpty()) {
            return;
        }
        final Tooltip tooltip = Tooltip.forComponent(component).withText(documentation);
        tooltip.setHoverDelay(TOOLTIP_DELAY_MS);
    }

    /** Tells a change made by the user from one made by the refresh. */
    private static final class Guard {

        private boolean pulling;

        void pull(final Runnable action) {
            final boolean before = pulling;
            pulling = true;
            try {
                action.run();
            } finally {
                pulling = before;
            }
        }

        void push(final Runnable action) {
            if (!pulling) {
                action.run();
            }
        }
    }
}
