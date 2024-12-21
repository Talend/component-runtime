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
package org.talend.sdk.component.runtime.manager.test;

import static org.talend.sdk.component.api.configuration.condition.ActiveIfs.Operator.AND;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.Pattern;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;
import lombok.Getter;

public class MethodsHolder {

    public void date(@Option("date") final ZonedDateTime date) {
        // no-op
    }

    public void charOption(@Option("delimiter") final char delimiter,
            @Option("delimiter2") final Character delimiter2) {
        // no-op
    }

    public void intOption(@Option("foo1") final int foo1, @Option("foo2") final Integer foo2) {
        // no-op
    }

    public void intOptionOverwrite(@Option("foo1") @Min(42) final int foo1,
            @Option("foo2") @Max(42) final Integer foo2) {
        // no-op
    }

    public void primitives(@Option("url") final String url, @Option final String defaultName,
            @Option("port") final int port) {
        // no-op
    }

    public void collections(@Option("urls") final List<String> urls, @Option("ports") final List<Integer> ports,
            @Option("mapping") final Map<String, String> mapping) {
        // no-op
    }

    public void object(final Config implicit, @Option("prefixed") final Config prefixed) {
        // no-op
    }

    public void nested(final ConfigOfConfig value) {
        // no-op
    }

    public void array(final Array value) {
        // no-op
    }

    public void visibility(final MyDatastore value) {
        // no-op
    }

    public void visibility(final RestDatastore value) {
        // no-op
    }

    public void visibility(@Option("configuration") final FilterConfiguration filters) {
        // no-op
    }

    public void visibility(@Option("configuration") final ConfigWithActiveIfEnum config) {
        // no-op
    }

    @Getter
    public static class Array {

        @Option
        private String[] urls;
    }

    @Getter
    @DataSet("test")
    public static class Config {

        @Option
        @Proposable("test")
        private List<String> urls;

        @Option
        private Map<String, String> mapping;
    }

    @Getter
    public static class ConfigOfConfig {

        @Option
        private List<Config> multiple = new ArrayList<>();

        @Option
        private Map<String, Config> keyed;

        @Option
        private Config direct;

        @Option
        private String passthrough;
    }

    @Getter
    public static class MyDatastore {

        @Option
        @Required
        private String aString;

        @Option
        @Required
        private boolean complexConfig;

        @Option
        @ActiveIf(target = "complexConfig", value = "true")
        private ComplexConfiguration complexConfiguration = new ComplexConfiguration();

        @Getter
        public static class ComplexConfiguration {

            @Option
            @Required
            @Pattern("^https?://.+$")
            private String url = "";
        }
    }

    @Getter
    public static class RestDatastore {

        @Option
        @Required
        private APIDesc apiDesc = new APIDesc();

        @Option
        @ActiveIf(target = "apiDesc/loadAPI", value = "true")
        private ComplexConfiguration complexConfiguration = new ComplexConfiguration();

        @Getter
        public static class APIDesc {

            @Option
            @Required
            private boolean loadAPI;
        }

        @Getter
        public static class ComplexConfiguration {

            @Option
            @Required
            @Pattern("^https?://.+$")
            private String url = "";
        }
    }

    @Data
    public static class FilterConfiguration {

        @Option
        @Required
        @Documentation("How to combine filters")
        private String logicalOpType;

        @Option
        @Required
        @ActiveIf(target = "logicalOpType", value = "ALL")
        @Documentation("How to combine filters")
        private String logicalOpValue;

        @Option
        @Documentation("The list of filters to apply")
        private List<Criteria> filters = new ArrayList<>(Arrays.asList(new Criteria()));

        @Data
        @OptionsOrder({ "columnName", "function", "operator", "value" })
        @Documentation("An unitary filter.")
        public static class Criteria {

            @Option
            @Required
            @Documentation("The input field path to use for this criteria")
            private String columnName;

            @Option
            @Documentation("The operator")
            private String operator;

            @Option
            @Required
            @ActiveIfs(operator = AND, value = {
                    @ActiveIf(negate = true, target = "operator", value = "IS_NULL"),
                    @ActiveIf(negate = true, target = "operator", value = "IS_NOT_NULL"),
                    @ActiveIf(negate = true, target = "operator", value = "IS_EMPTY"),
                    @ActiveIf(negate = true, target = "operator", value = "IS_NOT_EMPTY") })
            @Documentation("The value to compare to")
            private String value;
        }
    }

    @Data
    public static class ConfigWithActiveIfEnum {

        @Option
        @Required
        private boolean bool1;

        @Option
        @Required
        private boolean bool2;

        @Option
        @Required
        private boolean bool3;

        @Option
        @Required
        private ActiveIfEnum enumRequired;

        @Option
        @Required
        @ActiveIf(target = "bool1", value = "true")
        private ActiveIfEnum enumIf;

        @Option
        @Required
        @ActiveIfs({
                @ActiveIf(target = "bool1", value = "true"),
                @ActiveIf(target = "bool2", value = "false"),
                @ActiveIf(target = "bool3", value = "true")
        })
        private ActiveIfEnum enumIfs;

        enum ActiveIfEnum {
            ONE,
            TWO,
            THREE
        }
    }
}
