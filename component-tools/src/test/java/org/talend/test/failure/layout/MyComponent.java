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
package org.talend.test.failure.layout;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

@Documentation("super my component")
@Version
@Icon(FILE_JOB_O)
@Processor(family = "test", name = "my")
public class MyComponent implements Serializable {

    public MyComponent(@Option("configuration") final MyConfig config) {
        // no-op
    }

    @ElementListener
    public Foo passthrough(final Foo item) {
        return item;
    }

    public static class Foo {

    }

    @GridLayout({ @GridLayout.Row({ "input" }), @GridLayout.Row({ "nested" }), @GridLayout.Row({ "badOption" }), })
    public static class MyConfig implements Serializable {

        @Option
        @Documentation("the input value")
        private String input;

        @Option
        @Documentation("it is nested")
        private MyNestedConfig nested = new MyNestedConfig();

        @Option
        @Documentation("it is nested")
        private OtherConfig other = new OtherConfig();

    }

    @OptionsOrder({ "user", "badOption" })
    public static class MyNestedConfig implements Serializable {

        @Option
        @Documentation("the user to log in")
        private String user = "unknown";

        @Option
        @Documentation("password")
        private String password;
    }

    @GridLayout({ @GridLayout.Row({ "prop1", "proxy" }), })
    @OptionsOrder({ "prop1", "prop2" })
    public static class OtherConfig {

        @Option
        @Documentation("prop 1")
        private String prop1 = "unknown";

        @Option
        @Documentation("prop 2")
        private String prop2 = "unknown";
    }
}
