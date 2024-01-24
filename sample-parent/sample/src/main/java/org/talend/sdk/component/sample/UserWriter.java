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
package org.talend.sdk.component.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

@Processor(name = "writer")
public class UserWriter implements Serializable {

    private final File file;

    private final FileService service;

    private transient PrintStream writer;

    public UserWriter(@Option("file") final File file, final FileService service) {
        this.file = file;
        this.service = service;
    }

    // tag::open[]
    @PostConstruct
    public void open() throws FileNotFoundException {
        writer = service.createOutput(file);
    }
    // end::open[]

    // tag::write[]
    @ElementListener
    public void write(final User user) throws IOException {
        writer.println(user.getId() + ";" + user.getName());
    }
    // end::write[]

    // tag::close[]
    @PreDestroy
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
    // end::close[]
}
