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
package org.talend.sdk.component.starter.server.service.facet.codenvy;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

@ApplicationScoped
public class CodenvyFacet implements FacetGenerator {

    @Inject
    private TemplateRenderer tpl;

    public void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
    }

    @Override
    public String description() {
        return "Pre-configures the project to be usable with Codenvy.";
    }

    @Override
    public String name() {
        return "Codenvy";
    }

    @Override
    public Category category() {
        return Category.TOOL;
    }

    @Override
    public String readme() {
        return "Codenvy allows you to code on this project from any web browser if you host your sources in a Github repository.\n\n"
                + "Click on this link and the project " + "will be opened on your account, ready to develop:\n\n"
                + "image:http://beta.codenvy.com/factory/resources/codenvy-contribute.svg["
                + "Codenvy,link=http://codenvy.io/f?url=https://github.com/@organization@/@repository@,window=\"_blank\"]";
    }

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors, final ServerInfo.Snapshot versions) {
        return Stream
                .of(new InMemoryFile(".codenvy.json",
                        tpl.render("generator/facet/codenvy/codenvy.json", new HashMap<String, String>() {

                            {
                                put("group", build.getGroup());
                                put("artifact", build.getArtifact());
                                put("version", build.getVersion());
                            }
                        })));
    }
}
