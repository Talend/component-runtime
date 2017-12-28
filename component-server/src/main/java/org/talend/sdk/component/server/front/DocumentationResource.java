/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.LocaleMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("documentation")
@ApplicationScoped
public class DocumentationResource {

    @Inject
    private LocaleMapper localeMapper;

    @Inject
    private ComponentDao componentDao;

    @Inject
    private ComponentManager manager;

    @GET
    @Path("component/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Documentation("Returns an asciidoctor version of the documentation for the component represented by its identifier `id`.")
    public DocumentationContent getDependency(@PathParam("id") final String id,
            @QueryParam("language") @DefaultValue("en") final String language) {
        final Locale locale = localeMapper.mapLocale(language);
        final String content = ofNullable(componentDao.findById(id))
                .map(meta -> manager
                        .findPlugin(meta.getParent().getPlugin())
                        .map(Container::getLoader)
                        .orElseThrow(() -> new WebApplicationException(Response
                                .status(NOT_FOUND)
                                .entity(new ErrorPayload(ErrorDictionary.PLUGIN_MISSING,
                                        "No plugin '" + meta.getParent().getPlugin() + "'"))
                                .build())))
                // todo: handle i18n properly, for now just fallback on not suffixed version and assume the dev put it
                // in the comp
                .flatMap(loader -> Stream
                        .of("documentation_" + locale.getLanguage() + ".adoc", "documentation_" + language + ".adoc",
                                "documentation.adoc")
                        .map(name -> loader.getResource("TALEND-INF/" + name))
                        .filter(Objects::nonNull)
                        .findFirst())
                .map(url -> {
                    try (final BufferedReader stream =
                            new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                        return stream.lines().collect(joining("\n"));
                    } catch (final IOException e) {
                        throw new WebApplicationException(Response
                                .status(INTERNAL_SERVER_ERROR)
                                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, e.getMessage()))
                                .build());
                    }
                })
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No component '" + id + "'"))
                        .build()));
        return new DocumentationContent("asciidoc", content);
    }
}
