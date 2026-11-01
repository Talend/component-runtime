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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;

/**
 * Builds the request of a service from the form: the i-th reference of the binding (default: the property itself) is
 * resolved to a path, and every value at or below that path is sent under the name of the i-th parameter of the
 * action, followed by what is left of the path (so {@code datastore} + {@code cfg.conn.url} becomes
 * {@code datastore.url}). References whose last segment starts with {@code $} are internal to the server and skipped,
 * and empty values are not sent.
 */
public final class ActionBodies {

    private static final String SELF = ".";

    private ActionBodies() {
        // static helpers only
    }

    /**
     * @return the request, or empty when the connector does not describe the action the binding names
     */
    public static Optional<ActionRequest> request(final String family, final ActionBinding binding,
            final List<ActionDefinition> actions, final String ownerPath, final FormValues values) {
        Preconditions.requireNonNull(binding, "binding");
        if (family == null || family.isBlank() || binding.name().isBlank()) {
            return Optional.empty();
        }
        final Optional<ActionDefinition> definition = actions
                .stream()
                .filter(action -> action.type().equals(binding.type()) && action.name().equals(binding.name()))
                .findFirst();
        if (definition.isEmpty()) {
            return Optional.empty();
        }
        final List<String> references = binding.parameters().isEmpty() ? List.of(SELF) : binding.parameters();
        final List<org.talend.sdk.component.tcketl.domain.property.PropertyDefinition> parameters =
                definition.get().parameters();
        final Map<String, String> body = new TreeMap<>();
        final Map<String, String> current = values.effective().asMap();
        for (int i = 0; i < references.size() && i < parameters.size(); i++) {
            final String reference = references.get(i);
            if (lastSegment(reference).startsWith("$")) {
                continue;
            }
            final String target = PathResolver.resolve(ownerPath, reference);
            final String root = parameters.get(i).path();
            current.forEach((key, value) -> {
                if (!value.isEmpty() && isAtOrBelow(key, target)) {
                    body.put(root + key.substring(target.length()), value);
                }
            });
        }
        return Optional.of(new ActionRequest(family, binding.type(), binding.name(), body));
    }

    private static boolean isAtOrBelow(final String key, final String target) {
        return key.equals(target) || key.startsWith(target + ".") || key.startsWith(target + "[");
    }

    private static String lastSegment(final String reference) {
        return reference.substring(reference.lastIndexOf('.') + 1);
    }
}
