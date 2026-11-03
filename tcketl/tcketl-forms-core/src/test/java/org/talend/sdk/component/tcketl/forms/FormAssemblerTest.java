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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/** What the assembler asks of any toolkit: tabs, conditions, service buttons and lists, whatever the widgets are. */
class FormAssemblerTest {

    private static final List<ActionDefinition> ACTIONS = List.of(new ActionDefinition("fam", "update", "refresh",
            "Refresh", List.of(PropertyDefinition.builder("value", PropertyType.OBJECT).parameterIndex(0).build())));

    /** A widget that remembers what the assembler did with it. */
    private static final class Widget {

        private final String kind;

        private final String label;

        private final List<Widget> children = new ArrayList<>();

        private boolean visible = true;

        private Consumer<FormToolkit.ButtonResult> click;

        private Feedback feedback = Feedback.IDLE;

        private Runnable add;

        private List<FormToolkit.Entry<Widget>> entries = List.of();

        private Widget(final String kind, final String label) {
            this.kind = kind;
            this.label = label;
        }

        private List<Widget> all(final String wanted) {
            final List<Widget> found = new ArrayList<>();
            if (kind.equals(wanted)) {
                found.add(this);
            }
            children.forEach(child -> found.addAll(child.all(wanted)));
            return found;
        }

        private Widget field(final String path) {
            return all("field").stream().filter(w -> w.label.equals(path)).findFirst().orElseThrow();
        }
    }

    private static final class Recorder implements FormToolkit<Widget> {

        private final List<String> tabNames = new ArrayList<>();

        @Override
        public Widget empty(final String text) {
            return new Widget("empty", text);
        }

        @Override
        public Widget tabs(final List<String> names, final IntFunction<Widget> content) {
            tabNames.addAll(names);
            final Widget tabs = new Widget("tabs", "");
            for (int i = 0; i < names.size(); i++) {
                tabs.children.add(content.apply(i));
            }
            return tabs;
        }

        @Override
        public Widget scroll(final Widget content) {
            return content;
        }

        @Override
        public Widget rows(final List<List<Widget>> rows) {
            final Widget box = new Widget("rows", "");
            rows.forEach(box.children::addAll);
            return box;
        }

        @Override
        public void visible(final Widget node, final boolean visible) {
            node.visible = visible;
        }

        @Override
        public Widget group(final String title, final Widget content) {
            final Widget group = new Widget("group", String.valueOf(title));
            group.children.add(content);
            return group;
        }

        @Override
        public Widget field(final FieldSpec spec, final WidgetKind kind, final boolean compact,
                final FormBindings.Scope scope, final Runnable onBlur) {
            return new Widget("field", spec.path());
        }

        @Override
        public Widget button(final String text, final boolean visible, final String disabledReason,
                final Consumer<ButtonResult> onClick) {
            final Widget button = new Widget("button", text);
            button.visible = visible;
            button.click = onClick;
            return button;
        }

        @Override
        public ListEditor<Widget> collection(final String title, final String documentation, final boolean readOnly,
                final Runnable onAdd) {
            final Widget list = new Widget("list", title);
            list.add = onAdd;
            return new ListEditor<>() {

                @Override
                public Widget node() {
                    return list;
                }

                @Override
                public void message(final String textOrNull) {
                    // not observed
                }

                @Override
                public void entries(final List<Entry<Widget>> values) {
                    list.entries = values;
                }
            };
        }
    }

    private static Widget assemble(final List<PropertyDefinition> properties, final ActionContext actions,
            final Recorder toolkit, final FormViewModel[] holder) {
        final FormViewModel model = FormViewModel.of(properties, Map.of());
        holder[0] = model;
        final FormBindings bindings = new FormBindings(model);
        return new FormAssembler<>(model, bindings, new ActionService(model, actions), toolkit).build();
    }

    private static Widget click(final Widget button) {
        button.click.accept(feedback -> button.feedback = feedback);
        return button;
    }

