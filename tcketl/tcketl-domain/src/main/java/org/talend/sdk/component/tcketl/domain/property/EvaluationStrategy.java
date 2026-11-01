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

/** How a condition compares the value found at its target with the expected values. */
public enum EvaluationStrategy {
    /** String equality. */
    DEFAULT,
    /** Equality of the size of a string or array. */
    LENGTH,
    /** Substring of a string, or element of an array. */
    CONTAINS,
    /** A strategy this designer does not know: the clause cannot be evaluated and counts as satisfied. */
    UNSUPPORTED
}
