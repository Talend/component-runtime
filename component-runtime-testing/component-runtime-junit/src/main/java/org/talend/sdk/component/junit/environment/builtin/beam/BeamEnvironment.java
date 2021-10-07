/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.environment.builtin.beam;

import static org.jboss.shrinkwrap.resolver.api.maven.ScopeType.RUNTIME;
import static org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies.createDependency;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;
import org.talend.sdk.component.junit.environment.ClassLoaderEnvironment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BeamEnvironment extends ClassLoaderEnvironment {

    private String beamVersion;

    private String kitVersion;

    @Override
    protected AutoCloseable doStart(final Class<?> clazz, final Annotation[] annotations) {
        beamVersion = System.getProperty("talend.junit.beam.version", Versions.BEAM_VERSION);
        kitVersion = System.getProperty("talend.junit.kit.version", Versions.KIT_VERSION);
        resetBeamCache(false);
        final AutoCloseable delegate = super.doStart(clazz, annotations);
        return () -> {
            resetBeamCache(true);
            delegate.close();
        };
    }

    private void resetBeamCache(final boolean highLevelLog) {
        try { // until resetCache() is part of beam do it the hard way
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final Class<?> pof = loader.loadClass("org.apache.beam.sdk.options.PipelineOptionsFactory");

            final Method initializeRegistry = pof.getDeclaredMethod("resetCache");
            initializeRegistry.setAccessible(true);
            initializeRegistry.invoke(null);
        } catch (final NoClassDefFoundError | Exception ex) {
            final Consumer<String> logger = highLevelLog ? log::warn : log::debug;
            logger.accept(ex.getMessage());
        }
    }

    @Override
    protected MavenDependency[] rootDependencies() {
        return new MavenDependency[] { getRunnerDependency(), getComponentRuntimeBeamDependency() };
    }

    protected MavenDependency getRunnerDependency() {
        return createDependency(rootDependencyBase() + ":jar:" + beamVersion, RUNTIME, false);
    }

    protected MavenDependency getComponentRuntimeBeamDependency() {
        return createDependency("org.talend.sdk.component:component-runtime-beam:jar:" + kitVersion, RUNTIME, false);
    }

    @Override
    public String getName() {
        return super.getName().replace("Runner", "");
    }

    protected abstract String rootDependencyBase();
}
