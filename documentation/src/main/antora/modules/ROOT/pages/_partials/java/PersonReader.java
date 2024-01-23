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

import static java.util.Optional.ofNullable;
import static org.talend.sdk.component.api.component.Icon.IconType.USER_CIRCLE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.service.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Version(value = 2, migrationHandler = PersonReader.Migrations.class)
@Icon(USER_CIRCLE)
@Emitter(name = "reader")
public class PersonReader implements Serializable {

    private final File file;

    private final FileService service;

    private transient BufferedReader reader;

    public PersonReader(@Option("file") final File file, final FileService service) {
        this.file = file;
        this.service = service;
    }

    // tag::open[]
    @PostConstruct
    public void open() throws FileNotFoundException {
        reader = service.createInput(file);
    }
    // end::open[]

    // tag::next[]
    @Producer
    public Person readNext() throws IOException {
        final String line = reader.readLine();
        if (line == null) { // end of the data is marked with null
            return null;
        }
        final String[] info = line.split(";"); // poor csv parser
        return new Person(info[0], Integer.parseInt(info[1].trim()));
    }
    // end::next[]

    // tag::close[]
    @PreDestroy
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
    // end::close[]

    @Slf4j
    @AllArgsConstructor
    public static class Migrations implements MigrationHandler {

        private final MigrationV1V2 migrationV1V2;

        @Override
        public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
            switch (incomingVersion) {
            case 1:
                log.info("Migration incoming data to version 2 from version 1");
                return migrationV1V2.migrate(incomingData);
            default:
                log.info("No active migration from version " + incomingData);
            }
            return incomingData;
        }
    }

    @Service
    public static class MigrationV1V2 {

        public Map<String, String> migrate(final Map<String, String> incomingData) {
            ofNullable(incomingData.get("old_file")).ifPresent(v -> {
                incomingData.put("file", v);
                incomingData.remove("old_file");
            });
            return incomingData;
        }
    }
}
