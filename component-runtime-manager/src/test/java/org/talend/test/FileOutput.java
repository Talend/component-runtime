/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Locale;

import javax.annotation.PreDestroy;
import javax.json.JsonObject;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.AutoLayout;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Processor(family = "file", name = "out")
public class FileOutput implements Serializable {

    private final Configuration configuration;

    private final FileService service;

    @ElementListener
    public void length(final JsonObject data) throws IOException {
        final Writer writer = service.writerFor(new File(configuration.file).getAbsolutePath());
        synchronized (writer) {
            writer
                    .write(data
                            .keySet()
                            .stream()
                            .filter(k -> !k.toLowerCase(Locale.ROOT).contains("id"))
                            .map(data::getString)
                            .collect(joining(" ")) + System.lineSeparator());
        }
    }

    @PreDestroy
    public void close() throws IOException {
        final Writer writer = service.writerFor(new File(configuration.file).getAbsolutePath());
        synchronized (writer) {
            writer.close();
        }
    }

    @Data
    @AutoLayout
    public static class Configuration {

        @Option
        private String file;
    }
}
