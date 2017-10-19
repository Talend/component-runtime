// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager.service;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import org.talend.component.api.service.dependency.Resolver;
import org.talend.components.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.components.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResolverImpl implements Resolver, Serializable {

    private final String plugin;

    private final Function<String, File> fileResolver;

    @Override
    public Collection<File> resolveFromDescriptor(final InputStream descriptor) {
        try {
            return new MvnDependencyListLocalRepositoryResolver(null).resolveFromDescriptor(descriptor).map(fileResolver)
                    .collect(toList());
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new Replacer(plugin);
    }

    @AllArgsConstructor
    private static class Replacer implements Serializable {

        private final String plugin;

        Object readResolve() throws ObjectStreamException {
            return new SerializableService(plugin, Resolver.class.getName());
        }
    }

}
