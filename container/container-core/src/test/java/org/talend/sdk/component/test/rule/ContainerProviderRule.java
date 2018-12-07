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
package org.talend.sdk.component.test.rule;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.test.Constants;
import org.talend.sdk.component.test.dependencies.DependenciesTxtBuilder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ContainerProviderRule extends TempJars implements BeforeAllCallback, JUnit5InjectionSupport {

    private final ThreadLocal<ContainerManager> manager = new ThreadLocal<>();

    @Override
    public boolean supports(final Class<?> type) {
        return super.supports(type) || type == Container.class;
    }

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return Instance.class;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (super.supports(parameterContext.getParameter().getType())) {
            return super.findInstance(extensionContext, parameterContext.getParameter().getType());
        }
        final DependenciesTxtBuilder builder = new DependenciesTxtBuilder();
        final String[] deps = parameterContext.getParameter().getAnnotation(Instance.class).value();
        Stream.of(deps).forEach(builder::withDependency);
        return manager
                .get()
                .builder(extensionContext.getRequiredTestClass().getName() + "."
                        + extensionContext.getRequiredTestMethod().getName() + "#" + manager.get().findAll().size(),
                        create(builder.build()).getAbsolutePath())
                .create();
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        this.manager.set(newManager());
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        super.afterAll(context);
        this.manager.remove();
    }

    public ContainerManager current() {
        return manager.get();
    }

    protected ContainerManager newManager() {
        return new ContainerManager(
                ContainerManager.DependenciesResolutionConfiguration
                        .builder()
                        .resolver(new MvnDependencyListLocalRepositoryResolver(
                                Constants.DEPENDENCIES_LIST_RESOURCE_PATH, this::resolve))
                        .rootRepositoryLocation(new File(Constants.DEPENDENCIES_LOCATION))
                        .create(),
                ContainerManager.ClassLoaderConfiguration
                        .builder()
                        .parent(ContainerProviderRule.class.getClassLoader())
                        .create(),
                null, Level.INFO);
    }

    private File resolve(final String artifact) {
        return current().resolve(artifact);
    }

    @Target(PARAMETER)
    @Retention(RUNTIME)
    public @interface Instance {

        // dependencies
        String[] value();
    }
}
