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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.forms.Props.definitions;
import static org.talend.sdk.component.tcketl.forms.Props.map;
import static org.talend.sdk.component.tcketl.forms.Props.object;
import static org.talend.sdk.component.tcketl.forms.Props.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

/** Only the answer to the latest call of a key is delivered; what the services say reaches the view model. */
class ActionServiceTest {

    private static final ActionBinding SUGGEST = new ActionBinding("suggestions", "names");

    private static final List<ActionDefinition> ACTIONS = List.of(new ActionDefinition("fam", "suggestions", "names",
            "Names", List.of(PropertyDefinition.builder("value", PropertyType.STRING).parameterIndex(0).build())));

    /** Keeps the callbacks so that a test decides in which order the answers arrive. */
    private static final class Manual implements ActionContext {

        private final List<ActionRequest> requests = new ArrayList<>();

        private final List<Consumer<ActionOutcome>> sinks = new ArrayList<>();

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
            requests.add(request);
            sinks.add(onOutcome);
        }
    }

    private static FormViewModel model() {
        return FormViewModel.of(definitions(object("cfg"), string("cfg.name"), string("cfg.other")), map());
    }

    @Test
    void aSlowAnswerToAnOldCallNeverOverwritesTheLatestOne() {
        final Manual context = new Manual();
        final FormViewModel model = model();
        final ActionService service = new ActionService(model, context);
        final List<String> delivered = new ArrayList<>();
        model.set("cfg.name", "a");
        service.run("k", service.request(SUGGEST, "cfg.name").orElseThrow(), outcome -> delivered.add("first"));
        model.set("cfg.name", "ab");
        service.run("k", service.request(SUGGEST, "cfg.name").orElseThrow(), outcome -> delivered.add("second"));

        context.sinks.get(1).accept(new ActionOutcome.Checked(true, ""));
        context.sinks.get(0).accept(new ActionOutcome.Checked(true, ""));

        assertEquals(List.of("second"), delivered);
        assertEquals(Map.of("value", "ab"), context.requests.get(1).body());
    }

    @Test
    void nothingIsDeliveredOnceTheFormIsClosed() {
        final Manual context = new Manual();
        final ActionService service = new ActionService(model(), context);
        final List<String> delivered = new ArrayList<>();
        service.run("k", new ActionRequest("fam", "suggestions", "names", Map.of()), outcome -> delivered.add("x"));

        service.close();
        context.sinks.get(0).accept(new ActionOutcome.Checked(true, ""));

        assertTrue(delivered.isEmpty());
    }

    @Test
    void choicesAreKeptWhenAskedOrWhenTheServerSaysSoAndNotOtherwise() {
        final Manual context = new Manual();
        final ActionService service = new ActionService(model(), context);
        final ActionRequest request = new ActionRequest("fam", "suggestions", "names", Map.of());
        final List<ActionOutcome.Choices> got = new ArrayList<>();

        service.choices("k", request, false, got::add);
        context.sinks.get(0).accept(new ActionOutcome.Choices(false, List.of()));
        service.choices("k", request, false, got::add);
        context.sinks.get(1).accept(new ActionOutcome.Choices(true, List.of()));
        service.choices("k", request, false, got::add);

        assertEquals(2, context.requests.size());
        assertEquals(3, got.size());
    }

    @Test
    void aServiceTheConnectorDoesNotDescribeIsNotSupported() {
        final ActionService service = new ActionService(model(), new Manual());

        assertTrue(service.supports(SUGGEST));
        assertEquals(Optional.empty(), service.request(new ActionBinding("suggestions", "other"), "cfg.name"));
        assertTrue(!new ActionService(model(), ActionContext.NONE).supports(SUGGEST));
    }

    @Test
    void aRemoteMessageLastsUntilTheValueChangesAndAnUpdateReplacesTheSubTree() {
        final FormViewModel model = model();
        model.set("cfg.name", "a");

        model.setRemoteMessage("cfg.name", "taken");
        assertEquals(Optional.of("taken"), model.message("cfg.name"));

        model.set("cfg.name", "b");
        assertTrue(model.message("cfg.name").isEmpty());

        model.replaceSubtree("cfg.other", Map.of("", "from server"));
        assertEquals("from server", model.get("cfg.other"));
    }
}
