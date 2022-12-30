/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.test.connectors.input;

import java.time.LocalDate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.test.connectors.config.InputConfig;

import lombok.RequiredArgsConstructor;

@Version(1)
@Icon(value = Icon.IconType.CUSTOM, custom = "input")
@RequiredArgsConstructor
@Documentation("Doc: default DemoInput1 documentation without Internationalization.")
public class DemoInput1 {

    /*
     * The Producer (input component) handles the interaction
     * with a physical source and produces input data for the processing flow.
     * A producer must have a @Producer method without any parameter. It is triggered by the
     * 
     * @Emitter method of the partition mapper and can return any data.
     */

    InputConfig config;

    public DemoInput1(final @Option("InputConfig") InputConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
    }

    @PreDestroy
    public void release() {
    }

    @Producer
    public Object next() {

        return LocalDate.now();
    }

}