    @Test
    void aFormWithSeveralLayoutsHasOneTabEach() {
        final Recorder toolkit = new Recorder();
        assemble(List.of(
                PropertyDefinition
                        .builder("cfg", PropertyType.OBJECT)
                        .layout(new LayoutHints(List.of(new GridLayout("Main", List.of(List.of("a"))),
                                new GridLayout("Advanced", List.of(List.of("b")))), List.of()))
                        .build(),
                PropertyDefinition.builder("cfg.a", PropertyType.STRING).build(),
                PropertyDefinition.builder("cfg.b", PropertyType.STRING).build()), ActionContext.NONE, toolkit,
                new FormViewModel[1]);

        assertEquals(List.of("Main", "Advanced"), toolkit.tabNames);
    }

    @Test
    void aFieldFollowsItsCondition() {
        final FormViewModel[] holder = new FormViewModel[1];
        final Widget form = assemble(List.of(PropertyDefinition.builder("cfg", PropertyType.OBJECT).build(),
                PropertyDefinition.builder("cfg.mode", PropertyType.STRING).build(),
                PropertyDefinition
                        .builder("cfg.detail", PropertyType.STRING)
                        .visibility(new VisibilityCondition(VisibilityCondition.Operator.AND,
                                List.of(new ConditionClause("mode", EvaluationStrategy.DEFAULT, Map.of(),
                                        List.of("advanced"), false))))
                        .build()),
                ActionContext.NONE, new Recorder(), holder);

        assertFalse(form.field("cfg.detail").visible);
        holder[0].set("cfg.mode", "advanced");
        assertTrue(form.field("cfg.detail").visible);
        holder[0].set("cfg.mode", "basic");
        assertFalse(form.field("cfg.detail").visible);
    }

    private static List<PropertyDefinition> updatable() {
        return List.of(PropertyDefinition.builder("cfg", PropertyType.OBJECT).build(),
                PropertyDefinition
                        .builder("cfg.upd", PropertyType.OBJECT)
                        .action(new ActionBinding("update", "refresh", List.of("."), Map.of()))
                        .build(),
                PropertyDefinition.builder("cfg.upd.host", PropertyType.STRING).build());
    }

    private static ActionContext answering(final ActionOutcome outcome) {
        return new ActionContext() {

            @Override
            public String family() {
                return "fam";
            }

            @Override
            public List<ActionDefinition> actions() {
                return ACTIONS;
            }

            @Override
            public void execute(final ActionRequest request, final Consumer<ActionOutcome> onOutcome) {
                onOutcome.accept(outcome);
            }
        };
    }

    @Test
    void theUpdateButtonFillsTheFieldsOfItsObject() {
        final FormViewModel[] holder = new FormViewModel[1];
        final Widget form = assemble(updatable(), answering(new ActionOutcome.Replacement(Map.of(".host", "db"))),
                new Recorder(), holder);

        final Widget button = click(form.all("button").get(0));

        assertEquals(Feedback.Kind.OK, button.feedback.kind());
        assertEquals("db", holder[0].get("cfg.upd.host"));
    }

    @Test
    void anEmptyUpdateAnswerIsAnErrorAndChangesNothing() {
        final FormViewModel[] holder = new FormViewModel[1];
        final Widget form = assemble(updatable(), answering(new ActionOutcome.Replacement(Map.of())),
                new Recorder(), holder);

        final Widget button = click(form.all("button").get(0));

        assertEquals(Feedback.Kind.KO, button.feedback.kind());
        assertEquals("", holder[0].get("cfg.upd.host"));
    }

    @Test
    void aButtonWithoutAServiceStaysIdle() {
        final Widget form = assemble(updatable(), ActionContext.NONE, new Recorder(), new FormViewModel[1]);

        assertEquals(1, form.all("button").size());
        assertEquals(Feedback.IDLE, form.all("button").get(0).feedback);
    }

    @Test
    void aListHasOneEntryPerElementAndFollowsAddAndRemove() {
        final FormViewModel[] holder = new FormViewModel[1];
        final Widget form = assemble(List.of(PropertyDefinition.builder("cfg", PropertyType.OBJECT).build(),
                PropertyDefinition.builder("cfg.tags", PropertyType.ARRAY).build(),
                PropertyDefinition.builder("cfg.tags[]", PropertyType.STRING).build()), ActionContext.NONE,
                new Recorder(), holder);
        final Widget list = form.all("list").get(0);

        list.add.run();
        list.add.run();
        assertEquals(2, list.entries.size());

        list.entries.get(0).remove().run();
        assertEquals(1, list.entries.size());
        assertFalse(list.entries.get(0).canUp());
    }
}
