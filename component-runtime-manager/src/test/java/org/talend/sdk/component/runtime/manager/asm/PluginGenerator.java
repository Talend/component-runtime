/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.asm;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.xbean.asm9.AnnotationVisitor;
import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.commons.ClassRemapper;
import org.apache.xbean.asm9.commons.Remapper;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;

// part of that code abused of ASMifier so don't try to write it yourself to not loose too much time
public class PluginGenerator {

    public String toPackage(final String container) {
        return "org.talend.test.generated." + container.replace(".jar", "");
    }

    public File createChainPlugin(final File dir, final String plugin, final int maxBatchSize) {
        final File target = new File(dir, plugin);
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            final String packageName = toPackage(target.getParentFile().getParentFile().getName()).replace(".", "/");
            final String sourcePackage = "org/talend/test";
            final String fromPack = sourcePackage.replace('/', '.');
            final String toPack = packageName.replace('.', '/');
            final File root = new File(jarLocation(getClass()), sourcePackage);
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
                            outputStream.closeEntry();
                        } catch (final IOException e) {
                            fail(e.getMessage());
                        }
                    });
            outputStream.putNextEntry(new JarEntry("TALEND-INF/local-configuration.properties"));
            outputStream.write(("_maxBatchSize.value=" + maxBatchSize).getBytes(StandardCharsets.UTF_8));
            outputStream.closeEntry();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return target;
    }

    public File createChainPlugin(final File dir, final String plugin) {
        return createChainPlugin(dir, plugin, 1000);
    }

    public File createPlugin(final File pluginFolder, final String name, final String... deps) throws IOException {
        return createPluginAt(new File(pluginFolder, name),
                jar -> createComponent(name, jar, toPackage(name).replace(".", "/")), deps);
    }

    // avoid to load any class and since we have a shade of asm in the classpath
    // just generate the jars directly
    public File createPluginAt(final File target, final Consumer<JarOutputStream> jarFiller, final String... deps)
            throws IOException {
        target.getParentFile().mkdirs();
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            addDependencies(outputStream, deps);
            jarFiller.accept(outputStream);
        }
        return target;
    }

    public void createComponent(final String name, final JarOutputStream outputStream, final String packageName) {
        try {
            outputStream.write(createProcessor(outputStream, packageName, "proc"));
            outputStream.write(createModel(outputStream, packageName));
            outputStream.write(createService(outputStream, packageName, name));
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
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
                .visitMethod(ACC_PUBLIC, "doAction", "(L" + packageName + "/AModel;)L" + packageName + "/AModel;", null,
                        new String[0]);
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

    public byte[] createModel(final JarOutputStream outputStream, final String packageName) throws IOException {
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

    public byte[] createProcessor(final JarOutputStream outputStream, final String packageName, final String name)
            throws IOException {
        final String className = packageName + "/AProcessor.class";
        outputStream.putNextEntry(new ZipEntry(className));
        final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
        final AnnotationVisitor processorAnnotation = writer.visitAnnotation(Type.getDescriptor(Processor.class), true);
        processorAnnotation.visit("family", "comp");
        processorAnnotation.visit("name", name);
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

    private void addDependencies(final JarOutputStream outputStream, final String[] deps) throws IOException {
        // start by writing the dependencies file
        outputStream.putNextEntry(new ZipEntry("META-INF/test/dependencies"));
        outputStream.write("The following files have been resolved:\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write(String.join("\n", deps).getBytes(StandardCharsets.UTF_8));
    }

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
