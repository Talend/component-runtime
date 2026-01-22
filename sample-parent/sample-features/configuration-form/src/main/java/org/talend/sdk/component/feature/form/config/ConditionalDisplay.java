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
package org.talend.sdk.component.feature.form.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIf.EvaluationStrategy;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs.Operator;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "aBoolean" }),
        @GridLayout.Row({ "aString" }),
        @GridLayout.Row({ "conditionString" }),
        @GridLayout.Row({ "secondConditionString" }),
        @GridLayout.Row({ "thirdConditionString" }),
        @GridLayout.Row({ "fourthConditionString" }),
        @GridLayout.Row({ "fifthConditionString" }),
        @GridLayout.Row({ "sixthConditionString" }),
})
public class ConditionalDisplay implements Serializable {

    @Option
    @Documentation("A boolean.")
    private boolean aBoolean;

    @Option
    @Documentation("A String.")
    private String aString;

    @Option
    @ActiveIf(target = "aBoolean", value = "true")
    @Documentation("Displayed if aBoolean is 'true'.")
    private String conditionString;

    @Option
    @ActiveIf(target = "aString", value = { "abc", "def" }, evaluationStrategy = EvaluationStrategy.CONTAINS)
    @Documentation("Displayed if aString contains 'abc' or 'def'.")
    private String secondConditionString;

    @Option
    @ActiveIf(target = "aString", value = "10", evaluationStrategy = EvaluationStrategy.LENGTH)
    @Documentation("Displayed if aString is 10 characters length.")
    private String thirdConditionString;

    @Option
    @ActiveIf(target = "aString", value = "abc", evaluationStrategy = EvaluationStrategy.CONTAINS, negate = true)
    @Documentation("Displayed if aString doesn't contain 'abc'.")
    private String fourthConditionString;

    @Option
    @ActiveIfs(value = {
            @ActiveIf(target = "aBoolean", value = "true"),
            @ActiveIf(target = "aString", value = "abc", evaluationStrategy = EvaluationStrategy.CONTAINS)
    }, operator = Operator.AND)
    @Documentation("Displayed if aBoolean is selected AND aString contain 'abc' (bugged in studio).")
    private String fifthConditionString;

    @Option
    @ActiveIfs(value = {
            @ActiveIf(target = "aBoolean", value = "true"),
            @ActiveIf(target = "aString", value = "abc", evaluationStrategy = EvaluationStrategy.CONTAINS)
    }, operator = Operator.OR)
    @Documentation("Displayed if aBoolean is selected OR aString contain 'abc'.")
    private String sixthConditionString;
}