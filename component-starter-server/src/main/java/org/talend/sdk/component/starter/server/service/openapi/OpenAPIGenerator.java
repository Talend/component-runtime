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
package org.talend.sdk.component.starter.server.service.openapi;

import static java.util.Collections.singletonList;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.starter.server.service.Strings;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.facet.util.NameConventions;
import org.talend.sdk.component.starter.server.service.openapi.model.ApiModel;
import org.talend.sdk.component.starter.server.service.openapi.model.openapi.OpenAPI;
import org.talend.sdk.component.starter.server.service.openapi.model.swagger.SwaggerAPI;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class OpenAPIGenerator {

    @Inject
    private TemplateRenderer renderer;

    @Inject
    private NameConventions nameConventions;

    @Inject
    private Jsonb jsonb;

    @RequiredArgsConstructor
    enum ApiType {

        OAS20("2.0"),
        OAS30("3.0"),
        TALEND_API_CREATOR("apic-1.0"),
        TALEND_API_TESTER("apit-1.0"),
        UNKNOWN("");

        @Getter
        private final String key;

        public static ApiType fromVersion(final String version) {
            for (ApiType api : values()) {
                if (version.startsWith(api.key)) {
                    return api;
                }
            }
            return UNKNOWN;
        }

        public List<ApiType> getSupportedAPIs() {
            return Arrays.asList(OAS30, OAS20);
        }
    }

    public Collection<FacetGenerator.InMemoryFile> generate(final String family, final Build build,
            final String basePackage, final JsonObject openapi) {
        final ApiType apiType = getApiType(openapi);
        // TODO validateVersion(openapi);
        final ApiModel api = getApiModel(apiType, openapi);

        log.warn("[generate] {}", api.getInfo());
        final String defaultUrl = api.getDefaultUrl();

        final String pck = '/' + basePackage.replace('.', '/') + '/';
        final String javaBase = build.getMainJavaDirectory() + pck;
        final String resourcesBase = build.getMainResourcesDirectory() + pck;
        return ofNullable(openapi.getJsonObject("paths"))
                .map(it -> toFiles(basePackage, family, javaBase, resourcesBase, it))
                .orElseGet(Collections::emptyList);
    }

    private ApiType getApiType(final JsonObject openapi) {
        final String version = ofNullable(
                ofNullable(openapi.getJsonString("openapi"))
                        .orElse(openapi.getJsonString("swagger")))
                                .map(JsonString::getString)
                                .orElse("");
        return ApiType.fromVersion(version);
    }

    private ApiModel getApiModel(final ApiType api, final JsonObject json) {
        switch (api) {
        case OAS20:
            return jsonb.fromJson(json.toString(), SwaggerAPI.class);
        case OAS30:
            return jsonb.fromJson(json.toString(), OpenAPI.class);
        case UNKNOWN:
        default:
            throw new IllegalArgumentException(
                    String.format("UNKNOWN API! Only %s are supported", api.getSupportedAPIs()));
        }
    }

    private Collection<FacetGenerator.InMemoryFile> toFiles(final String basePackage, final String family,
            final String baseFolder, final String resourcesBaseFolder, final JsonObject paths) {
        final List<Operation> operations = extractOperations(paths);
        final Collection<FacetGenerator.InMemoryFile> payloads = new ArrayList<>();

        payloads
                .add(new FacetGenerator.InMemoryFile(baseFolder + "connection/APIConnection.java",
                        renderer.render("generator/openapi/connection.mustache", new ConnectionModel(basePackage))));
        payloads
                .add(new FacetGenerator.InMemoryFile(resourcesBaseFolder + "connection/Messages.properties",
                        "APIConnection.baseUrl._displayName = Base URL\n"
                                + "APIConnection.baseUrl._placeholder = Base URL...\n"));

        final Collection<Option> options = operations
                .stream()
                .flatMap(operation -> operation
                        .getParameters()
                        .stream()
                        .map(param -> new Option(param.getName(), "get" + Strings.capitalize(param.getName()),
                                param.getJavaType(), param.getDefaultValue(),
                                new ArrayList<>(singletonList(operation.getName())), param.getWidget())))
                .collect(toMap(Option::getName, identity(), (a, b) -> {
                    a.getSupportedAPI().addAll(b.getSupportedAPI());
                    return a;
                }))
                .values();
        payloads
                .add(new FacetGenerator.InMemoryFile(baseFolder + "dataset/APIDataSet.java", renderer
                        .render("generator/openapi/dataset.mustache", new DataSetModel(
                                options.stream().anyMatch(it -> it.getDefaultValue() != null), basePackage, operations,
                                options, options.stream().anyMatch(it -> it.getType().startsWith("List<")),
                                options
                                        .stream()
                                        .anyMatch(
                                                it -> it.getWidget() != null && it.getWidget().startsWith("@Code"))))));
        payloads
                .add(new FacetGenerator.InMemoryFile(resourcesBaseFolder + "dataset/Messages.properties",
                        "APIDataSet.api._displayName = API\n" + "APIDataSet.connection._displayName = API connection\n"
                                +
                                // options
                                options.stream().flatMap(opt -> {
                                    final Stream<String> displayName = Stream
                                            .of("APIDataSet." + opt.getName() + "._displayName = <" + opt.getName()
                                                    + ">");
                                    if ("string".equalsIgnoreCase(opt.type)) {
                                        return Stream
                                                .concat(displayName,
                                                        Stream
                                                                .of("APIDataSet." + opt.getName() + "._placeholder = "
                                                                        + opt.getName() + "..."));
                                    }
                                    return displayName;
                                }).sorted().collect(joining("\n", "", "\n\n")) +
                                // enum values/labels
                                operations
                                        .stream()
                                        .flatMap(op -> Stream
                                                .of("APIType." + op.getName() + "._displayName = " + op.getName(),
                                                        "APIType." + op.getName() + "._placeholder = " + op.getName()
                                                                + "..."))
                                        .sorted()
                                        .collect(joining("\n", "", "\n"))));

        payloads
                .add(new FacetGenerator.InMemoryFile(baseFolder + "client/APIClient.java",
                        renderer
                                .render("generator/openapi/client.mustache",
                                        new ClientModel(basePackage,
                                                operations
                                                        .stream()
                                                        .flatMap(it -> it.getParameters().stream())
                                                        .anyMatch(it -> it.javaType.startsWith("List<")),
                                                operations
                                                        .stream()
                                                        .flatMap(it -> it.getParameters().stream())
                                                        .map(Parameter::getJavaImport)
                                                        .distinct()
                                                        .collect(toList()),
                                                "JsonObject", operations))));

        payloads
                .add(new FacetGenerator.InMemoryFile(baseFolder + "source/APIConfiguration.java", renderer
                        .render("generator/openapi/configuration.mustache", new ConfigurationModel(basePackage))));
        payloads
                .add(new FacetGenerator.InMemoryFile(baseFolder + "source/APISource.java",
                        renderer.render("generator/openapi/source.mustache", new ConnectionModel(basePackage))));
        payloads
                .add(new FacetGenerator.InMemoryFile(resourcesBaseFolder + "source/Messages.properties",
                        "APIConfiguration.dataset._displayName = API Dataset\n\n" + family + ".Source._displayName = "
                                + family + " Input\n"));

        payloads
                .add(new FacetGenerator.InMemoryFile(baseFolder + "package-info.java", renderer
                        .render("generator/openapi/package-info.mustache", new PackageModel(basePackage, family))));
        payloads
                .add(new FacetGenerator.InMemoryFile(resourcesBaseFolder + "Messages.properties",
                        family + "._displayName = " + family + "\n\n" + family
                                + ".datastore.APIConnection._displayName = " + family + " Connection\n" + family
                                + ".dataset.APIDataSet._displayName = " + family + " DataSet\n"));

        return payloads;
    }

    private List<Operation> extractOperations(final JsonObject paths) {
        return paths
                .entrySet()
                .stream()
                .filter(path -> path.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                .flatMap(path -> path
                        .getValue()
                        .asJsonObject()
                        .entrySet()
                        .stream()
                        .filter(jsonValue -> jsonValue.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                        .map(operation -> new Operation(
                                getString(operation.getValue(), "operationId")
                                        .map(nameConventions::toJavaName)
                                        .map(Introspector::decapitalize)
                                        .orElseGet(() -> nameConventions
                                                .toJavaName(operation.getKey() + "_" + path.getKey())),
                                ofNullable(operation.getKey()).orElse("GET").toUpperCase(ROOT), path.getKey(),
                                Stream
                                        .concat(getObject(operation.getValue(), "requestBody")
                                                .map(body -> Stream
                                                        .of(new Parameter("body", "getBody", "string", "", null,
                                                                "String", "\\\"{}\\\"", "@Code(\"javascript\")")))
                                                .orElseGet(Stream::empty),
                                                getObjectList(operation.getValue(), "parameters")
                                                        .orElseGet(Stream::empty)
                                                        .map(it -> {
                                                            final String type = getString(it, "in").orElse("query");
                                                            final String name = it.getString("name");
                                                            return mapParameter(it, type, name);
                                                        }))
                                        .collect(toList()))))
                .collect(toList());
    }

    private Parameter mapParameter(final JsonObject it, final String type, final String name) {
        final String javaName = Introspector.decapitalize(nameConventions.toJavaName(name));
        return new Parameter(javaName, "get" + Strings.capitalize(javaName), type,
                getJavaMarkerForParameter(name, type),
                getMarkerImportForParameter(name, type),
                getObject(it, "schema").map(this::mapJavaType).orElse("String"),
                getObject(it, "schema").map(schema -> schema.get("default")).map(defaultValue -> {
                    switch (defaultValue.getValueType()) {
                    case TRUE:
                    case FALSE:
                    case NUMBER:
                        return String.valueOf(defaultValue);
                    case STRING:
                        return JsonString.class.cast(defaultValue).getString();
                    default:
                        return null;
                    }
                }).orElse(null), null);
    }

    private String getMarkerImportForParameter(final String name, final String type) {
        switch (type) {
        case "query":
            return "org.talend.sdk.component.api.service.http.Query";
        case "path":
            return "org.talend.sdk.component.api.service.http.Path";
        case "header":
            return "org.talend.sdk.component.api.service.http.Header";
        case "body":
        case "formData":
            return null;
        default:
            throw new IllegalArgumentException("Unsupported parameter: " + type + "(" + name + ")");
        }
    }

    private String getJavaMarkerForParameter(final String name, final String type) {
        switch (type) {
        case "query":
            return "@Query(\"" + name + "\") ";
        case "path":
            return "@Path(\"" + name + "\") ";
        case "header":
            return "@Header(\"" + name + "\") ";
        case "body":
        case "formData":
            return "";
        default:
            throw new IllegalArgumentException("Unsupported parameter: " + type + "(" + name + ")");
        }
    }

    private String mapJavaType(final JsonObject jsonObject) {
        // we may not have a type but a `$ref`
        // TODO parse ref correctly value referenced
        final String jsonType = jsonObject.getString("type", "unknown");

        switch (jsonType) {
        case "number":
            return "double";
        case "integer":
            return "int";
        case "boolean":
            return "boolean";
        case "string":
            return "String";
        case "array": {
            final JsonObject subSchema = jsonObject.getJsonObject("items");
            return "List<" + mapJavaType(subSchema) + ">";
        }
        case "object":
            return "List<JsonObject>";
        case "unknown": {
            System.out.println("Maybe a ref: " + jsonObject.getString("$ref", "noref"));
            return "JsonObject";
        }
        default:
            throw new IllegalArgumentException("Unsupported type: " + jsonObject);
        }
    }

    private Optional<Stream<JsonObject>> getObjectList(final JsonValue value, final String key) {
        ensureObject(value);
        return ofNullable(value.asJsonObject().getJsonArray(key))
                .map(array -> array.stream().filter(this::ensureObject).map(JsonValue::asJsonObject));
    }

    private Optional<String> getString(final JsonValue value, final String key) {
        ensureObject(value);
        return ofNullable(value.asJsonObject().getJsonString(key)).map(JsonString::getString);
    }

    private Optional<JsonObject> getObject(final JsonValue value, final String key) {
        ensureObject(value);
        return ofNullable(value.asJsonObject().getJsonObject(key));
    }

    private boolean ensureObject(final JsonValue value) {
        if (value.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new IllegalArgumentException("Not an object: " + value);
        }
        return true;
    }

    private void validateVersion(final JsonObject openapi) {
        ofNullable(ofNullable(openapi.getJsonString("openapi"))
                .orElse(openapi.getJsonString("version")))
                        .map(JsonString::getString)
                        .filter(it -> it.startsWith("3.0."))
                        .orElseThrow(() -> new IllegalArgumentException("Only OpenAPI version 3.0 are supported"));
    }

    @Data
    private static class Operation {

        private final String name;

        private final String verb;

        private final String path;

        private final Collection<Parameter> parameters;
    }

    @Data
    private static class Parameter {

        private final String name;

        private final String getter;

        private final String type;

        private final String javaMarker;

        private final String javaImport;

        private final String javaType;

        private final String defaultValue;

        private final String widget;
    }

    @Data
    private static class ClientModel {

        private final String packageName;

        private final boolean importList;

        private final Collection<String> importAPI;

        private final String responsePayloadType;

        private final Collection<Operation> operations;
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

        private final Collection<Operation> operations;

        private final Collection<Option> options;

        private final boolean importList;

        private final boolean importCode;
    }

    @Data
    private static class Option {

        private final String name;

        private final String getter;

        private final String type;

        private final String defaultValue;

        private final Collection<String> supportedAPI;

        private final String widget;
    }

    @Data
    private static class ConfigurationModel {

        private final String packageName;
    }
}
