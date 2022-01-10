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
package org.talend.sdk.component.server.test.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "file", name = "output")
public class FileWriter implements Serializable {

    private final File file;

    private transient BufferedWriter writer;

    public FileWriter(@Option("file") final File path) {
        this.file = path;
    }

    @PostConstruct
    public void open() throws IOException {
        writer = new BufferedWriter(new java.io.FileWriter(file));
    }

    @ElementListener
    public void onElement(final Object data) throws IOException {
        writer.write(data.toString() + "\n");
    }

    @AfterGroup
    public void flush() throws IOException {
        writer.flush(); // we are buffered so we need to flush from time to time
    }

    @PreDestroy
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
