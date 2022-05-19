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

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.starter.server.service.Strings;
import org.talend.sdk.component.starter.server.service.apitester.model.Scenario;
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

    private String javaBase;

    private String testBase;

    private String packageName;

    private Scenario scenario;

    private JsonObject jsonScenario;

    public Collection<InMemoryFile> generate(final String family, final Build build,
            final String basePackage, final JsonObject apitester) {

        jsonScenario = apitester;
        packageName = '/' + basePackage.replace('.', '/') + '/';
        final String javaBase = build.getMainJavaDirectory() + packageName;
        testBase = build.getTestJavaDirectory() + packageName;
        final String resourcesBase = build.getMainResourcesDirectory() + packageName;

        scenario = jsonb.fromJson(apitester.toString(), Scenario.class);

        return toFiles(basePackage, family, javaBase, resourcesBase);
    }

    private Collection<InMemoryFile> toFiles(final String pck, final String family, final String base,
            final String resources) {
        final Collection<InMemoryFile> payloads = new ArrayList<>();

        // datastore
        payloads
                .add(new InMemoryFile(base + "configuration/Connection.java",
                        renderer.render("generator/apitester/connection.mustache", new ConnectionModel(pck))));
        payloads
                .add(new InMemoryFile(resources + "configuration/Messages.properties",
                        "Connection.forceHTTP._displayName = force HTTP\n" +
                                "Connection.instanceHost._displayName = instance Host\n" +
                                "Connection.stopOnFailure._displayName = Stop on failure\n" +
                                "Connection.xhrEmulation._displayName = xhr Emulation\n"));
        // dataset

        final String envName = scenario.getEnvironments().stream().findFirst().get().getName();
        final Collection<Option> options = scenario
                .getEnvironments()
                .stream()
                .findFirst()
                .get()
                .getVariables()
                .values()
                .stream()
                .map(var -> new Option(var.getName(),
                        "get" + Strings.capitalize(var.getName()),
                        "set" + Strings.capitalize(var.getName()),
                        "String",
                        var.getValue(), ""))
                .collect(Collectors.toList());

        final DataSetModel datasetModel = new DataSetModel(true, pck, true, true, options, family, envName);
        payloads
                .add(new InMemoryFile(base + "configuration/Dataset.java",
                        renderer.render("generator/apitester/dataset.mustache", datasetModel)));
        payloads
                .add(new InMemoryFile(resources + "configuration/Messages.properties",
                        "Connection.forceHTTP._displayName = force HTTP\n" +
                                "Connection.instanceHost._displayName = instance Host\n" +
                                "Connection.stopOnFailure._displayName = Stop on failure\n" +
                                "Connection.xhrEmulation._displayName = xhr Emulation\n" +
                                "Dataset.connection._displayName = Connection \n" +
                                options.stream()
                                        .map(o -> "Dataset." + o.getName() + "._displayName = " + o.getName())
                                        .collect(Collectors.joining("\n"))));
        // service
        payloads
                .add(new InMemoryFile(base + "service/UIService.java",
                        renderer.render("generator/apitester/service.mustache", datasetModel)));
        payloads
                .add(new InMemoryFile(base + "service/LicenseServerClient.java",
                        renderer.render("generator/apitester/server-client.mustache", new ConnectionModel(pck))));
        // source
        payloads
                .add(new InMemoryFile(base + "source/Source.java",
                        renderer.render("generator/apitester/source.mustache", new ConnectionModel(pck))));
        payloads
                .add(new InMemoryFile(resources + "source/Messages.properties",
                        family + ".Input._displayName = " + family + " Input\n" +
                                "Configuration.dataset._displayName = API Dataset\n\n" + family
                                + ".Source._displayName = "
                                + family + " Input\n"));
        // configuration
        payloads
                .add(new InMemoryFile(base + "source/Configuration.java", renderer
                        .render("generator/apitester/configuration.mustache", new ConfigurationModel(pck))));
        // package-info
        payloads
                .add(new InMemoryFile(base + "package-info.java", renderer
                        .render("generator/apitester/package-info.mustache", new PackageModel(pck, family))));
        payloads
                .add(new InMemoryFile(resources + "Messages.properties",
                        family + "._displayName = " + family + "\n\n" +
                                family + ".actions.healthcheck.ACTION_HEALTH_CHECK._displayName = " + family
                                + " Health check\n" +
                                family + ".Input._displayName = " + family + " Input\n" +
                                family + ".datastore.Connection._displayName = " + family + " Connection\n" +
                                family + ".dataset.Dataset._displayName = " + family + " Dataset\n"));
        // icon
        payloads.add(new InMemoryFile("src/main/resources/icons/apitester.svg", renderer
                .render("generator/apitester/apitester.svg", null)));
        // scenario
        payloads.add(new InMemoryFile("src/main/resources/scenario.json", jsonScenario.toString()));
        // tests
        payloads.add(new InMemoryFile(testBase + "source/SourceTest.java", renderer
                .render("generator/apitester/test-source.mustache", datasetModel)));
        payloads.add(new InMemoryFile(testBase + "service/UIServiceTest.java", renderer
                .render("generator/apitester/test-service.mustache", datasetModel)));

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

        private final Collection<Option> options;

        private final String family;

        private final String environment;

    }

    @Data
    private static class Option {

        private final String name;

        private final String getter;

        private final String setter;

        private final String type;

        private final String defaultValue;

        private final String widget;
    }

    @Data
    private static class ConfigurationModel {

        private final String packageName;
    }

}
