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

import org.talend.sdk.component.tcketl.domain.Preconditions;

/** The {@code @ActiveIf}/{@code @ActiveIfs} of a property: clauses joined by {@code operator}. No clause = always. */
public record VisibilityCondition(Operator operator, List<ConditionClause> clauses) {

    public static final VisibilityCondition ALWAYS = new VisibilityCondition(Operator.AND, List.of());

    public VisibilityCondition {
        Preconditions.requireNonNull(operator, "operator");
        clauses = Preconditions.immutableCopy(clauses, "clauses");
    }

    public boolean isAlways() {
        return clauses.isEmpty();
    }

    public enum Operator {
        AND,
        OR
    }
}
