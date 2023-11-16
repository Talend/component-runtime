/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.runtime.documentation.component.configuration;

import static org.talend.runtime.documentation.component.configuration.CommonConfig.PROPOSABLE_GET_TABLE_FIELDS;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@GridLayout({ @GridLayout.Row({ "field" }), @GridLayout.Row({ "order" }), })
public class OrderBuilder implements Serializable {

    @Option
    @Proposable(PROPOSABLE_GET_TABLE_FIELDS)
    @Documentation("filed to be ordered")
    private String field;

    @Option
    @Documentation("the order")
    private Order order;

    public OrderBuilder(final QueryBuilder.Fields field, final Order order) {
        this.field = field.name();
        this.order = order;
    }

    public enum Order {
        DESC,
        ASC;
    }
}
