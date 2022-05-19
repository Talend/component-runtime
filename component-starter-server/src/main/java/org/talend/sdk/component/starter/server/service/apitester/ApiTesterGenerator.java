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
package org.talend.sdk.component.starter.server.service.apitester;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator.InMemoryFile;
import org.talend.sdk.component.starter.server.service.facet.util.NameConventions;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ApiTesterGenerator {

    @Inject
    private TemplateRenderer renderer;

    @Inject
    private NameConventions nameConventions;

    @Inject
    private Jsonb jsonb;

    public Collection<InMemoryFile> generate(final String family, final Build build,
            final String basePackage, final JsonObject openapi) {
        final String pck = '/' + basePackage.replace('.', '/') + '/';
        final String javaBase = build.getMainJavaDirectory() + pck;
        final String resourcesBase = build.getMainResourcesDirectory() + pck;
        return ofNullable(openapi.getJsonObject("paths"))
                .map(it -> toFiles(basePackage, family, javaBase, resourcesBase, it))
                .orElseGet(Collections::emptyList);
    }

    private Collection<InMemoryFile> toFiles(final String pck, final String family,
            final String base, final String resources, final JsonObject scenario) {
        final Collection<InMemoryFile> payloads = new ArrayList<>();

        // datastore
        payloads
                .add(new InMemoryFile(base + "connection/APIConnection.java",
                        renderer.render("generator/apitester/connection.mustache", new ConnectionModel(pck))));
        payloads
                .add(new InMemoryFile(resources + "connection/Messages.properties",
                        "APIConnection.baseUrl._displayName = Base URL\n"
                                + "APIConnection.baseUrl._placeholder = Base URL...\n"));
        // dataset
        final DataSetModel datasetModel = new DataSetModel(true, "basePackage", true, true);
        payloads
                .add(new InMemoryFile(base + "dataset/APIDataSet.java",
                        renderer.render("generator/apitester/dataset.mustache", datasetModel)));
        payloads
                .add(new InMemoryFile(resources + "dataset/Messages.properties",
                        "APIDataSet.api._displayName = API\n"
                                + "APIDataSet.connection._displayName = API connection\n"));

        payloads
                .add(new InMemoryFile(base + "source/APIConfiguration.java", renderer
                        .render("generator/apitester/configuration.mustache", new ConfigurationModel(pck))));
        payloads
                .add(new InMemoryFile(base + "source/APISource.java",
                        renderer.render("generator/apitester/source.mustache", new ConnectionModel(pck))));
        payloads
                .add(new InMemoryFile(resources + "source/Messages.properties",
                        "APIConfiguration.dataset._displayName = API Dataset\n\n" + family + ".Source._displayName = "
                                + family + " Input\n"));
        payloads
                .add(new InMemoryFile(base + "package-info.java", renderer
                        .render("generator/apitester/package-info.mustache", new PackageModel(pck, family))));
        payloads
                .add(new InMemoryFile(resources + "Messages.properties",
                        family + "._displayName = " + family + "\n\n" + family
                                + ".datastore.APIConnection._displayName = " + family + " Connection\n" + family
                                + ".dataset.APIDataSet._displayName = " + family + " DataSet\n"));

        return payloads;
    }

    @Data
    private static class ClientModel {

        private final String packageName;

        private final boolean importList;

        private final Collection<String> importAPI;

        private final String responsePayloadType;

    }

    @Data
    private static class PackageModel {

        private final String packageName;

        private final String family;
    }

    @Data
    private static class ConnectionModel {

        private final String packageName;
    }

    @Data
    private static class DataSetModel {

        private final boolean importDefaultValue;

        private final String packageName;

        private final boolean importList;

        private final boolean importCode;
    }

    @Data
    private static class ConfigurationModel {

        private final String packageName;
    }

}
