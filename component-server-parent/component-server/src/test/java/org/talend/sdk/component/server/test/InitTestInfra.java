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
package org.talend.sdk.component.server.test;

import static java.util.Optional.ofNullable;
import static org.apache.xbean.asm7.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm7.ClassWriter.COMPUTE_FRAMES;
import static org.apache.xbean.asm7.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm7.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm7.Opcodes.ALOAD;
import static org.apache.xbean.asm7.Opcodes.ARETURN;
import static org.apache.xbean.asm7.Opcodes.DUP;
import static org.apache.xbean.asm7.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm7.Opcodes.NEW;
import static org.apache.xbean.asm7.Opcodes.RETURN;
import static org.apache.xbean.asm7.Opcodes.V1_8;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.meecrowave.Meecrowave;
import org.apache.xbean.asm7.AnnotationVisitor;
import org.apache.xbean.asm7.ClassReader;
import org.apache.xbean.asm7.ClassWriter;
import org.apache.xbean.asm7.MethodVisitor;
import org.apache.xbean.asm7.Type;
import org.apache.xbean.asm7.commons.ClassRemapper;
import org.apache.xbean.asm7.commons.Remapper;
import org.apache.ziplock.Files;
import org.apache.ziplock.IO;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;

// create a test m2 repo and setup the server configuration to ensure components are found
public class InitTestInfra implements Meecrowave.ConfigurationCustomizer {

    @Override
    public void accept(final Meecrowave.Builder builder) {
        Locale.setDefault(Locale.ENGLISH);
        builder.setJsonbPrettify(true);
        builder
                .setTempDir(new File(jarLocation(InitTestInfra.class).getParentFile(), getClass().getSimpleName())
                        .getAbsolutePath());
        System.setProperty("talend.component.server.maven.repository", createM2(builder.getTempDir()));
    }

    private String createM2(final String tempDir) {
        // reusing tempDir we don't need to delete it, done by meecrowave
        final File m2 = new File(tempDir, ".m2/repository");

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

        final File ziplock = new File(m2, "org/apache/tomee/ziplock/7.0.5/ziplock-7.0.5.jar");
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
            return createRepackaging(target, "org/talend/sdk/component/server/test/model", outputStream -> {
                try {
                    outputStream.putNextEntry(new JarEntry(packageName.replace('.', '/') + "/Messages.properties"));
                    outputStream
                            .write("chain.list._displayName = The List Component\n".getBytes(StandardCharsets.UTF_8));
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
                        }
                    }.store(out, "i18n for the config types");
                    out.closeEntry();

                    out.putNextEntry(new JarEntry("TALEND-INF/dependencies.txt"));
                    out.write("org.apache.tomee:ziplock:jar:7.0.5:runtime".getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();

                    out.putNextEntry(new JarEntry("TALEND-INF/documentation.adoc"));
                    out
                            .write("== input\n\n=== Configuration\n\nSomething1\n\n== output\n\n=== Configuration\n\nSomething else"
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
