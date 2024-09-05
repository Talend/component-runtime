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
package org.talend.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;

import lombok.Data;

@Emitter(family = "db", name = "input")
public class DataInput implements Serializable {

    private static final Map<String, List<Object>> data = new HashMap<>();

    static {
        data.put("users", new ArrayList<Object>() {

            {
                add(new User("10", "sophia"));
                add(new User("20", "emma"));
                add(new User("30", "liam"));
                add(new User("40", "ava"));
            }
        });

        data.put("address", new ArrayList<Object>() {

            {
                add(new Address("40", "paris"));
                add(new Address("10", "nantes"));
                add(new Address("20", "strasbourg"));
                add(new Address("30", "lyon"));
            }
        });

        data.put("salary", new ArrayList<Object>() {

            {
                add(new Salary("20", "1900"));
                add(new Salary("30", "3055"));
                add(new Salary("40", "2600.30"));
                add(new Salary("10", "2000.5"));
            }
        });
    }

    private transient Iterator<Object> iterator;

    private Jsonb jsonb;

    private final String tableName;

    public DataInput(@Option("tableName") String tableName, Jsonb jsonb) {
        this.tableName = tableName;
        this.jsonb = jsonb;
    }

    @Producer
    public JsonObject data() {
        if (iterator == null) {
            iterator = data.get(tableName).iterator();
        }
        return iterator.hasNext() ? jsonb.fromJson(jsonb.toJson(iterator.next()), JsonObject.class) : null;
    }

    @Data
    private static class User {

        private final String id;

        private final String name;
    }

    @Data
    private static class Address {

        private final String userId;

        private final String city;
    }

    @Data
    private static class Salary {

        private final String userId;

        private final String salary;
    }

}
