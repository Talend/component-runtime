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
package org.talend.sdk.component.source;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.service.GenerateExceptionServices;
import org.talend.sdk.component.service.TckValidatorService;

@Documentation("TODO fill the documentation for this source")
public class TCKInputSource implements Serializable {

    private final TCKInputMapperConfiguration configuration;

    private final TckValidatorService service;

    private final GenerateExceptionServices exceptionServices;

    public TCKInputSource(@Option("configuration") final TCKInputMapperConfiguration configuration,
            final TckValidatorService service,
            final GenerateExceptionServices exceptionServices) {
        this.configuration = configuration;
        this.service = service;
        this.exceptionServices = exceptionServices;
    }

    @PostConstruct
    public void init() {
        exceptionServices.generateException();
        // this method will be executed once for the whole component execution,
        // this is where you can establish a connection for instance
    }

    @Producer
    public Record next() {
        exceptionServices.generateRuntimeException();
        // this is the method allowing you to go through the dataset associated
        // to the component configuration
        //
        // return null means the dataset has no more data to go through
        // you can use the builderFactory to create a new Record.
        return null;
    }

    @PreDestroy
    public void release() {
        // this is the symmetric method of the init() one,
        // release potential connections you created or data you cached
    }
}