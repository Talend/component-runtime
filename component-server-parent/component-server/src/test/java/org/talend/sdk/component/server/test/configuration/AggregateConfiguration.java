/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.test.configuration;

import static java.util.Arrays.asList;
import static org.talend.sdk.component.api.component.Icon.IconType.AGGREGATE;

import java.io.Serializable;
import java.util.List;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@Icon(AGGREGATE)
@Documentation("Aggregate fields.")
@OptionsOrder({ "groupBy", "operations" })
public class AggregateConfiguration implements Serializable {

    @Option
    @Required
    @Documentation("The list of fields used for the aggregation.")
    private List<String> groupBy = asList("");

    @Option
    @Required
    @Documentation("The list of operation that will be executed.")
    private List<AggregateOperation> operations = asList(new AggregateConfiguration.AggregateOperation());

    @Data
    @OptionsOrder({ "fieldPath", "operation", "outputFieldPath" })
    @Documentation("Identifies a field.")
    public static class AggregateOperation implements Serializable {

        @Option
        @Required
        @Suggestable("datalist")
        @Documentation("The source field path.")
        private String fieldPath = "";

        @Option
        @Required
        @Documentation("The operation to apply.")
        private AggregateFieldOperationType operation = AggregateFieldOperationType.SUM;

        @Option
        @Required
        @Documentation("The resulting field name.")
        private String outputFieldPath = "";

    }

    public enum AggregateFieldOperationType {
        SUM
    }
}
