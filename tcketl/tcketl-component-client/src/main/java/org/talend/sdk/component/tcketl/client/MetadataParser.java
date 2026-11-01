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
package org.talend.sdk.component.tcketl.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ConditionClause;
import org.talend.sdk.component.tcketl.domain.property.ConfigurationTypeRef;
import org.talend.sdk.component.tcketl.domain.property.EvaluationStrategy;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.UiHints;
import org.talend.sdk.component.tcketl.domain.property.VisibilityCondition;

/**
 * Turns the {@code metadata} map of a property into typed hints. The keys are the ones the component server writes
 * (see {@code documentation-integration/10-appendix/property-metadata-keys.md}); a key that is not known is ignored,
 * so that a newer server never breaks the designer. Nothing here depends on the HTTP exchange.
 */
final class MetadataParser {

    static final String DOCUMENTATION = "documentation::value";

    private static final String DEFAULT_VALUE = "ui::defaultvalue::value";

    private static final String GRID_PREFIX = "ui::gridlayout::";

    private static final String VALUE_SUFFIX = "::value";

    private static final String ACTION_PREFIX = "action::";

    private static final String CONDITION_PREFIX = "condition::if::";

    private static final String ACTION_PARAMETERS = "parameters";

    private static final int SINGLE_CONDITION = -1;

    private MetadataParser() {
        // static helpers only
    }

    static boolean isTrue(final Map<String, String> metadata, final String key) {
        return "true".equalsIgnoreCase(metadata.get(key));
    }

    static String text(final Map<String, String> metadata, final String key) {
        return metadata.getOrDefault(key, "");
    }

    /** {@code ui::defaultvalue::value} wins over the default the server reports, like in the reference renderer. */
    static String defaultValue(final Map<String, String> metadata, final String reported) {
        final String declared = metadata.get(DEFAULT_VALUE);
        if (declared != null) {
            return declared;
        }
        return reported == null ? "" : reported;
    }

    static int parameterIndex(final Map<String, String> metadata) {
        try {
            return Integer.parseInt(text(metadata, "definition::parameter::index").trim());
        } catch (final NumberFormatException e) {
            return PropertyDefinition.NOT_A_PARAMETER;
        }
    }

    static ConfigurationTypeRef configurationType(final Map<String, String> metadata) {
        final String type = text(metadata, "configurationtype::type").trim();
        return type.isEmpty() ? ConfigurationTypeRef.NONE
                : new ConfigurationTypeRef(type, text(metadata, "configurationtype::name").trim());
    }

