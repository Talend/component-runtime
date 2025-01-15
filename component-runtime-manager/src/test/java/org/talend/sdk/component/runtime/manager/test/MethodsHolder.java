/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.Pattern;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.type.DataSet;

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
}
