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
package org.talend.sdk.component.server.test;

import static java.util.Optional.ofNullable;
import static org.apache.xbean.asm9.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm9.ClassWriter.COMPUTE_FRAMES;
import static org.apache.xbean.asm9.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm9.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm9.Opcodes.ALOAD;
import static org.apache.xbean.asm9.Opcodes.ARETURN;
import static org.apache.xbean.asm9.Opcodes.DUP;
import static org.apache.xbean.asm9.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm9.Opcodes.NEW;
import static org.apache.xbean.asm9.Opcodes.RETURN;
import static org.apache.xbean.asm9.Opcodes.V1_8;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.json.bind.config.PropertyOrderStrategy;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.configuration.Configuration;
import org.apache.xbean.asm9.AnnotationVisitor;
import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.commons.ClassRemapper;
import org.apache.xbean.asm9.commons.Remapper;
import org.apache.ziplock.Files;
import org.apache.ziplock.IO;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

// create a test m2 repo and setup the server configuration to ensure components are found
public class InitTestInfra implements Meecrowave.ConfigurationCustomizer {

    @Override
    public void accept(final Configuration builder) {
        builder.setJsonbOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL);

        if (Boolean.getBoolean("org.talend.sdk.component.server.test.InitTestInfra.skip")) {
            return;
        }
        Locale.setDefault(Locale.ENGLISH);
        builder.setJsonbPrettify(true);
        builder
                .setTempDir(new File(jarLocation(InitTestInfra.class).getParentFile(), getClass().getSimpleName())
                        .getAbsolutePath());
        System.setProperty("component.manager.classpath.skip", "true");
        System.setProperty("component.manager.callers.skip", "true");
        System.setProperty("talend.component.server.maven.repository", createM2(builder.getTempDir()));
        System
                .setProperty("talend.component.server.component.documentation.translations",
                        createI18nDocRepo(builder.getTempDir()));
        System.setProperty("talend.component.server.user.extensions.location", createUserJars(builder.getTempDir()));
        System
                .setProperty("talend.component.server.icon.paths",
                        "icons/%s.svg,icons/svg/%s.svg,%s.svg,%s_icon32.png,icons/%s_icon32.png,icons/png/%s_icon32.png");
        System.setProperty("talend.component.server.locale.mapping", "en*=en\nfr*=fr\ntest=te\nde*=de");
        System.setProperty("talend.component.server.gridlayout.translation.support", "true");

        final String skipLogs = System.getProperty("component.server.test.logging.skip", "true");
        System.setProperty("talend.component.server.request.log", Boolean.toString("false".equals(skipLogs)));

