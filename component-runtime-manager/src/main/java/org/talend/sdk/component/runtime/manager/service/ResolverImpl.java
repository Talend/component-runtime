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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResolverImpl implements Resolver, Serializable {

    private final String plugin;

    private final Function<String, File> fileResolver;

    @Override
    public Collection<File> resolveFromDescriptor(final InputStream descriptor) {
        try {
            return new MvnDependencyListLocalRepositoryResolver(null, fileResolver)
                    .resolveFromDescriptor(descriptor)
                    .map(Artifact::toPath)
                    .map(fileResolver)
                    .collect(toList());
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, Resolver.class.getName());
    }
}
