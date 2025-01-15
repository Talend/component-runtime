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
package org.talend.test.failure.missingplaceholder;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;

@Version
@Icon(FILE_JOB_O)
@Processor(family = "test", name = "my")
public class MyComponent implements Serializable {

    public MyComponent(@Option final Foo foo) {
        // no-op
    }

    @ElementListener
    public Record passthrough(final Record item) {
        return item;
    }

    public static class Foo {

        @Option
        private String missingPlaceholderStr;

        @Option
        private int missingPlaceholderInt;

        @Option
        private Integer missingPlaceholderInteger;

        @Option
        private long missingPlaceholderLong;

        @Option
        private Long missingPlaceholderLongWrapper;

        @Option
        private float missingPlaceholderFloat;

        @Option
        private float missingPlaceholderFloatWrapper;

        @Option
        private double missingPlaceholderDouble;

        @Option
        private Double missingPlaceholderDoubleWrapper;

        @Option
        private boolean missingPlaceholderBoolean;

        @Option
        private Boolean missingPlaceholderBooleanWrapper;

        @Option
        private char missingPlaceholderChar;

        @Option
        private Character missingPlaceholderCharacter;

        @Option
        private AnEnum noMissingPlaceholderEnum;

        public enum AnEnum {
            AAA,
            BBB,
            CCC;
        }
    }
}
