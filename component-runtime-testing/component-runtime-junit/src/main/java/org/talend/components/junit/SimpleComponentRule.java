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
package org.talend.components.junit;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.chain.CountingSuccessListener;
import org.talend.components.runtime.manager.chain.ExecutionChainBuilder;
import org.talend.components.runtime.manager.chain.ToleratingErrorHandler;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleComponentRule implements TestRule {

    static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private final String packageName;

    private final ThreadLocal<PreState> initState = ThreadLocal.withInitial(PreState::new);

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (final EmbeddedComponentManager manager = new EmbeddedComponentManager(packageName)) {
                    STATE.set(new State(manager, new ArrayList<>(), initState.get().emitter));
                    base.evaluate();
                } finally {
                    STATE.remove();
                    initState.remove();
                }
            }
        };
    }

    public <T> List<T> collect(final Class<T> recordType, final String family, final String component, final int version,
            final Map<String, String> configuration) {
        ExecutionChainBuilder.start().withConfiguration("test", true).fromInput(family, component, version, configuration)
                .toProcessor("test", "collector", 1, emptyMap()).create(asManager(), file -> {
                    throw new IllegalArgumentException();
                }, new CountingSuccessListener(), new ToleratingErrorHandler(0)).get().execute();
        return getCollectedData(recordType);
    }

    public <T> void process(final Iterable<T> inputs, final String family, final String component, final int version,
            final Map<String, String> configuration) {
        setInputData(inputs);
        ExecutionChainBuilder.start().withConfiguration("test", true).fromInput("test", "emitter", 1, emptyMap())
                .toProcessor(family, component, version, configuration).create(asManager(), file -> {
                    throw new IllegalArgumentException();
                }, new CountingSuccessListener(), new ToleratingErrorHandler(0)).get().execute();
    }

    public ComponentManager asManager() {
        return STATE.get().manager;
    }

    public <T> void setInputData(final Iterable<T> data) {
        initState.get().emitter = data.iterator();
    }

    public <T> List<T> getCollectedData(final Class<T> recordType) {
        return STATE.get().collector.stream().filter(recordType::isInstance).map(recordType::cast).collect(toList());
    }

    static class PreState {

        Iterator<?> emitter;
    }

    @RequiredArgsConstructor
    static class State {

        final ComponentManager manager;

        final Collection<Object> collector;

        final Iterator<?> emitter;
    }

    private static class EmbeddedComponentManager extends ComponentManager {

        private EmbeddedComponentManager(final String componentPackage) {
            super(findM2(), "TALEND-INF/dependencies.txt", "org.talend.components:type=component,value=%s");
            addJarContaining(Thread.currentThread().getContextClassLoader(), componentPackage.replace('.', '/'));
            container.create("component-runtime-junit.jar", jarLocation(SimpleCollector.class).getAbsolutePath());
        }

        @Override
        protected boolean isContainerClass(final String name) {
            /*
             * return super.isContainerClass(value) || value.startsWith(componentPackage) || value.startsWith("org.junit.")
             * || value.startsWith("org.talend.components.junit");
             */
            return true; // embedded mode (no plugin structure) so just run with all classes in parent classloader
        }
    }

}
