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
package org.talend.sdk.component.tcketl.domain.property;

import java.util.List;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * One {@code @ActiveIf} test. {@code target} is the reference to the tested property exactly as declared (relative
 * syntax: sibling name, {@code ../x}, {@code ./x}); the clause holds when the target matches any of {@code values},
 * flipped by {@code negate}. {@code strategyOptions} refine the strategy, e.g. {@code lowercase=true} for
 * {@link EvaluationStrategy#CONTAINS}.
 */
public record ConditionClause(
        String target,
        EvaluationStrategy strategy,
        Map<String, String> strategyOptions,
        List<String> values,
        boolean negate
) {

    public ConditionClause {
        Preconditions.requireNonBlank(target, "target");
        Preconditions.requireNonNull(strategy, "strategy");
        strategyOptions = Preconditions.sortedCopy(strategyOptions, "strategyOptions");
        values = Preconditions.immutableCopy(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("a condition needs at least one expected value");
        }
    }
}
