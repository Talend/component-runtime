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
package org.talend.sdk.component.server.test.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

@Icon(custom = "myicon", value = Icon.IconType.CUSTOM)
@Processor(family = "chain", name = "file")
public class FileOutput implements Serializable {

    private final File file;

    private transient Writer writer;

    public FileOutput(@Option("file") final File file) {
        this.file = file;
    }

    @PostConstruct
    public void init() throws IOException {
        writer = new FileWriter(file);
    }

    @ElementListener
    public void length(final int data) throws IOException {
        writer.write(Integer.toString(data) + System.lineSeparator());
    }

    @PreDestroy
    public void close() throws IOException {
        if (writer == null) {
            return;
        }
        writer.close();
    }
}
