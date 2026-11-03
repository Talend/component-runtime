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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;
import org.talend.sdk.component.tcketl.forms.ActionContext;
import org.talend.sdk.component.tcketl.forms.FormViewModel;

/** The Vaadin widgets follow the model and write to it only when the user edits them. */
class VaadinFormViewTest {

    private static PropertyDefinition.Builder property(final String path, final PropertyType type) {
        return PropertyDefinition.builder(path, type);
    }

    private static List<PropertyDefinition> properties() {
        return List.of(property("cfg", PropertyType.OBJECT).build(),
                property("cfg.name", PropertyType.STRING).constraints(Constraints.NONE.withRequired(true)).build(),
                property("cfg.mode", PropertyType.ENUM)
                        .constraints(Constraints.NONE.withEnumValues(List.of("A", "B")))
                        .build(),
                property("cfg.flag", PropertyType.BOOLEAN).build(),
                property("cfg.secret", PropertyType.STRING)
                        .visibility(new VisibilityCondition(VisibilityCondition.Operator.AND,
                                List.of(new ConditionClause("flag", EvaluationStrategy.DEFAULT, Map.of(),
                                        List.of("true"), false))))
                        .build());
    }

    private static Stream<Component> walk(final Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(VaadinFormViewTest::walk));
    }

    private static <T> T find(final VaadinFormView view, final Class<T> type, final int index) {
        return walk(view.component()).filter(type::isInstance).map(type::cast).skip(index).findFirst().orElseThrow();
    }

    @Test
    void anEditWritesToTheModelAndAModelChangeIsShown() {
        final FormViewModel model = FormViewModel.of(properties(), Map.of("cfg.name", "first"));
        final VaadinFormView view = VaadinFormView.of(model);
        try {
            final TextField name = find(view, TextField.class, 0);
            assertEquals("first", name.getValue());
            assertEquals(0L, model.revisionProperty().get());

            name.setValue("typed");
            assertEquals("typed", model.get("cfg.name"));

            model.set("cfg.name", "from model");
            assertEquals("from model", name.getValue());

            @SuppressWarnings("unchecked")
            final ComboBox<String> mode = find(view, ComboBox.class, 0);
            assertEquals(null, mode.getValue());
            mode.setValue("B");
            assertEquals("B", model.get("cfg.mode"));
            model.set("cfg.mode", "A");
            assertEquals("A", mode.getValue());
        } finally {
            view.dispose();
        }
    }

    @Test
    void aConditionalFieldIsHiddenUntilItsConditionHolds() {
        final FormViewModel model = FormViewModel.of(properties(), Map.of());
        final VaadinFormView view = VaadinFormView.of(model);
        try {
            final TextField secret = find(view, TextField.class, 1);
            final Component cell = secret.getParent().orElseThrow();
            assertFalse(isShown(secret));

            find(view, Checkbox.class, 0).setValue(true);

            assertEquals("true", model.get("cfg.flag"));
            assertTrue(isShown(secret));
            assertTrue(cell.isVisible());
        } finally {
            view.dispose();
        }
    }

    @Test
    void aStoredValueIsShownByCombosThatHaveNoItemsYet() {
        final List<PropertyDefinition> properties = List.of(property("cfg", PropertyType.OBJECT).build(),
                property("cfg.suggested", PropertyType.STRING)
                        .action(new ActionBinding("suggestions", "names", List.of(), Map.of()))
                        .build(),
                property("cfg.dynamic", PropertyType.STRING)
                        .action(new ActionBinding("dynamic_values", "values", List.of(), Map.of()))
                        .build());
        final FormViewModel model =
                FormViewModel.of(properties, Map.of("cfg.suggested", "kept", "cfg.dynamic", "other"));
        final VaadinFormView view = VaadinFormView.of(model, new ActionContext() {

            @Override
            public String family() {
                return "fam";
            }

            @Override
            public List<ActionDefinition> actions() {
                return List.of(new ActionDefinition("fam", "suggestions", "names", "Names", List.of()),
                        new ActionDefinition("fam", "dynamic_values", "values", "Values", List.of()));
            }

            @Override
            public void execute(final ActionRequest request, final Consumer<ActionOutcome> onOutcome) {
                // never answers: the combos have no items
            }
        });
        try {
            assertEquals("kept", find(view, ComboBox.class, 1).getValue());
            assertEquals("other", find(view, ComboBox.class, 0).getValue());
        } finally {
            view.dispose();
        }
    }

    /** A component is shown when none of its ancestors is hidden. */
    private static boolean isShown(final Component component) {
        Component current = component;
        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }
            current = current.getParent().orElse(null);
        }
        return true;
    }
}