    /** The panes in the order the server wrote them, and the plain field order. */
    static LayoutHints layout(final Map<String, String> metadata) {
        final List<GridLayout> grids = new ArrayList<>();
        for (final Map.Entry<String, String> entry : metadata.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(GRID_PREFIX) && key.endsWith(VALUE_SUFFIX)
                    && key.length() > GRID_PREFIX.length() + VALUE_SUFFIX.length()) {
                final String name = key.substring(GRID_PREFIX.length(), key.length() - VALUE_SUFFIX.length()).trim();
                if (!name.isEmpty()) {
                    grid(name, entry.getValue()).ifPresent(grids::add);
                }
            }
        }
        return new LayoutHints(grids, items(text(metadata, "ui::optionsorder::value"), true));
    }

    static UiHints ui(final Map<String, String> metadata) {
        final UiHints.Builder ui = UiHints
                .builder()
                .hidden(isTrue(metadata, "ui::hidden"))
                .readOnly(isTrue(metadata, "ui::readonly"))
                .credential(isTrue(metadata, "ui::credential"))
                .textArea(isTrue(metadata, "ui::textarea"))
                .dateTime(dateTime(metadata.get("ui::datetime")))
                .dateFormat(text(metadata, "ui::datetime::dateFormat"))
                .useSeconds(!"false".equalsIgnoreCase(metadata.get("ui::datetime::useSeconds")))
                .useUtc(!"false".equalsIgnoreCase(metadata.get("ui::datetime::useUTC")))
                .path(path(metadata.get("ui::path::value")));
        final String code = metadata.get("ui::code::value");
        if (code != null) {
            ui.codeLanguage(code.isBlank() ? "text" : code.trim());
        }
        if (metadata.containsKey("ui::structure::value") || metadata.containsKey("ui::structure::type")) {
            ui
                    .structure(structure(metadata.get("ui::structure::type")),
                            text(metadata, "ui::structure::value").trim(),
                            text(metadata, "ui::structure::discoverSchema").trim());
        }
        return ui.build();
    }

    /**
     * The {@code @ActiveIf} clauses: either one unnumbered set of {@code condition::if::*} keys, or several sets
     * suffixed with {@code ::<n>} when the property carries {@code @ActiveIfs}, joined by
     * {@code condition::ifs::operator}.
     */
    static VisibilityCondition visibility(final Map<String, String> metadata) {
        final Map<Integer, Map<String, String>> sets = new TreeMap<>();
        for (final Map.Entry<String, String> entry : metadata.entrySet()) {
            final String key = entry.getKey();
            if (!key.startsWith(CONDITION_PREFIX)) {
                continue;
            }
            final String[] parts = key.substring(CONDITION_PREFIX.length()).split("::");
            if (parts.length == 1) {
                sets.computeIfAbsent(SINGLE_CONDITION, index -> new LinkedHashMap<>()).put(parts[0], entry.getValue());
            } else if (parts.length == 2 && isNumber(parts[1])) {
                sets.computeIfAbsent(Integer.valueOf(parts[1]), index -> new LinkedHashMap<>())
                        .put(parts[0], entry.getValue());
            }
        }
        final List<ConditionClause> clauses = new ArrayList<>();
        for (final Map<String, String> set : sets.values()) {
            clause(set).ifPresent(clauses::add);
        }
        if (clauses.isEmpty()) {
            return VisibilityCondition.ALWAYS;
        }
        final boolean or = "OR".equalsIgnoreCase(text(metadata, "condition::ifs::operator").trim());
        return new VisibilityCondition(or ? VisibilityCondition.Operator.OR : VisibilityCondition.Operator.AND,
                clauses);
    }

    /**
     * The services bound to a property: {@code action::<type>} is the name of the action, {@code action::<type>::
     * parameters} the references of what the request carries and any other {@code action::<type>::<attribute>} a
     * setting.
     */
    static List<ActionBinding> actions(final Map<String, String> metadata) {
        final Map<String, String> names = new LinkedHashMap<>();
        final Map<String, List<String>> parameters = new LinkedHashMap<>();
        final Map<String, Map<String, String>> attributes = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : metadata.entrySet()) {
            final String key = entry.getKey();
            if (!key.startsWith(ACTION_PREFIX)) {
                continue;
            }
            final String rest = key.substring(ACTION_PREFIX.length());
            final int separator = rest.indexOf("::");
            if (separator < 0) {
                if (!rest.isBlank()) {
                    names.put(rest, entry.getValue());
                }
                continue;
            }
            final String type = rest.substring(0, separator);
            final String attribute = rest.substring(separator + 2);
            if (type.isBlank() || attribute.isBlank()) {
                continue;
            }
            if (ACTION_PARAMETERS.equals(attribute)) {
                parameters.put(type, items(entry.getValue(), false));
            } else {
                attributes.computeIfAbsent(type, name -> new LinkedHashMap<>()).put(attribute, entry.getValue());
            }
        }
        return names
                .entrySet()
                .stream()
                .map(action -> new ActionBinding(action.getKey(), action.getValue(),
                        parameters.getOrDefault(action.getKey(), List.of()),
                        attributes.getOrDefault(action.getKey(), Map.of())))
                .collect(Collectors.toUnmodifiableList());
    }

    private static Optional<GridLayout> grid(final String name, final String layout) {
        final List<List<String>> rows = new ArrayList<>();
        for (final String row : layout.split("\\|")) {
            final List<String> cells = items(row, true);
            if (!cells.isEmpty()) {
                rows.add(cells);
            }
        }
        return rows.isEmpty() ? Optional.empty() : Optional.of(new GridLayout(name, rows));
    }

    private static Optional<ConditionClause> clause(final Map<String, String> set) {
        final String target = set.getOrDefault("target", "").trim();
        if (target.isEmpty()) {
            return Optional.empty();
        }
        final String expected = set.getOrDefault("value", "true");
        final String[] values = expected.split(",");
        final String strategy = set.getOrDefault("evaluationStrategy", "");
        return Optional
                .of(new ConditionClause(target, strategy(strategy), strategyOptions(strategy),
                        values.length == 0 ? List.of("") : Arrays.asList(values),
                        "true".equalsIgnoreCase(set.get("negate"))));
    }

    /** {@code NAME} or {@code NAME(option=value,...)}, the name being case insensitive. */
    private static EvaluationStrategy strategy(final String declaration) {
        final String text = declaration.trim();
        final int open = text.indexOf('(');
        final String name = (open < 0 ? text : text.substring(0, open)).trim().toLowerCase(Locale.ROOT);
        switch (name) {
            case "":
            case "default":
                return EvaluationStrategy.DEFAULT;
            case "length":
                return EvaluationStrategy.LENGTH;
            case "contains":
                return EvaluationStrategy.CONTAINS;
            default:
                return EvaluationStrategy.UNSUPPORTED;
        }
    }

    private static Map<String, String> strategyOptions(final String declaration) {
        final String text = declaration.trim();
        final int open = text.indexOf('(');
        final Map<String, String> options = new LinkedHashMap<>();
        if (open >= 0 && text.endsWith(")")) {
            for (final String option : text.substring(open + 1, text.length() - 1).split(",")) {
                final int equals = option.indexOf('=');
                if (equals > 0) {
                    options.put(option.substring(0, equals).trim().toLowerCase(Locale.ROOT),
                            option.substring(equals + 1).trim());
                }
            }
        }
        return options;
    }

    private static UiHints.DateTimeKind dateTime(final String value) {
        switch (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)) {
            case "date":
                return UiHints.DateTimeKind.DATE;
            case "time":
                return UiHints.DateTimeKind.TIME;
            case "datetime":
                return UiHints.DateTimeKind.DATE_TIME;
            case "zoneddatetime":
                return UiHints.DateTimeKind.ZONED_DATE_TIME;
            default:
                return UiHints.DateTimeKind.NONE;
        }
    }

    private static UiHints.PathKind path(final String value) {
        switch (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)) {
            case "FILE":
                return UiHints.PathKind.FILE;
            case "DIRECTORY":
                return UiHints.PathKind.DIRECTORY;
            default:
                return UiHints.PathKind.NONE;
        }
    }

    /** {@code @Structure} defaults to an incoming structure. */
    private static UiHints.StructureDirection structure(final String type) {
        return "OUT".equalsIgnoreCase(type == null ? "" : type.trim()) ? UiHints.StructureDirection.OUT
                : UiHints.StructureDirection.IN;
    }

    /** The comma separated entries, trimmed, without the empty ones, and without duplicates when asked. */
    private static List<String> items(final String list, final boolean distinct) {
        final List<String> items = new ArrayList<>();
        for (final String item : list.split(",")) {
            final String trimmed = item.trim();
            if (!trimmed.isEmpty() && (!distinct || !items.contains(trimmed))) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private static boolean isNumber(final String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit) && value.length() < 9;
    }
}
