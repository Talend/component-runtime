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

package org.talend.sdk.component.runtime.manager.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static java.util.Optional.ofNullable;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.fail;

public class ComponentValidatorTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testFailureAction() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateActions(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/action");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
                "public java.lang.String org.talend.test.failure.action.MyService.test(org.talend.test.failure.action.MyDataStore) doesn't return a class org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus, please fix it");
        validator.run();
    }

    @Test
    public void testFailureDuplicatedDataSet() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateDataSet(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/duplicated/dataset");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Duplicated DataSet found : default");
        validator.run();
    }

    @Test
    public void testFailureDuplicatedDataStore() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateDataStore(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/duplicated/datastore");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Duplicated DataStore found : default");
        validator.run();
    }

    @Test
    public void testFailureDataStore() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateDataStore(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/datastore");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("No @HealthCheck for [default] datastores");
        validator.run();
    }

    @Test
    public void testFailureFamily() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateFamily(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/family");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                "No @org.talend.sdk.component.api.component.Icon for the component class org.talend.test.failure.family.MyComponent, add it in package-info.java or disable this validation (which can have side effects in integrations/designers)");
        validator.run();
    }

    @Test
    public void testFailureI18n() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateInternationalization(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/i18n");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Some component internationalization is not complete");
        validator.run();
    }

    @Test
    public void testFailureI18nCustom() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateInternationalization(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/i18n/custom");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
                "interface org.talend.test.failure.i18n.custom.MyInternalization is missing some internalization messages");
        validator.run();
    }

    @Test
    public void testFailureI18nMissing() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateInternationalization(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/i18n/missing");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Some component internationalization is not complete");
        validator.run();
    }

    @Test
    public void testFailureMissingIcon() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateMetadata(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/missing/icon");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                "Component class org.talend.test.failure.missing.icon.MyComponent should use @Icon and @Version");
        validator.run();
    }

    @Test
    public void testFailureMissingVersion() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateMetadata(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/missing/version");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                "Component class org.talend.test.failure.missing.version.MyComponent should use @Icon and @Version");
        validator.run();
    }

    @Test
    public void testFailureSerialization() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateSerializable(true);
        listPackageClasses(pluginDir, "org/talend/test/failure/serialization");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("All components must be serializable for BEAM execution support");
        validator.run();
    }

    @Test
    public void testSucessDataStore() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
        ComponentValidator.Configuration cfg = new ComponentValidator.Configuration();
        cfg.setValidateDataStore(true);
        listPackageClasses(pluginDir, "org/talend/test/valid/datastore");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());

        validator.run();
    }

    @Test
    public void testFullValidation() throws IOException {
        final File pluginDir = new File(TEMPORARY_FOLDER.getRoot() + "/" + testName.getMethodName());
        pluginDir.mkdir();
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
        listPackageClasses(pluginDir, "org/talend/test/valid");
        ComponentValidator validator = new ComponentValidator(cfg, new File[] { pluginDir }, new TestLog());
        validator.run();
    }

    private void listPackageClasses(File pluginDir, String sourcePackage) throws IOException {
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

    public static class TestLog implements ComponentValidator.Log {

        @Override
        public void debug(String s) {
            System.out.println(s);
        }

        @Override
        public void error(String s) {
            System.out.println(s);
        }
    }

}
