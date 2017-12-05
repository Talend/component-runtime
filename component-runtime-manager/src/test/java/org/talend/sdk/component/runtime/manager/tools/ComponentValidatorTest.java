/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

package org.talend.sdk.component.runtime.manager.tools;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Optional.ofNullable;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import lombok.extern.java.Log;

public class ComponentValidatorTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private final TestName testName = new TestName();

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final TestRule methodRules =
            outerRule(testName).around(expectedException).around((base, description) -> new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    final ComponentPackage config = description.getAnnotation(ComponentPackage.class);
                    final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
                    ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
                    cfg.setValidateFamily(true);
                    cfg.setValidateSerializable(true);
                    cfg.setValidateMetadata(true);
                    cfg.setValidateInternationalization(true);
                    cfg.setValidateDataSet(true);
                    cfg.setValidateActions(true);
                    cfg.setValidateComponent(true);
                    cfg.setValidateModel(true);
                    cfg.setValidateDataStore(true);
                    listPackageClasses(pluginDir, config.value().replace('.', '/'));
                    final Runnable validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());
                    base.evaluate(); // setup the expectations
                    if (!config.success()) {
                        expectedException.expect(IllegalStateException.class);
                    }
                    validator.run();
                }
            });

    @Test
    @ComponentPackage("org.talend.test.failure.action.dynamicvalues")
    public void testFailureActionDynamicValues() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- No @DynamicValues(\"TheValues\"), add a service with this method: @DynamicValues(\"TheValues\") Values proposals();");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.proposal.enumconfig")
    public void testFailureEnumProposal() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- private org.talend.test.failure.proposal.enumconfig.ComponentConfiguredWithEnum$TheEnum org.talend.test.failure.proposal.enumconfig.ComponentConfiguredWithEnum$Foo.value must not define @Proposable since it is an enum");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.action")
    public void testFailureAction() throws IOException {
        expectedException.expectMessage(
                "public java.lang.String org.talend.test.failure.action.MyService.test(org.talend.test.failure.action.MyDataStore) doesn't return a class org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus, please fix it");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.duplicated.dataset")
    public void testFailureDuplicatedDataSet() throws IOException {
        expectedException.expectMessage("Duplicated DataSet found : default");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.duplicated.datastore")
    public void testFailureDuplicatedDataStore() throws IOException {
        expectedException.expectMessage("Duplicated DataStore found : default");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.datastore")
    public void testFailureDataStore() throws IOException {
        expectedException.expectMessage("No @HealthCheck for dataStore: 'default' with checkable: 'default'\n"
                + "- org.talend.test.failure.datastore.MyDataStore2 has @Checkable but is not a @DataStore");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.family")
    public void testFailureFamily() throws IOException {
        expectedException.expectMessage("Some error were detected:\n"
                + "- No resource bundle for org.talend.test.failure.family.MyComponent, you should create a org/talend/test/failure/family/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n")
    public void testFailureI18n() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- org.talend.test.failure.i18n.Messages is missing the key(s): test.my._displayName");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n.custom")
    public void testFailureI18nCustom() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- Key org.talend.test.failure.i18n.custom.MyInternalization.message_wrong from interface org.talend.test.failure.i18n.custom.MyInternalization is no more used\n"
                        + "- Missing key org.talend.test.failure.i18n.custom.MyInternalization.message in interface org.talend.test.failure.i18n.custom.MyInternalization resource bundle");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n.missing")
    public void testFailureI18nMissing() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- No resource bundle for org.talend.test.failure.i18n.missing.MyComponent, you should create a org/talend/test/failure/i18n/missing/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missing.icon")
    public void testFailureMissingIcon() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- Component class org.talend.test.failure.missing.icon.MyComponent should use @Icon and @Version");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missing.version")
    public void testFailureMissingVersion() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- Component class org.talend.test.failure.missing.version.MyComponent should use @Icon and @Version");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.serialization")
    public void testFailureSerialization() throws IOException {
        expectedException.expectMessage(
                "Some error were detected:\n- class org.talend.test.failure.serialization.MyComponent is not Serializable");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.datastore", success = true)
    public void testSucessDataStore() throws IOException {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid", success = true)
    public void testFullValidation() throws IOException {
        // no-op
    }

    // .properties are ok from the classpath, no need to copy them
    private void listPackageClasses(final File pluginDir, final String sourcePackage) throws IOException {
        final File root = new File(jarLocation(getClass()), sourcePackage);
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

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface ComponentPackage {

        String value();

        boolean success() default false;
    }

    @Log
    public static class TestLog implements org.talend.sdk.component.runtime.manager.tools.Log {

        @Override
        public void debug(final String s) {
            log.info(s);
        }

        @Override
        public void error(final String s) {
            log.severe(s);
        }

        @Override
        public void info(final String s) {
            log.info(s);
        }
    }
}
