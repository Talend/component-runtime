/*
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

package org.talend.sdk.component.tools;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Optional.ofNullable;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ExtendWith(ComponentValidatorTest.ValidatorExtension.class)
class ComponentValidatorTest {

    @Data
    static class ExceptionSpec {

        private String message = "";

        void expectMessage(final String message) {
            this.message = message;
        }
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface ComponentPackage {

        String value();

        boolean success() default false;

        boolean validateDocumentation() default false;
    }

    @Slf4j
    public static class TestLog implements Log {

        @Override
        public void debug(final String s) {
            log.info(s);
        }

        @Override
        public void error(final String s) {
            log.error(s);
        }

        @Override
        public void info(final String s) {
            log.info(s);
        }
    }

    public static class ValidatorExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback,
            AfterAllCallback, JUnit5InjectionSupport {

        private static final ExtensionContext.Namespace NAMESPACE =
                ExtensionContext.Namespace.create(ValidatorExtension.class.getName());

        @Override
        public void beforeAll(final ExtensionContext context) throws Exception {
            final TemporaryFolder temporaryFolder = new TemporaryFolder();
            context.getStore(NAMESPACE).put(TemporaryFolder.class.getName(), temporaryFolder);
            temporaryFolder.create();
        }

        @Override
        public void afterAll(final ExtensionContext context) {
            TemporaryFolder.class.cast(context.getStore(NAMESPACE).get(TemporaryFolder.class.getName())).delete();
        }

        @Override
        public void beforeEach(final ExtensionContext context) {
            final ComponentPackage config = context.getElement().get().getAnnotation(ComponentPackage.class);
            final ExtensionContext.Store store = context.getStore(NAMESPACE);
            final File pluginDir =
                    new File(TemporaryFolder.class.cast(store.get(TemporaryFolder.class.getName())).getRoot() + "/"
                            + context.getRequiredTestMethod().getName());
            final ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
            cfg.setValidateFamily(true);
            cfg.setValidateSerializable(true);
            cfg.setValidateMetadata(true);
            cfg.setValidateInternationalization(true);
            cfg.setValidateDataSet(true);
            cfg.setValidateActions(true);
            cfg.setValidateComponent(true);
            cfg.setValidateModel(true);
            cfg.setValidateDataStore(true);
            cfg.setValidateDocumentation(config.validateDocumentation());
            listPackageClasses(pluginDir, config.value().replace('.', '/'));
            store.put(ComponentPackage.class.getName(), config);
            store.put(ComponentValidator.class.getName(),
                    new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog()));
            store.put(ExceptionSpec.class.getName(), new ExceptionSpec());
        }

        @Override
        public void afterEach(final ExtensionContext context) {
            final ExtensionContext.Store store = context.getStore(NAMESPACE);
            final boolean fails = !ComponentPackage.class.cast(store.get(ComponentPackage.class.getName())).success();
            try {
                ComponentValidator.class.cast(store.get(ComponentValidator.class.getName())).run();
                if (fails) {
                    fail("should have failed");
                }
            } catch (final IllegalStateException ise) {
                if (fails) {
                    assertTrue(ise
                            .getMessage()
                            .contains(ExceptionSpec.class
                                    .cast(context.getStore(NAMESPACE).get(ExceptionSpec.class.getName()))
                                    .getMessage()));
                } else {
                    fail(ise);
                }
            }
        }

        @Override
        public Object findInstance(final ExtensionContext extensionContext, final Class<?> type) {
            return extensionContext.getStore(NAMESPACE).get(type.getName());
        }

        @Override
        public boolean supports(final Class<?> type) {
            return ExceptionSpec.class == type;
        }

        @Override
        public Class<? extends Annotation> injectionMarker() {
            return Inject.class; // skip
        }
    }

    @Test
    @ComponentPackage("org.talend.test.failure.action.dynamicvalues")
    void testFailureActionDynamicValues(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- No @DynamicValues(\"TheValues\"), add a service with this method: @DynamicValues(\"TheValues\") Values proposals();");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.proposal.enumconfig")
    void testFailureEnumProposal(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- private org.talend.test.failure.proposal.enumconfig.ComponentConfiguredWithEnum$TheEnum org.talend.test.failure.proposal.enumconfig.ComponentConfiguredWithEnum$Foo.value must not define @Proposable since it is an enum");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.action")
    void testFailureAction(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "public java.lang.String org.talend.test.failure.action.MyService.test(org.talend.test.failure.action.MyDataStore) doesn't return a class org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus, please fix it");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.duplicated.dataset")
    void testFailureDuplicatedDataSet(final ExceptionSpec expectedException) {
        expectedException.expectMessage("Duplicated DataSet found : default");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.duplicated.datastore")
    void testFailureDuplicatedDataStore(final ExceptionSpec expectedException) {
        expectedException.expectMessage("Duplicated DataStore found : default");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.datastore")
    void testFailureDataStore(final ExceptionSpec expectedException) {
        expectedException.expectMessage("No @HealthCheck for dataStore: 'default' with checkable: 'default'\n"
                + "- org.talend.test.failure.datastore.MyDataStore2 has @Checkable but is not a @DataStore");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.family")
    void testFailureFamily(final ExceptionSpec expectedException) {
        expectedException.expectMessage("Some error were detected:\n"
                + "- No resource bundle for org.talend.test.failure.family.MyComponent, you should create a org/talend/test/failure/family/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n")
    void testFailureI18n(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- org.talend.test.failure.i18n.Messages is missing the key(s): test.my._displayName");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n.custom")
    void testFailureI18nCustom(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- Key org.talend.test.failure.i18n.custom.MyInternalization.message_wrong from interface org.talend.test.failure.i18n.custom.MyInternalization is no more used\n"
                        + "- Missing key org.talend.test.failure.i18n.custom.MyInternalization.message in interface org.talend.test.failure.i18n.custom.MyInternalization resource bundle");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n.missing")
    void testFailureI18nMissing(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- No resource bundle for org.talend.test.failure.i18n.missing.MyComponent, you should create a org/talend/test/failure/i18n/missing/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missing.icon")
    void testFailureMissingIcon(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- Component class org.talend.test.failure.missing.icon.MyComponent should use @Icon and @Version");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missing.version")
    void testFailureMissingVersion(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- Component class org.talend.test.failure.missing.version.MyComponent should use @Icon and @Version");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.serialization")
    void testFailureSerialization(final ExceptionSpec expectedException) {
        expectedException.expectMessage(
                "Some error were detected:\n- class org.talend.test.failure.serialization.MyComponent is not Serializable");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.documentation.component", validateDocumentation = true)
    void testFailureDocumentationComponent(final ExceptionSpec expectedException) {
        expectedException.expectMessage("Some error were detected:\n"
                + "- No @Documentation on 'org.talend.test.failure.documentation.component.MyComponent'");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.documentation.option", validateDocumentation = true)
    void testFailureDocumentationOption(final ExceptionSpec expectedException) {
        expectedException.expectMessage("Some error were detected:\n"
                + "- No @Documentation on 'private java.lang.String org.talend.test.failure.documentation.option.MyComponent$MyConfig.input'");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.datastore", success = true)
    void testSucessDataStore() {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid", success = true, validateDocumentation = true)
    void testFullValidation() {
        // no-op
    }

    // .properties are ok from the classpath, no need to copy them
    private static void listPackageClasses(final File pluginDir, final String sourcePackage) {
        final File root = new File(jarLocation(ComponentValidatorTest.class), sourcePackage);
        File classDir = new File(pluginDir, sourcePackage);
        classDir.mkdirs();
        ofNullable(root.listFiles())
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .filter(c -> c.getName().endsWith(".class"))
                .forEach(c -> {
                    try {
                        Files.copy(c.toPath(), new File(classDir, c.getName()).toPath());
                    } catch (IOException e) {
                        fail("cant create test plugin");
                    }
                });
    }
}
