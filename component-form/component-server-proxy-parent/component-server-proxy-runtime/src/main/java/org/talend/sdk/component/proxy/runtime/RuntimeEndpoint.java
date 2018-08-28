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
package org.talend.sdk.component.proxy.runtime;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.joda.time.Duration;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.design.extension.repository.Config;
import org.talend.sdk.component.runtime.beam.transform.avro.SchemalessJsonToIndexedRecord;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("runtime")
@ApplicationScoped
public class RuntimeEndpoint {

    private ComponentManager manager;

    private Jsonb jsonb;

    private ExecutorService bulkheadPool;

    @PostConstruct
    private void init() {
        log.warn("Temporary sampling is active");
        System.setProperty("component.manager.callers.skip",
                System.getProperty("component.manager.callers.skip", "true"));
        manager = ComponentManager.instance();
        jsonb = JsonbBuilder.create();
        bulkheadPool = Executors.newFixedThreadPool(64);
    }

    @PreDestroy
    private void destroy() {
        try {
            jsonb.close();
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
        bulkheadPool.shutdownNow();
    }

    @POST
    @Path("read/{id}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/avro-binary")
    public Collection<IndexedRecord> read(@PathParam("id") final String id,
            @PathParam("version") final int configVersion, @QueryParam("limit") @DefaultValue("50") final String limit,
            @QueryParam("instantiation-type") @DefaultValue("configurationtype") final String instantiationType,
            final Map<String, String> configuration) {
        final ComponentFamilyMeta.BaseMeta<?> componentMeta = findComponent(instantiationType, id);
        final Map<String, String> mutatedConfig = ofNullable(configuration).map(HashMap::new).orElseGet(HashMap::new);
        setLimit(componentMeta.getParameterMetas(), mutatedConfig, limit);

        // this runtime code is really temporary, in practise more validations are required like
        // - it is not validated -> createComponent, if optional is empty -> try to create a native
        // - etc...

        if (!componentMeta.isValidated()) { // assume this is because it is beam
            return runBeam(PTransform.class.cast(manager
                    .createComponent(componentMeta.getParent().getName(), componentMeta.getName(),
                            ComponentFamilyMeta.PartitionMapperMeta.class.isInstance(componentMeta)
                                    ? ComponentManager.ComponentType.MAPPER
                                    : ComponentManager.ComponentType.PROCESSOR,
                            configVersion, mutatedConfig)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Can't find component '" + componentMeta.getId() + "'"))),
                    Integer.parseInt(limit));
        }
        // create the component using the native API
        return runNative(manager
                .findMapper(componentMeta.getParent().getName(), componentMeta.getName(), configVersion, mutatedConfig)
                .orElseThrow(
                        () -> new IllegalArgumentException("Can't find component '" + componentMeta.getId() + "'")),
                Integer.parseInt(limit));
    }

    private ComponentFamilyMeta.BaseMeta<?> findComponent(final String instantiationType, final String id) {
        switch (instantiationType) {
        case "configurationtype":
            final Config config = manager
                    .find(container -> Stream.of(container.get(RepositoryModel.class)))
                    .filter(Objects::nonNull)
                    .flatMap(model -> model.getFamilies().stream())
                    .flatMap(family -> family.getConfigs().stream())
                    .filter(it -> it.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("{\"message\":\"No config for this id\"}")
                            .build()));

            final Map<String, ComponentFamilyMeta.PartitionMapperMeta> mappers = manager
                    .find(c -> c.get(ContainerComponentRegistry.class).getComponents().entrySet().stream())
                    .filter(e -> e.getKey().equals(config.getKey().getFamily()))
                    .map(Map.Entry::getValue)
                    .map(ComponentFamilyMeta::getPartitionMappers)
                    .findFirst()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("{\"message\":\"Plugin not found\"}")
                            .build()));
            if (mappers.isEmpty()) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\":\"No source for this configuration\"}")
                        .build());
            }
            return mappers.values().stream().filter(meta -> {
                final Collection<ParameterMeta> sourceMeta = flatten(meta.getParameterMetas()).collect(toList());
                final Optional<ParameterMeta> nestedConfig = sourceMeta
                        .stream()
                        .filter(it -> config.getKey().getConfigType().equals(
                                it.getMetadata().getOrDefault("tcomp::configurationtype::type", ""))
                                && config.getKey().getConfigName().equals(
                                        it.getMetadata().getOrDefault("tcomp::configurationtype::name", "")))
                        .findFirst();
                return nestedConfig.isPresent() && sourceMeta
                        .stream()
                        .filter(it -> !it.getPath().startsWith(nestedConfig.get().getPath() + '.'))
                        .noneMatch(it -> Boolean.parseBoolean(
                                it.getMetadata().getOrDefault("tcomp::validation" + "::required", "false")));
            })
                    .min(comparing(ComponentFamilyMeta.PartitionMapperMeta::getName)) // deterministic selection, can be
                                                                                      // sthg else
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("{\"message\":\"No source " + "found for this " + "configuration\"}")
                            .build()));

        default:
            return manager
                    .find(container -> Stream.of(container.get(ContainerComponentRegistry.class)))
                    .filter(Objects::nonNull)
                    .flatMap(registry -> registry.getComponents().values().stream())
                    .flatMap(comp -> Stream.concat(comp.getPartitionMappers().values().stream(),
                            comp.getProcessors().values().stream()))
                    .filter(comp -> comp.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("{\"message\":\"No component " + "for this id\"}")
                            .build()));
        }
    }

    private Stream<ParameterMeta> flatten(final Collection<ParameterMeta> metas) {
        return metas.stream().flatMap(meta -> Stream.concat(Stream.of(meta), meta.getNestedParameters().stream()));
    }

    // H: the limit is not in an array or so, => we can use the path directly
    private void setLimit(final List<ParameterMeta> parameterMetas, final Map<String, String> mutatedConfig,
            final String limit) {
        parameterMetas.forEach(param -> {
            if (param.getName().equals("$limit")) {
                mutatedConfig.put(param.getPath(), limit);
            } else if (param.getNestedParameters() != null) {
                setLimit(param.getNestedParameters(), mutatedConfig, limit);
            }
        });
    }

    private List<IndexedRecord> runNative(final Mapper mapper, final int limit) {
        final Collection<JsonObject> objects = new ArrayList<>(50);
        mapper.start();
        try {
            final Input input = mapper.create();
            input.start();
            try {
                int remaining = limit;
                while (--remaining >= 0) {
                    final Object next = input.next();
                    objects.add(toJson(next));
                }
            } finally {
                input.stop();
            }
        } finally {
            mapper.stop();
        }

        return objects.stream().map(this::toAvro).collect(toList());
    }

    private Collection<IndexedRecord> runBeam(final PTransform<?, ?> transform, final int limit) {
        final Type[] records = extractTransformRecords(transform);
        if (records != null && records.length == 2) {
            if (Class.class.isInstance(records[0]) && Class.class.isInstance(records[1])) {
                if (records[1] == JsonObject.class && records[0] == PBegin.class) {
                    return runBeam((PTransform<PBegin, PCollection<IndexedRecord>>) transform);
                } else if (IndexedRecord.class.isAssignableFrom(Class.class.cast(records[1]))
                        && records[0] == PBegin.class) {
                    return runBeam((PTransform<PBegin, PCollection<IndexedRecord>>) transform);
                }
            }
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .encoding("{\"error\":\"not a source (processor)\"}")
                    .build());
        }
        // unsafe mode
        return runBeam((PTransform<PBegin, PCollection<IndexedRecord>>) transform);
    }

    private Collection<IndexedRecord> runBeam(final PTransform<PBegin, PCollection<IndexedRecord>> transform) {
        final String id = UUID.randomUUID().toString().replace("-", "");
        final Pipeline pipeline =
                Pipeline
                        .create(PipelineOptionsFactory
                                .fromArgs("--blockOnRun=false", "--enforceImmutability=false",
                                        "--enforceEncodability=false",
                                        "--targetParallelism="
                                                + Math.max(1, Runtime.getRuntime().availableProcessors()))
                                .create());
        pipeline.apply("source", transform).apply(new Capturer(id));

        Collection<IndexedRecord> records;
        Capturer.CAPTURES.put(id, new ArrayList<>());
        Future<PipelineResult.State> future = null;
        try {
            future = bulkheadPool.submit(() -> pipeline.run().waitUntilFinish(Duration.standardMinutes(5)));
            future.get(5, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (final TimeoutException e) {
            future.cancel(true);
            log.warn(e.getMessage(), e);
        } finally {
            records = Capturer.CAPTURES.remove(id);
        }

        return records;
    }

    private JsonObject toJson(final Object next) {
        return JsonObject.class.isInstance(next) ? JsonObject.class.cast(next)
                : jsonb.fromJson(jsonb.toJson(next), JsonObject.class);
    }

    private IndexedRecord toAvro(final JsonObject jsonObject) {
        return new SchemalessJsonToIndexedRecord.Fn("org.talend.avro.generated.record").toAvro(jsonObject);
    }

    private Type[] extractTransformRecords(final Object runtimeObject) {
        final Type genericSuperclass = runtimeObject.getClass().getGenericSuperclass();
        if (ParameterizedType.class.isInstance(genericSuperclass)) {
            final ParameterizedType pt = ParameterizedType.class.cast(genericSuperclass);
            if (pt.getActualTypeArguments().length == 2) {
                return Stream.of(pt.getActualTypeArguments()).map(it -> {
                    if (ParameterizedType.class.isInstance(it)) {
                        final ParameterizedType pCollectionPt = ParameterizedType.class.cast(it);
                        if (pCollectionPt.getActualTypeArguments().length == 1) {
                            return pCollectionPt.getActualTypeArguments()[0];
                        }
                    } else if (PBegin.class == it || PDone.class == it) {
                        return it;
                    }
                    return Object.class;
                }).toArray(Type[]::new);
            }
        }
        return null;
    }

    @Data
    private static class Capturer extends PTransform<PCollection<IndexedRecord>, PDone> {

        private static final ConcurrentMap<String, Collection<IndexedRecord>> CAPTURES = new ConcurrentHashMap<>();

        private final String id;

        @Override
        public PDone expand(final PCollection<IndexedRecord> input) {
            input.apply(ParDo.of(new DoFn<IndexedRecord, IndexedRecord>() {

                @ProcessElement
                public void onElement(@Element final IndexedRecord record) {
                    CAPTURES.get(id).add(record);
                }
            }));
            return PDone.in(input.getPipeline());
        }
    }
}
