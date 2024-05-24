/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.annotation.JsonbPropertyOrder;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.json.TalendAccessMode;

import routines.system.IPersistableRow;

class EnsurePojoMappingTest {

    @Test
    void run() throws Exception {
        final row1Struct model = new row1Struct();
        model.firstName = "Gary";
        model.lName = "Moore";
        model.Age = "dead";
        JsonbConfig jsonbConfig = new JsonbConfig().setProperty("johnzon.accessMode", new TalendAccessMode());
        try (final Jsonb jsonb = JsonbBuilder.newBuilder().withConfig(jsonbConfig).build()) {
            assertEquals("{\"firstName\":\"Gary\",\"lName\":\"Moore\",\"Age\":\"dead\"}", jsonb.toJson(model));
        }
    }

    @JsonbPropertyOrder({ "firstName", "lName", "Age" })
    public static class row1Struct implements IPersistableRow<row1Struct> {

        public String firstName;

        public String getFirstName() {
            return this.firstName;
        }

        public String lName;

        public String getLName() {
            return this.lName;
        }

        public String Age;

        public String getAge() {
            return this.Age;
        }
    }
}
