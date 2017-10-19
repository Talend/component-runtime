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
package org.talend.components.test.rule;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.stream.Stream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.components.container.ContainerManager;
import org.talend.components.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.components.test.Constants;
import org.talend.components.test.dependencies.DependenciesTxtBuilder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ContainerProviderRule extends TempJars implements TestRule {

    private final Object test;

    private final ThreadLocal<ContainerManager> manager = new ThreadLocal<>();

    @Override
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (final ContainerManager manager = newManager()) {
                    ContainerProviderRule.this.manager.set(manager);
                    Class<?> current = test.getClass();
                    while (current != Object.class && current != null) {
                        Stream.of(current.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Instance.class)).forEach(f -> {
                            if (!f.isAccessible()) {
                                f.setAccessible(true);
                            }
                            final DependenciesTxtBuilder builder = new DependenciesTxtBuilder();
                            final String[] deps = f.getAnnotation(Instance.class).value();
                            Stream.of(deps).forEach(builder::withDependency);
                            try {
                                f.set(test, manager.create(
                                        description.getClassName() + "." + description.getMethodName() + "#" + f.getName(),
                                        create(builder.build()).getAbsolutePath()));
                            } catch (final IllegalAccessException e) {
                                throw new IllegalArgumentException(e);
                            }
                        });
                        current = current.getSuperclass();
                    }

                    statement.evaluate();
                } finally {
                    after();
                    ContainerProviderRule.this.manager.remove();
                }
            }
        };
    }

    public ContainerManager current() {
        return manager.get();
    }

    protected ContainerManager newManager() {
        return new ContainerManager(
                ContainerManager.DependenciesResolutionConfiguration.builder()
                        .resolver(new MvnDependencyListLocalRepositoryResolver(Constants.DEPENDENCIES_LIST_RESOURCE_PATH))
                        .rootRepositoryLocation(new File(Constants.DEPENDENCIES_LOCATION)).create(),
                ContainerManager.ClassLoaderConfiguration.builder().parent(ContainerProviderRule.class.getClassLoader())
                        .create());
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Instance {

        // dependencies
        String[] value();
    }
}
