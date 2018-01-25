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
package org.talend.sdk.component.server.front;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.ACTION_ERROR;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.BAD_FORMAT;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.COMPONENT_MISSING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Providers;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.filter.message.DeprecatedEndpoint;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.front.model.execution.PrimitiveWrapper;
import org.talend.sdk.component.server.front.model.execution.WriteStatistics;
import org.talend.sdk.component.server.service.objectmap.JsonObjectMap;

import lombok.extern.slf4j.Slf4j;

// todo: enable to inject a processor before/after the input/output
@Slf4j
@Path("execution")
@ApplicationScoped
@DeprecatedEndpoint
@Consumes(MediaType.APPLICATION_JSON)
public class ExecutionResource {

    private static final int MAX_RECORDS = 1000;

    private static final byte[] EOL = "\n".getBytes(StandardCharsets.UTF_8);

    // requires to not be a valid java field/parameter value
    private static final String CONFIGURATION_VERSION = "tcomp::family::version";

    @Inject
    private ExecutorService executorService;

    @Inject
    private ComponentManager manager;

    @Inject
    private ComponentServerConfiguration appConfiguration;

    private Jsonb inlineStreamingMapper; // particular cause wouldn't support prettification

    private JsonReaderFactory readerFactory;

    private OutputFactory mockOutputFactory = name -> (OutputEmitter) value -> {
        // no-op
    };

    @PostConstruct
    private void init() {
        inlineStreamingMapper = JsonbBuilder.create();
        readerFactory = Json.createReaderFactory(emptyMap());
    }

    @PreDestroy
    private void destroy() {
        try {
            inlineStreamingMapper.close();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @POST
    @Produces("talend/stream")
    @Path("read/{family}/{component}")
    @Documentation("Read inputs from an instance of mapper. The number of returned records if enforced to be limited to 1000. "
            + "The format is a JSON based format where each like is a json record.")
    public void read(@Suspended final AsyncResponse response, @Context final Providers providers,
            @PathParam("family") final String family, @PathParam("component") final String component,
            @QueryParam("size") @DefaultValue("50") final long size, final Map<String, String> configuration) {
        final long maxSize = Math.min(size, MAX_RECORDS);
        response.setTimeoutHandler(asyncResponse -> log.warn("Timeout on dataset retrieval"));
        response.setTimeout(appConfiguration.datasetRetrieverTimeout(), SECONDS);
        executorService.submit(() -> {
            final Optional<Mapper> mapperOptional =
                    manager.findMapper(family, component, getConfigComponentVersion(configuration), configuration);
            if (!mapperOptional.isPresent()) {
                response.resume(new WebApplicationException(Response
                        .status(BAD_REQUEST)
                        .entity(new ErrorPayload(COMPONENT_MISSING, "Didn't find the input component"))
                        .build()));
                return;
            }

            final Mapper mapper = mapperOptional.get();
            mapper.start();
            try {
                final Input input = mapper.create();
                try {
                    input.start();

                    response.resume((StreamingOutput) output -> {
                        Object data;

                        int current = 0;
                        while (current++ < maxSize && (data = input.next()) != null) {
                            if (CharSequence.class.isInstance(data) || Number.class.isInstance(data)
                                    || Boolean.class.isInstance(data)) {
                                final PrimitiveWrapper wrapper = new PrimitiveWrapper();
                                wrapper.setValue(data);
                                data = wrapper;
                            }
                            inlineStreamingMapper.toJson(data, output);
                            output.write(EOL);
                        }
                    });

                } finally {
                    input.stop();
                }
            } finally {
                mapper.stop();
            }
        });
    }

    @POST
    @Consumes("talend/stream")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("write/{family}/{component}")
    @Documentation("Sends records using a processor instance. Note that the processor should have only an input. "
            + "Behavior for other processors is undefined. "
            + "The input format is a JSON based format where each like is a json record - same as for the symmetric endpoint.")
    public void write(@Suspended final AsyncResponse response, @Context final Providers providers,
            @PathParam("family") final String family, @PathParam("component") final String component,
            @QueryParam("group-size") @DefaultValue("50") final long chunkSize, final InputStream stream)
            throws IOException {
        response.setTimeoutHandler(asyncResponse -> log.warn("Timeout on dataset retrieval"));
        response.setTimeout(appConfiguration.datasetRetrieverTimeout(), SECONDS);
        executorService.submit(() -> {
            Processor processor = null;
            final WriteStatistics statistics = new WriteStatistics(0);
            try (final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    response.resume(new WebApplicationException(Response
                            .status(BAD_REQUEST)
                            .entity(new ErrorPayload(ACTION_ERROR, "No configuration sent"))
                            .build()));
                    return;
                }

                final JsonObject configuration;
                try (final JsonReader input = readerFactory.createReader(new StringReader(line))) {
                    configuration = input.readObject();
                }

                final Map<String, String> config = convertConfig(configuration);
                final Optional<Processor> processorOptional =
                        manager.findProcessor(family, component, getConfigComponentVersion(config), config);
                if (!processorOptional.isPresent()) {
                    response.resume(new WebApplicationException(Response
                            .status(BAD_REQUEST)
                            .entity(new ErrorPayload(COMPONENT_MISSING, "Didn't find the output component"))
                            .build()));
                    return;
                }

                processor = processorOptional.get();
                processor.start();

                int groupCount = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        final JsonObject object;
                        try (final JsonReader input = readerFactory.createReader(new StringReader(line))) {
                            object = input.readObject();
                        }
                        if (groupCount == 0) {
                            processor.beforeGroup();
                        }
                        groupCount++;
                        processor.onNext(name -> {
                            if (!Branches.DEFAULT_BRANCH.equals(name)) {
                                throw new IllegalArgumentException(
                                        "Can't access branch '" + name + "' from component " + family + "#" + name);
                            }
                            return new JsonObjectMap(object);
                        }, mockOutputFactory);
                        statistics.setCount(statistics.getCount() + 1);
                        if (groupCount == chunkSize) {
                            processor.afterGroup(mockOutputFactory);
                        }
                    }
                }
            } catch (final Exception e) {
                response.resume(new WebApplicationException(Response
                        .status(BAD_REQUEST)
                        .entity(new ErrorPayload(ACTION_ERROR, "Didn't find the input component"))
                        .build()));
            } finally {
                ofNullable(processor).ifPresent(Processor::stop);
            }
            response.resume(statistics);
        });
    }

    private Map<String, String> convertConfig(final JsonObject configuration) {
        return configuration.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
            switch (e.getValue().getValueType()) {
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case NUMBER:
                return JsonNumber.class.cast(e.getValue()).toString();
            case STRING:
                return JsonString.class.cast(e.getValue()).getString();
            default:
                throw new WebApplicationException(Response
                        .status(BAD_REQUEST)
                        .entity(new ErrorPayload(BAD_FORMAT,
                                "Unsupported parameter " + e.getKey() + "=" + e.getValue()))
                        .build());
            }
        }));
    }

    private int getConfigComponentVersion(final Map<String, String> config) {
        return Integer.parseInt(config.getOrDefault(CONFIGURATION_VERSION, "1"));
    }
}
