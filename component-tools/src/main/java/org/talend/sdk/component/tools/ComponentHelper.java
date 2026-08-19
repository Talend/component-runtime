/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.standalone.DriverRunner;

import lombok.RequiredArgsConstructor;

public class ComponentHelper {

    public static Stream<Class<? extends Annotation>> componentMarkers() {
        return Stream.of(PartitionMapper.class, Processor.class, Emitter.class, DriverRunner.class);
    }

    public static Class<?> findPackageOrFail(final Class<?> component, final Predicate<Class<?>> tester,
            final String api) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final String pck = component.getPackage() == null ? null : component.getPackage().getName();
        if (pck != null) {
            String currentPackage = pck;
            do {
                try {
                    final Class<?> pckInfo = loader.loadClass(currentPackage + ".package-info");
                    if (tester.test(pckInfo)) {
                        return pckInfo;
                    }
                } catch (final ClassNotFoundException e) {
                    // no-op
                }

                final int endPreviousPackage = currentPackage.lastIndexOf('.');
                if (endPreviousPackage < 0) { // we don't accept default package since it is not specific enough
                    break;
                }

                currentPackage = currentPackage.substring(0, endPreviousPackage);
            } while (true);
        }
        throw new IllegalArgumentException("No @" + api + " for the component " + component
                + ", add it in package-info.java or disable this validation"
                + " (which can have side effects in integrations/designers)");
    }

    public static Optional<Component> components(final Class<?> component) {
        return ComponentHelper
                .componentMarkers()
                .map(component::getAnnotation)
                .filter(Objects::nonNull)
                .findFirst()
                .map(ComponentHelper::asComponent);
    }

    public static String findFamily(final Component c, final Class<?> pckMarker) {
        return ofNullable(c)
                .map(Component::family)
                .filter(name -> !name.isEmpty())
                .orElseGet(() -> ComponentHelper
                        .findPackageOrFail(pckMarker, (Class<?> clazz) -> clazz.isAnnotationPresent(Components.class),
                                Components.class.getName())
                        .getAnnotation(Components.class)
                        .family());
    }

    public static Component asComponent(final Annotation a) {
        final String family = ComponentHelper.get(a, "family");
        final String name = ComponentHelper.get(a, "name");

        return new Component(family, name);
    }

    private static String get(final Annotation a, final String methodName) {
        try {
            final Method getter = a.annotationType().getMethod(methodName);
            return (String) getter.invoke(a);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Can' find " + methodName + " on annotation " + a.annotationType().getName(),
                    ex);
        }
    }

    @RequiredArgsConstructor
    public static class Component {

        private final String family;

        private final String name;

        public String family() {
            return family;
        }

        public String name() {
            return name;
        }
    }
}
