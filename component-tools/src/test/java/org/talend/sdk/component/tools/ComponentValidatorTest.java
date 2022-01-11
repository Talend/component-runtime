/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

        String pluginId() default "";

        boolean success() default false;

        boolean validateDocumentation() default false;

        boolean validateDataSet() default true;

        boolean validateWording() default false;

        boolean validateSvg() default true;

        boolean validateExceptions() default false;

        boolean failOnValidateExceptions() default true;

        String sourceRoot() default "";
    }

    @Slf4j
    public static class TestLog implements Log {

        private final Collection<String> messages = new ArrayList<>();

        @Override
        public void debug(final String s) {
            log.info(s);
            messages.add("[DEBUG] " + s);
        }

        @Override
        public void error(final String s) {
            log.error(s);
            messages.add("[ERROR] " + s);
        }

        @Override
        public void info(final String s) {
            log.info(s);
            messages.add("[INFO] " + s);
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
            cfg.setValidateDataSet(config.validateDataSet());
            cfg.setValidateActions(true);
            cfg.setValidateComponent(true);
            cfg.setValidateModel(true);
            cfg.setValidateDataStore(true);
            cfg.setValidateLayout(true);
            cfg.setValidateOptionNames(true);
            cfg.setValidateLocalConfiguration(true);
            cfg.setValidateOutputConnection(true);
            cfg.setValidatePlaceholder(true);
            cfg.setValidateSvg(config.validateSvg());
            cfg.setValidateNoFinalOption(true);
            cfg.setValidateDocumentation(config.validateDocumentation());
            cfg.setValidateWording(config.validateWording());
            cfg.setValidateExceptions(config.validateExceptions());
            cfg.setFailOnValidateExceptions(config.failOnValidateExceptions());
            Optional.of(config.pluginId()).filter(it -> !it.isEmpty()).ifPresent(cfg::setPluginId);
            listPackageClasses(pluginDir, config.value().replace('.', '/'));
            store.put(ComponentPackage.class.getName(), config);
            final TestLog log = new TestLog();
            store.put(TestLog.class.getName(), log);
            store.put(ComponentValidator.class.getName(), new ComponentValidator(cfg, new File[] { pluginDir }, log));
            store.put(ExceptionSpec.class.getName(), new ExceptionSpec());
        }

        @Override
        public void afterEach(final ExtensionContext context) {
            final ExtensionContext.Store store = context.getStore(NAMESPACE);
            final boolean fails = !ComponentPackage.class.cast(store.get(ComponentPackage.class.getName())).success();
            final String expectedMessage = ExceptionSpec.class
                    .cast(context.getStore(NAMESPACE).get(ExceptionSpec.class.getName()))
                    .getMessage();
            try {
                ComponentValidator.class.cast(store.get(ComponentValidator.class.getName())).run();
                if (fails) {
                    fail("should have failed");
                }
                if (expectedMessage != null) {
                    final Collection<String> messages = TestLog.class.cast(store.get(TestLog.class.getName())).messages;
                    assertTrue(messages.stream().anyMatch(it -> it.contains(expectedMessage)),
                            expectedMessage + "\n\n> " + messages);
                }
            } catch (final IllegalStateException ise) {
                if (fails) {
                    final String exMsg = ise.getMessage();
                    assertTrue(exMsg.contains(expectedMessage), expectedMessage + "\n\n> " + exMsg);
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
    @ComponentPackage(value = "org.talend.test.failure.exceptions", validateExceptions = true)
    void testFailureException(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "org.talend.test.failure.exceptions.InvalidComponentException inherits from ComponentException, this will lead to ClassNotFound errors in some environments. Use instead ComponentException directly!");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.exceptions", validateExceptions = true, success = true)
    void testValidException() {
        //
    }

    @Test
    @ComponentPackage("org.talend.test.failure.options.badname")
    void testBadOptionName(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- Option name `$forbidden1` is invalid, you can't start an option name with a '$' and it can't contain a '.'. Please fix it on field `org.talend.test.failure.options.badname.BadConfig#forbidden1`\n"
                                + "- Option name `forbidden1.name` is invalid, you can't start an option name with a '$' and it can't contain a '.'. Please fix it on field `org.talend.test.failure.options.badname.BadConfig#forbidden2`");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.svgerror")
    void testFailureSvg(final ExceptionSpec expectedException) {
        expectedException.expectMessage("[myicon.svg] viewBox must be '0 0 16 16' found '0 0 16 17'");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.svgerror", validateSvg = false, success = true)
    void testIgnoreSvg(final ExceptionSpec expectedException) {
        //
    }

    @Test
    @ComponentPackage("org.talend.test.failure.enumi18n")
    void testFailureEnumi18n(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Missing key Foo.B._displayName in class org.talend.test.failure.enumi18n.MyComponent$Foo resource bundle");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.action.dynamicvalues", validateDataSet = false)
    void testFailureActionDynamicValues(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Some error were detected:\n- No @DynamicValues(\"TheValues\"), add a service with this method: @DynamicValues(\"TheValues\") Values proposals();");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.proposal.enumconfig")
    void testFailureEnumProposal(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- private org.talend.test.failure.proposal.enumconfig.ComponentConfiguredWithEnum$TheEnum org.talend.test.failure.proposal.enumconfig.ComponentConfiguredWithEnum$Foo.value must not define @Proposable since it is an enum");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.action")
    void testFailureAction(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "public String org.talend.test.failure.action.MyService.test(org.talend.test.failure.action.MyDataStore) doesn't return a class org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus, please fix it");
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
        expectedException
                .expectMessage(
                        "- org.talend.test.failure.datastore.MyDataStore2 has @Checkable but is not a @DataStore\n"
                                + "- No @HealthCheck for dataStore: 'default' with checkable: 'default'");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.family")
    void testFailureFamily(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- No resource bundle for org.talend.test.failure.family.MyComponent, you should create a org/talend/test/failure/family/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n")
    void testFailureI18n(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Some error were detected:\n- org.talend.test.failure.i18n.Messages is missing the key(s): test.my._displayName");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n.custom")
    void testFailureI18nCustom(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("Some error were detected:\n"
                        + "- Missing key org.talend.test.failure.i18n.custom.MyInternalization.message in interface org.talend.test.failure.i18n.custom.MyInternalization resource bundle\n"
                        + "- Key org.talend.test.failure.i18n.custom.MyInternalization.message_wrong from interface org.talend.test.failure.i18n.custom.MyInternalization is no more used");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.i18n.missing")
    void testFailureI18nMissing(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Some error were detected:\n- No resource bundle for org.talend.test.failure.i18n.missing.MyComponent, you should create a org/talend/test/failure/i18n/missing/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missing.icon")
    void testFailureMissingIcon(final ExceptionSpec expectedException) {
        expectedException.expectMessage("- No @Icon on class org.talend.test.failure.missing.icon.MyComponent");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missing.version")
    void testFailureMissingVersion(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Some error were detected:\n- Component class org.talend.test.failure.missing.version.MyComponent should use @Icon and @Version");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.serialization")
    void testFailureSerialization(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Some error were detected:\n- class org.talend.test.failure.serialization.MyComponent is not Serializable");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.documentation.component", validateDocumentation = true,
            validateDataSet = false)
    void testFailureDocumentationComponent(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("Some error were detected:\n"
                        + "- No @Documentation on 'org.talend.test.failure.documentation.component.MyComponent'");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.wording.component", validateDocumentation = true,
            validateDataSet = false, validateWording = true)
    void testFailureDocumentationWordingComponent(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("Some error were detected:\n"
                        + "- @Documentation on 'org.talend.test.failure.wording.component.MyComponent' is empty or is"
                        + " not capitalized or ends not by a dot.");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.wording.option", validateDocumentation = true,
            validateDataSet = false, validateWording = true)
    void testFailureDocumentationWordingOption(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("Some error were detected:\n" + "- @Documentation on 'empty' is empty or is"
                        + " not capitalized or ends not by a dot.\n- @Documentation on 'input' is empty or is not capitalized or ends not by a dot");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.wording", validateDocumentation = true, validateDataSet = false,
            validateWording = true, success = true)
    void testValidDocumentationWordingComponent(final ExceptionSpec expectedException) {
        //
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.documentation.option", validateDocumentation = true,
            validateDataSet = false)
    void testFailureDocumentationOption(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("Some error were detected:\n"
                        + "- No @Documentation on 'private String org.talend.test.failure.documentation.option.MyComponent$MyConfig.input'");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.layout", validateDataSet = false)
    void testFailureLayoutOption(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("Some error were detected:\n"
                        + "- Option 'badOption' in @OptionOrder doesn't exist in declaring class 'org.talend.test.failure.layout.MyComponent$MyNestedConfig'\n"
                        + "- Option 'badOption' in @GridLayout doesn't exist in declaring class 'org.talend.test.failure.layout.MyComponent$MyConfig'\n"
                        + "- Option 'proxy' in @GridLayout doesn't exist in declaring class 'org.talend.test.failure.layout.MyComponent$OtherConfig'");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missingaction18n")
    void testMissingActionI18n(final ExceptionSpec spec) {
        spec
                .expectMessage("org.talend.test.failure.missingaction18n.Messages is missing the key(s): "
                        + "demo.actions.healthcheck.default._displayName");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.customicon")
    void testFailureCustomIcon(final ExceptionSpec spec) {
        spec.expectMessage("Some error were detected:\n- No icon: 'missing' found");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.customiconapi", success = true)
    void testValidCustomIconAPI(final ExceptionSpec spec) {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.customicon", success = true)
    void testValidCustomIcon(final ExceptionSpec spec) {
        // jus a warning so this test is semantically "valid" but we still assert this message
        spec.expectMessage("icons/present.svg' found, this will run in degraded mode in Talend Cloud");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.datastore", success = true)
    void testSuccessDataStore() {
        // no-op
    }

    @Test
    @ComponentPackage("org.talend.test.failure.multiplerootconfig")
    void testFailureDuplicatedRootConfiguration(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "Component must use a single root option. 'org.talend.test.failure.multiplerootconfig.MyComponent'");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.datasetprocessornosource")
    void testFailureDataSetNoSource(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- No source instantiable without adding parameters for @DataSet(\"dataset\") (org.talend.test"
                                + ".failure.datasetprocessornosource.MyComponent$MyDataSet), please ensure at least a source using this "
                                + "dataset can be used just filling the dataset information.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.datasetrequiredinsource")
    void testFailureDataSetSourceHasRequired(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- No source instantiable without adding parameters for @DataSet(\"dataset\") (org.talend.test"
                                + ".failure.datasetrequiredinsource.MyComponent$MyDataSet), please ensure at least a source using this "
                                + "dataset can be used just filling the dataset information.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.missingplaceholder")
    void testFailureMissingPlaceholder(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "No Foo.missingPlaceholder._placeholder set for foo.missingPlaceholder in Messages.properties of packages: "
                                + "[org.talend.test.failure.missingplaceholder]");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.nesteddataset", success = true)
    void testValidDataSetHasInstantiableSource() {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.nestedconfigtypes", success = true)
    void testValidNestedConfigTypes() {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.datasetassourceconfig", success = true)
    void testValidDataSetIsSourceConfig() {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.datasetintwosourceswithonewithadditionalrequired", success = true)
    void testValidDataSetIfASourceIsAvailableEvenIfAnotherOneHasRequireds() {
        // no-op
    }

    @Test
    @ComponentPackage("org.talend.test.failure.inputmissingdataset")
    void testFailureMissingDataSetInInput(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("- The component org.talend.test.failure.inputmissingdataset.MyComponent "
                        + "is missing a dataset in its configuration (see @DataSet)");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.outputmissingdataset")
    void testFailureMissingDataSetInOutput(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("- The component org.talend.test.failure.outputmissingdataset.MyComponent "
                        + "is missing a dataset in its configuration (see @DataSet)");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.noi18ndatastorewithbundle")
    void testFailureI18nDatastoreWithBundle(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("- org.talend.test.failure.noi18ndatastorewithbundle.Messages is missing the key(s): "
                        + "demo.datastore.default._displayName");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.noi18ndatastore")
    void testFailureI18nDatastore(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- No resource bundle for org.talend.test.failure.noi18ndatastore.MyDataStore translations, "
                                + "you should create a org/talend/test/failure/noi18ndatastore/Messages.properties at least.");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.aftergroup")
    void testFailureAfterGroup(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("- @Output parameter must be of type OutputEmitter\n"
                        + "- Parameter of AfterGroup method need to be annotated with Output");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.localconfigurationwrongkey", pluginId = "test")
    void testFailureLocalConfigurationKey(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage(
                        "- 'demo.conf.key' does not start with 'test', it is recommended to prefix all keys by the family");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.failure.finaloption", pluginId = "test")
    void testFailureFinalOption(final ExceptionSpec expectedException) {
        expectedException
                .expectMessage("@Option fields must not be final, found one field violating this rule: "
                        + "private final String org.talend.test.failure.finaloption.MyComponent$MyConfig.input");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.localconfiguration", success = true, validateDataSet = false,
            pluginId = "test")
    void testValidLocalConfigurationKey() {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid", success = true, validateDocumentation = true)
    void testFullValidation() {
        // no-op
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.structure", success = true, validateSvg = false)
    void testValidStructure() {
        // no-op
    }

    @Test
    @ComponentPackage("org.talend.test.failure.structure")
    void testFailureStructure(final ExceptionSpec spec) {
        spec
                .expectMessage(
                        "- class org.talend.test.failure.structure.MyComponentWithStructure$MyDataSet#incoming uses @Structure but is not a List<String> nor a List<Object>\n"
                                + "- class org.talend.test.failure.structure.MyComponentWithStructure$MyDataSet#outgoing uses @Structure but is not a List<String> nor a List<Object>");
    }

    @Test
    @ComponentPackage(value = "org.talend.test.valid.update", success = true, validateDataSet = false)
    void testValidUpdate() {
        // no-op
    }

    @Test
    @ComponentPackage("org.talend.test.failure.noupdatematching")
    void testFailureUpdateMatching(final ExceptionSpec spec) {
        spec
                .expectMessage("- No @Update service found for field "
                        + "private org.talend.test.failure.noupdatematching.Model org.talend.test.failure.noupdatematching.Config.model, "
                        + "did you intend to use @Updatable?");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.updatableafter")
    void testFailureUpdateAfter(final ExceptionSpec spec) {
        spec.expectMessage("- @Updatable.after should only reference direct child primitive fields");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.updatebadtype")
    void testFailureUpdateBadType(final ExceptionSpec spec) {
        spec
                .expectMessage("- @Updatable field 'private org.talend.test.failure.updatebadtype.Model "
                        + "org.talend.test.failure.updatebadtype.Config.model' does not match returned type of "
                        + "'String org.talend.test.failure.updatebadtype.UpdateService.update()'");
    }

    @Test
    @ComponentPackage("org.talend.test.failure.multipleinputsforoutput")
    void testFailureMultipleInputsForOutputs(final ExceptionSpec spec) {
        spec
                .expectMessage(
                        "- The Output component 'class org.talend.test.failure.multipleinputsforoutput.MyComponent' must have only one single input branch parameter in its ElementListener method.");
    }

    // .properties are ok from the classpath, no need to copy them
    private static void listPackageClasses(final File pluginDir, final String sourcePackage) {
        final File root = new File(jarLocation(ComponentValidatorTest.class), sourcePackage);
        final File classDir = new File(pluginDir, sourcePackage);
        classDir.mkdirs();
        files(root).filter(c -> c.getName().endsWith(".class") || c.getName().endsWith(".properties")).forEach(c -> {
            try {
                Files.copy(c.toPath(), new File(classDir, c.getName()).toPath());
            } catch (final IOException e) {
                fail("cant create test plugin");
            }
        });
        final String localConfRelativePath = "TALEND-INF/local-configuration.properties";
        final File localConfig = new File(root, localConfRelativePath);
        if (localConfig.exists()) {
            final File output = new File(pluginDir, localConfRelativePath);
            output.getParentFile().mkdirs();
            try {
                Files.copy(localConfig.toPath(), output.toPath());
            } catch (final IOException e) {
                fail("cant create test plugin: " + e.getMessage());
            }
        }
        final File icons = new File(root, "icons");
        if (icons.exists()) {
            new File(pluginDir, "icons").mkdirs();
            files(icons).filter(c -> c.getName().endsWith(".png") || c.getName().endsWith(".svg")).forEach(c -> {
                try {
                    Files.copy(c.toPath(), new File(pluginDir, "icons/" + c.getName()).toPath());
                } catch (final IOException e) {
                    fail("cant create test plugin: " + e.getMessage());
                }
            });
        }
    }

    private static Stream<File> files(final File root) {
        return ofNullable(root.listFiles()).map(Stream::of).orElseGet(Stream::empty);
    }
}
