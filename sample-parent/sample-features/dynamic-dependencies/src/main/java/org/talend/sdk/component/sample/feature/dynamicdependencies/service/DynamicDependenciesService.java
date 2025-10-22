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
package org.talend.sdk.component.sample.feature.dynamicdependencies.service;

import java.io.Serializable;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Config;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dataset;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dataset.Dependency;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DynamicDependenciesService implements Serializable {

    public static final String DEPENDENCY_ACTION = "DEPENDENCY_ACTION";

    public static final String FIXEDSCHEMA_ACTION = "FIXEDSCHEMA_ACTION";

    public static final String ENTRY_MAVEN = "maven";

    public static final String ENTRY_CLASS = "clazz";

    public static final String ENTRY_IS_LOADED = "is_loaded";

    public static final String ENTRY_CLASSLOADER = "classloader";

    public static final String ENTRY_FROM_LOCATION = "from_location";

    public static final String ENTRY_IS_TCK_CONTAINER = "is_tck_container";

    public static final String ENTRY_IS_LOADED_IN_TCK = "is_loaded_in_tck_manager";

    @Service
    private RecordBuilderFactory factory;

    public Iterator<Record> loadIterator(final Config config) {
        Schema schema = buildSchema();

        List<Record> records = new ArrayList<>();
        for (Dependency dependency : config.getDse().getDependencies()) {
            Builder builder = factory.newRecordBuilder(schema);

            String maven = String.format("%s:%s:%s", dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getVersion());

            boolean isLoaded = false;
            String classLoaderId = "N/A";
            String fromLocation = "N/A";
            try {
                Class<?> clazz = Class.forName(dependency.getClazz());
                isLoaded = true;
                classLoaderId = clazz.getClassLoader().toString();

                CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    fromLocation = codeSource.getLocation().getPath();
                } else {
                    fromLocation = "CodeSource is null for this class.";
                }
            } catch (ClassNotFoundException e) {
                manageException(config.isDieOnError(),
                        "Cannot load class %s from system classloader".formatted(dependency.getClazz()), e);
            }

            boolean isTckContainer = false; // to improve
            boolean isLoadedInTck = false; // to improve

            Record record = builder
                    .withString(ENTRY_MAVEN, maven)
                    .withString(ENTRY_CLASS, dependency.getClazz())
                    .withBoolean(ENTRY_IS_LOADED, isLoaded)
                    .withString(ENTRY_CLASSLOADER, classLoaderId)
                    .withString(ENTRY_FROM_LOCATION, fromLocation)
                    .withBoolean(ENTRY_IS_TCK_CONTAINER, isTckContainer)
                    .withBoolean(ENTRY_IS_LOADED_IN_TCK, isLoadedInTck)
                    .build();
            records.add(record);
        }

        return records.iterator();
    }

    private Schema buildSchema() {
        return factory.newSchemaBuilder(Type.RECORD)
                .withEntry(factory.newEntryBuilder().withName(ENTRY_MAVEN).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_CLASS).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_LOADED).withType(Type.BOOLEAN).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_CLASSLOADER).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_FROM_LOCATION).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_TCK_CONTAINER).withType(Type.BOOLEAN).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_LOADED_IN_TCK).withType(Type.BOOLEAN).build())
                .build();
    }

    private void manageException(final boolean dieOnError, final String message, final Exception e) {
        String msg = "Dynamic dependencies connector raised an exception: %s : %s".formatted(message, e.getMessage());
        log.error(msg, e);
        if (dieOnError) {
            throw new ComponentException(msg, e);
        }
    }

    @DynamicDependencies(DEPENDENCY_ACTION)
    public List<String> getDynamicDependencies(@Option("configuration") final Dataset dataset) {
        return dataset.getDependencies()
                .stream()
                .map(d -> String.format("%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion()))
                .toList();
    }

    @DiscoverSchema(FIXEDSCHEMA_ACTION)
    public Schema guessSchema4Input(final Dataset dse) {
        return buildSchema();
    }
}