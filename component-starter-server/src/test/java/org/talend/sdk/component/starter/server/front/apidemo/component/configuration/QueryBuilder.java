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
package org.talend.sdk.component.starter.server.front.apidemo.component.configuration;

import static org.talend.sdk.component.starter.server.front.apidemo.component.configuration.CommonConfig.PROPOSABLE_GET_TABLE_FIELDS;

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
@GridLayout({ @GridLayout.Row({ "field" }), @GridLayout.Row({ "operation" }), @GridLayout.Row({ "value" }) })
public class QueryBuilder implements Serializable {

    @Option
    @Proposable(PROPOSABLE_GET_TABLE_FIELDS)
    @Documentation("field name")
    private String field;

    @Option
    @Documentation("operation")
    private Operation operation;

    @Option
    @Documentation("value")
    private String value = "";

    public enum Operation {

        Equals("="),
        Not_Equals("!="),
        Greater_Than(">"),
        Greater_Than_OR_Equals(">="),
        Less_Than("<"),
        Less_Than_Or_Equals("<=");

        private String op;

        Operation(final String op) {
            this.op = op;
        }

        public String operation() {
            return op;
        }
    }

    public QueryBuilder(final Fields field, final Operation operation, final String value) {
        this.field = field.name();
        this.operation = operation;
        this.value = value;
    }

    public enum Fields {
        parent,
        category,
        made_sla,
        watch_list,
        upon_reject,
        sys_updated_on,
        approval_history,
        number,
        user_input,
        sys_created_on,
        delivery_plan,
        impact,
        active,
        work_notes_list,
        business_service,
        priority,
        sys_domain_path,
        time_worked,
        expected_start,
        rejection_goto,
        opened_at,
        business_duration,
        group_list,
        work_end,
        approval_set,
        wf_activity,
        work_notes,
        short_description,
        correlation_display,
        delivery_task,
        work_start,
        assignment_group,
        additional_assignee_list,
        description,
        calendar_duration,
        close_notes,
        sys_class_name,
        closed_by,
        follow_up,
        contact_type,
        urgency,
        company,
        reassignment_count,
        activity_due,
        assigned_to,
        comments,
        approval,
        sla_due,
        comments_and_work_notes,
        due_date,
        sys_mod_count,
        sys_tags,
        escalation,
        upon_approval,
        correlation_id,
        location;
    }

}
