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
package org.talend.sdk.component.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.tools.ComponentHelper.Component;
import org.talend.test.valid.localconfiguration.MyComponent;

class ComponentHelperTest {

    @Test
    void componentMarkers() {
        ComponentHelper.componentMarkers().anyMatch((Class<?> cl) -> PartitionMapper.class.equals(cl));
    }

    @Test
    void findPackageOrFail() {
        try {
            ComponentHelper.findPackageOrFail(ComponentHelper.class, x -> true, "api");
            Assertions.fail("should fail while no .package-info");
        } catch (IllegalArgumentException ex) {
            Assertions
                    .assertTrue(ex
                            .getMessage()
                            .contains(
                                    "No @api for the component class org.talend.sdk.component.tools.ComponentHelper"));
        }

        final Class<?> myPackage = ComponentHelper.findPackageOrFail(MyComponent.class, x -> true, "api");

        Assertions.assertNotNull(myPackage);
        Assertions.assertEquals("org.talend.test.valid.localconfiguration.package-info", myPackage.getName());
    }

    @Test
    void components() {
        final Optional<Component> component = ComponentHelper.components(C1.class);
        Assertions.assertTrue(component.isPresent());
        Assertions.assertEquals("fc1", component.get().family());
        Assertions.assertEquals("c1", component.get().name());

        final Optional<Component> noComponent = ComponentHelper.components(C2.class);
        Assertions.assertFalse(noComponent.isPresent());
    }

    @Test
    void findFamily() {
        final Component component = ComponentHelper.asComponent(C1.class.getAnnotation(Processor.class));
        Assertions.assertEquals("fc1", ComponentHelper.findFamily(component, null));

        Assertions.assertEquals("demo", ComponentHelper.findFamily(null, MyComponent.class));
    }

    @Processor(family = "fc1", name = "c1")
    static class C1 {
    }

    static class C2 {
    }
}