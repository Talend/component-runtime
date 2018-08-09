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
package org.talend.sdk.component.proxy.service;

import static java.util.stream.Collectors.toList;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.apache.johnzon.jsonschema.JsonSchemaValidator;
import org.apache.johnzon.jsonschema.JsonSchemaValidatorFactory;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.proxy.api.service.ValidationService;
import org.talend.sdk.component.proxy.jcache.CacheResolverManager;
import org.talend.sdk.component.proxy.jcache.ProxyCacheKeyGenerator;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

@ApplicationScoped
@CacheDefaults(cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
public class ValidationServiceImpl implements ValidationService {

    @Inject
    private ConfigurationClient configurations;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Inject
    private ValidationServiceImpl self;

    private final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();

    @Override
    public CompletionStage<Result> validate(final RequestContext context, final String formId,
            final JsonObject properties) {
        return self
                .getValidator(context, formId)
                .thenApply(validator -> validator.apply(properties))
                .thenApply(vr -> new Result(
                        vr.getErrors().stream().map(e -> new ValidationError(e.getField(), e.getMessage())).collect(
                                toList())));
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.validation.jsonschema")
    public CompletionStage<JsonSchemaValidator> getValidator(final RequestContext context, final String formId) {
        return configurations.getDetails(context.language(), formId, context::findPlaceholder).thenCompose(config -> {
            final JsonSchema jsonSchema = new JsonSchema();
            final JsonSchemaConverter converter = new JsonSchemaConverter(jsonb, jsonSchema, config.getProperties());
            return CompletableFuture
                    .allOf(config
                            .getProperties()
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(p -> p.getName().equals(p.getPath()))
                            .map(it -> new PropertyContext<>(it, context))
                            .map(CompletionStages::toStage)
                            .map(converter::convert)
                            .toArray(CompletableFuture[]::new))
                    .thenApply(r -> jsonSchema);
        }).thenApply(schema -> jsonb.fromJson(jsonb.toJson(schema), JsonObject.class)).thenApply(factory::newInstance);
    }
}