        //
        System.setProperty("talend.component.server.plugins.reloading.active", "true");
        System.setProperty("talend.component.server.plugins.reloading.interval", "5");
        System.setProperty("talend.component.server.plugins.reloading.method", "connectors");
        System
                .setProperty("talend.component.server.plugins.reloading.marker",
                        "target/InitTestInfra/.m2/repository/CONNECTORS_VERSION");

    }

    private String createUserJars(final String tempDir) {
        final File base = new File(tempDir, "/user-extensions/component-with-user-jars");
        base.mkdirs();
        try (final FileWriter writer = new FileWriter(new File(base, "user-configuration.properties"))) {
            writer.write("i.m.a.virtual.configuration.entry = Yes I am!\n");
            writer.write("i.m.another.virtual.configuration.entry = Yes I am too!");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        final Manifest man = new Manifest();
        man.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        man.getMainAttributes().putValue("Created-By", "Talend Component Kit Server tests");
        try (final JarOutputStream jar =
                new JarOutputStream(new FileOutputStream(new File(base, "user-extension.jar")), man)) {
            jar
                    .putNextEntry(new JarEntry(
                            "TALEND-INF/org.talend.sdk.component.server.test.custom.CustomService.properties"));
            jar.write("i.am = in the classpath\nand.was.added = by the user".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return base.getParentFile().getAbsolutePath();
    }

    private String createI18nDocRepo(final String tempDir) {
        final File base = new File(tempDir, "i18n-doc");
        base.mkdirs();
        try (final FileWriter writer = new FileWriter(new File(base, "documentation_the-test-component_te.adoc"))) {
            writer.write("== Input\n\nSome Input Overriden For Test locale\n\n=== Configuration\n\nblabla\n");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return base.getAbsolutePath();
    }

    private String createM2(final String tempDir) {
        // reusing tempDir we don't need to delete it, done by meecrowave
        // reusing tempDir we don't need to delete it, done by meecrowave
        final File m2 = new File(tempDir, ".m2/repository");

        try {
            final Path conn = m2.toPath().resolve("CONNECTORS_VERSION");
            if (!conn.toFile().exists()) {
                conn.toFile().getParentFile().mkdirs();
                java.nio.file.Files.write(conn, "1.2.3".getBytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        final PluginGenerator generator = new PluginGenerator();

        {
            final String groupId = "org.talend.test1";
            final String artifactId = "the-test-component";
            final String version = "1.2.6";
            createComponent(m2, groupId, artifactId, version, generator::createChainPlugin);
        }
        {
            final String groupId = "org.talend.test2";
            final String artifactId = "another-test-component";
            final String version = "14.11.1986";
            createComponent(m2, groupId, artifactId, version, generator::createPlugin);
        }
        {
            final String groupId = "org.talend.comp";
            final String artifactId = "jdbc-component";
            final String version = "0.0.1";
            createComponent(m2, groupId, artifactId, version, generator::createJdbcPlugin);
        }
        {
            final String groupId = "org.talend.comp";
            final String artifactId = "file-component";
            final String version = "0.0.1";
            createComponent(m2, groupId, artifactId, version, generator::createFilePlugin);
        }
        {
            final String groupId = "org.talend.test";
            final String artifactId = "collection-of-object";
            final String version = "0.0.1";
            createComponent(m2, groupId, artifactId, version, generator::createConfigPlugin);
        }
        {
            final String groupId = "org.talend.test";
            final String artifactId = "component-with-user-jars";
            final String version = "0.0.1";
            createComponent(m2, groupId, artifactId, version, generator::createCustomPlugin);
        }
        {
            final String groupId = "org.talend.test";
            final String artifactId = "migration-component";
            final String version = "0.0.1";
            createComponent(m2, groupId, artifactId, version, generator::createMigrationPlugin);
        }
        if (Boolean.getBoolean("components.server.beam.active")) {
            final String groupId = System.getProperty("components.sample.beam.groupId");
            final String artifactId = System.getProperty("components.sample.beam.artifactId");
            final String version = System.getProperty("components.sample.beam.version");
            createComponent(m2, groupId, artifactId, version, file -> {
                final File libDir = new File(System.getProperty("components.sample.beam.location"));
                File libFile = null;
                for (String f : libDir.list()) {
                    if (f.startsWith(artifactId)) {
                        libFile = new File(libDir, f);
                        break;
                    }
                }
                try (final InputStream from = new FileInputStream(libFile);
                        final OutputStream to = new FileOutputStream(file)) {
                    IO.copy(from, to);
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        final File ziplock = new File(m2, "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar");
        ziplock.getParentFile().mkdirs();
        try (final InputStream from = new FileInputStream(jarLocation(IO.class));
                final OutputStream to = new FileOutputStream(ziplock)) {
            IO.copy(from, to);
        } catch (final IOException e) {
            fail(e.getMessage());
        }

        return m2.getAbsolutePath();
    }

    private void createComponent(final File m2, final String groupId, final String artifactId, final String version,
            final Consumer<File> jarCreator) {
        final File artifact = new File(m2, groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/'
                + artifactId + '-' + version + ".jar");
        Files.mkdir(artifact.getParentFile());
        jarCreator.accept(artifact);

        final String components = System.getProperty("talend.component.server.component.coordinates");
        final String coord = groupId + ':' + artifactId + ":jar:" + version + ":compile";
        System
                .setProperty("talend.component.server.component.coordinates",
                        ofNullable(components).map(c -> c + "," + coord).orElse(coord));
        System.setProperty(artifactId + ".location", coord);
    }

    // copied from runtime-manager for now but we can completely fork it to test specific features so don't merge them
    private static class PluginGenerator {

        private String toPackage(final String marker) {
            return "org.talend.test.generated." + marker.replace('-', '_');
        }

        private File createChainPlugin(final File target) {
            final String packageName = toPackage(target.getParentFile().getParentFile().getName()).replace(".", "/");
            return createRepackaging(target, "org/talend/sdk/component/server/test/model", out -> {
                try {
                    out.putNextEntry(new JarEntry(packageName.replace('.', '/') + "/Messages.properties"));
                    out.write("chain.list._displayName = The List Component\n".getBytes(StandardCharsets.UTF_8));

                    out.putNextEntry(new JarEntry("icons/light/myicon.svg"));
                    out
                            .write(IO
                                    .readBytes(Thread
                                            .currentThread()
                                            .getContextClassLoader()
                                            .getResource("icons/logo.svg")));
                    out.closeEntry();

                    out.putNextEntry(new JarEntry("META-INF/services/" + ComponentMetadataEnricher.class.getName()));
                    out
                            .write("org.talend.sdk.component.server.test.model.MetadataEnricher\n"
                                    .getBytes(StandardCharsets.UTF_8));
                    out
                            .write("org.talend.sdk.component.server.test.model.MetadataEnricherLowestPriority\n"
                                    .getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();

                } catch (final IOException ioe) {
                    fail(ioe.getMessage());
                }
            });
        }

        private File createJdbcPlugin(final File target) {
            return createRepackaging(target, "org/talend/sdk/component/server/test/jdbc", out -> {
                try {
                    // enum displayname
                    out.putNextEntry(new JarEntry("org/talend/test/generated/jdbc_component/Messages.properties"));
                    new Properties() {

                        {
                            put("jdbc.datastore.jdbc._displayName", "JDBC DataStore");
                            put("jdbc.dataset.jdbc._displayName", "JDBC DataSet");
                            put("Type.PRECISE._displayName", "Furious");
                            put("jdbc.Database/jdbc/Standard._category", "DB/Std/Yes");
                            put("jdbc.actions.user.custom._displayName", "My Custom Action");
                            put("org.talend.test.generated.jdbc_component.JdbcService$I18n.read", "God save the queen");
                        }
                    }.store(out, "i18n for the config types");
                    out.closeEntry();
                    out.putNextEntry(new JarEntry("org/talend/test/generated/jdbc_component/Messages_fr.properties"));
                    new Properties() {

                        {
                            put("org.talend.test.generated.jdbc_component.JdbcService$I18n.read",
                                    "Liberté, égalité, fraternité");
                            put("configuration.connection._documentation", "Documentation pour Connection");
                            put("configuration.connection.configurations._documentation",
                                    "Documentation pour configurations");
                            put("configuration.connection.configurations[].description._documentation",
                                    "Documentation pour configurations description");
                            put("configuration.connection.configurations[].driver._documentation",
                                    "Documentation pour configurations conducteur");
                            put("configuration.connection.configurations[].dependencies._documentation",
                                    "Documentation pour configurations dependances");
                            put("configuration.connection.url._documentation", "Documentation pour hurle...");
                            put("configuration.driver._documentation", "Documentation pour conducteur...");
                            put("configuration.query._documentation", "Documentation pour requète...");
                            put("configuration.timeout._documentation", "Documentation pour expiration...");
                            put("configuration.connection.host._documentation", "Documentation pour hôte");
                            put("configuration.connection.port._documentation", "Documentation pour porteàfaux");
                            put("configuration.connection.username._documentation", "Documentation pour utilisateur");
                            put("configuration.connection.password._documentation", "Documentation pour mot de passe");
                        }
                    }.store(out, "i18n for the config types");
                    out.closeEntry();

                    out.putNextEntry(new JarEntry("TALEND-INF/dependencies.txt"));
                    out.write("org.apache.tomee:ziplock:jar:8.0.14:runtime".getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();

                    /*
                     * test that we take it from the classpath to support fallbacks
                     * out.putNextEntry(new JarEntry("icons/db-input_icon32.png"));
                     * out.write(IO.readBytes(Thread.currentThread().getContextClassLoader().getResource(
                     * "icons/db-input_icon32.png")));
                     * out.closeEntry();
                     */

                    out.putNextEntry(new JarEntry("TALEND-INF/documentation.adoc"));
                    out
                            .write(("== input\n\ndesc\n\n=== Configuration\n\nSomething1\n\n"
                                    + "== output\n\n=== Configuration\n\nSomething else")
                                    .getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        private File createFilePlugin(final File target) {
            return createRepackaging(target, "org/talend/sdk/component/server/test/file", null);
        }

        private File createConfigPlugin(final File target) {
            return createRepackaging(target, "org/talend/sdk/component/server/test/configuration", out -> {
                try {
                    out.putNextEntry(new JarEntry("TALEND-INF/dependencies.txt"));
                    out.closeEntry();
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        private File createCustomPlugin(final File target) {
            return createRepackaging(target, "org/talend/sdk/component/server/test/custom", out -> {
                try {
                    out.putNextEntry(new JarEntry("TALEND-INF/dependencies.txt"));
                    out.closeEntry();

                    out.putNextEntry(new JarEntry("TALEND-INF/documentation.adoc"));
                    out.write(("= Default").getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();

                    out.putNextEntry(new JarEntry("TALEND-INF/documentation_fr.adoc"));
                    out.write(("= Fr doc").getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        public File createMigrationPlugin(final File target) {
            return createRepackaging(target, "org/talend/sdk/component/server/test/migration", out -> {
                try {
                    out.putNextEntry(new JarEntry("TALEND-INF/dependencies.txt"));
                    out.closeEntry();
                    out.putNextEntry(new JarEntry("org/talend/test/generated/migration_component/Messages.properties"));
                    new Properties() {

                        {
                            put("migration.datastore.migration._displayName", "JDBC DataStore");
                            put("migration.dataset.migration._displayName", "JDBC DataSet");
                            put("migration.Database/migration/Standard._category", "DB/Std/Yes");
                            put("migration.parameters.user.custom._displayName", "My Custom Action");
                            put("org.talend.test.generated.jdbc_component.JdbcService$I18n.read", "God save the queen");
                        }
                    }.store(out, "i18n for the config types");

                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            });
        }

        private File createRepackaging(final File target, final String sourcePackage,
                final Consumer<JarOutputStream> custom) {
            try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
                final String packageName =
                        toPackage(target.getParentFile().getParentFile().getName()).replace(".", "/");
                final String fromPack = sourcePackage.replace('/', '.');
                final String toPack = packageName.replace('.', '/');
                final File root = new File(jarLocation(InitTestInfra.class), sourcePackage);
                ofNullable(root.listFiles())
                        .map(Stream::of)
                        .orElseGet(Stream::empty)
                        .filter(c -> c.getName().endsWith(".class"))
                        .forEach(clazz -> {
                            try (final InputStream is = new FileInputStream(clazz)) {
                                final ClassReader reader = new ClassReader(is);
                                final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
                                reader.accept(new ClassRemapper(writer, new Remapper() {

                                    @Override
                                    public String map(final String key) {
                                        return key.replace(sourcePackage, toPack).replace(fromPack, packageName);
                                    }
                                }), EXPAND_FRAMES);
                                outputStream.putNextEntry(new JarEntry(toPack + '/' + clazz.getName()));
                                outputStream.write(writer.toByteArray());
                            } catch (final IOException e) {
                                fail(e.getMessage());
                            }
                        });
                ofNullable(custom).ifPresent(c -> c.accept(outputStream));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            return target;
        }

        private File createPlugin(final File target) {
            try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
                final String packageName =
                        toPackage(target.getParentFile().getParentFile().getName()).replace(".", "/");
                outputStream.write(createProcessor(outputStream, packageName));
                outputStream.write(createModel(outputStream, packageName));
                outputStream.write(createService(outputStream, packageName, target.getName()));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            return target;
        }

        private byte[] createService(final JarOutputStream outputStream, final String packageName, final String name)
                throws IOException {
            final String className = packageName + "/AService.class";
            outputStream.putNextEntry(new ZipEntry(className));
            final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
            writer.visitAnnotation(Type.getDescriptor(Service.class), true).visitEnd();
            writer
                    .visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.substring(0, className.length() - ".class".length()),
                            null, Type.getInternalName(Object.class), null);
            writer.visitSource(className.replace(".class", ".java"), null);

            addConstructor(writer);

            final MethodVisitor action = writer
                    .visitMethod(ACC_PUBLIC, "doAction", "(L" + packageName + "/AModel;)L" + packageName + "/AModel;",
                            null, new String[0]);
            final AnnotationVisitor actionAnnotation = action.visitAnnotation(Type.getDescriptor(Action.class), true);
            actionAnnotation.visit("family", "proc");
            actionAnnotation.visit("value", name + "Action");
            actionAnnotation.visitEnd();
            action.visitCode();
            action.visitTypeInsn(NEW, packageName + "/AModel");
            action.visitInsn(DUP);
            action.visitMethodInsn(INVOKESPECIAL, packageName + "/AModel", "<init>", "()V", false);
            action.visitInsn(ARETURN);
            action.visitInsn(ARETURN);
            action.visitMaxs(1, 1);
            action.visitEnd();

            writer.visitEnd();
            return writer.toByteArray();
        }

        private byte[] createModel(final JarOutputStream outputStream, String packageName) throws IOException {
            final String className = packageName + "/AModel.class";
            outputStream.putNextEntry(new ZipEntry(className));
            final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
            writer
                    .visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.substring(0, className.length() - ".class".length()),
                            null, Type.getInternalName(Object.class), null);
            writer.visitSource(className.replace(".class", ".java"), null);

            addConstructor(writer);

            // no real content (fields/methods) for now

            writer.visitEnd();
            return writer.toByteArray();
        }

        private byte[] createProcessor(final JarOutputStream outputStream, final String packageName)
                throws IOException {
            final String className = packageName + "/AProcessor.class";
            outputStream.putNextEntry(new ZipEntry(className));
            final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
            final AnnotationVisitor processorAnnotation =
                    writer.visitAnnotation(Type.getDescriptor(Processor.class), true);
            processorAnnotation.visit("family", "comp");
            processorAnnotation.visit("name", "proc");
            processorAnnotation.visitEnd();
            writer
                    .visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.substring(0, className.length() - ".class".length()),
                            null, Type.getInternalName(Object.class),
                            new String[] { Serializable.class.getName().replace(".", "/") });
            writer.visitSource(className.replace(".class", ".java"), null);

            addConstructor(writer);

            // generate a processor
            final MethodVisitor emitMethod = writer
                    .visitMethod(ACC_PUBLIC, "emit", "(L" + packageName + "/AModel;)L" + packageName + "/AModel;", null,
                            new String[0]);
            emitMethod.visitAnnotation(Type.getDescriptor(ElementListener.class), true).visitEnd();
            emitMethod.visitCode();
            emitMethod.visitTypeInsn(NEW, packageName + "/AModel");
            emitMethod.visitInsn(DUP);
            emitMethod.visitMethodInsn(INVOKESPECIAL, packageName + "/AModel", "<init>", "()V", false);
            emitMethod.visitInsn(ARETURN);
            emitMethod.visitInsn(ARETURN);
            emitMethod.visitMaxs(1, 1);
            emitMethod.visitEnd();

            writer.visitEnd();
            return writer.toByteArray();
        }

        /*
         * private void addDependencies(final JarOutputStream outputStream, final String[] deps) throws IOException { //
         * start by writing the dependencies file outputStream.putNextEntry(new ZipEntry("META-INF/test/dependencies"));
         * outputStream.write("The following files have been resolved:\n".getBytes(StandardCharsets.UTF_8));
         * outputStream.write(Stream.of(deps).collect(joining("\n")).getBytes(StandardCharsets.UTF_8)); }
         */

        private void addConstructor(final ClassWriter writer) {
            final MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            constructor.visitInsn(RETURN);
            constructor.visitMaxs(1, 1);
            constructor.visitEnd();
        }
    }
}
