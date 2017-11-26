/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.asm;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.xbean.asm6.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm6.ClassWriter.COMPUTE_FRAMES;
import static org.apache.xbean.asm6.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm6.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm6.Opcodes.ALOAD;
import static org.apache.xbean.asm6.Opcodes.ARETURN;
import static org.apache.xbean.asm6.Opcodes.DUP;
import static org.apache.xbean.asm6.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm6.Opcodes.NEW;
import static org.apache.xbean.asm6.Opcodes.RETURN;
import static org.apache.xbean.asm6.Opcodes.V1_8;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.xbean.asm6.AnnotationVisitor;
import org.apache.xbean.asm6.ClassReader;
import org.apache.xbean.asm6.ClassWriter;
import org.apache.xbean.asm6.MethodVisitor;
import org.apache.xbean.asm6.Type;
import org.apache.xbean.asm6.commons.Remapper;
import org.apache.xbean.asm6.commons.RemappingClassAdapter;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;

// part of that code abused of ASMifier so don't try to write it yourself to not loose too much time
public class PluginGenerator {

    public String toPackage(final String container) {
        return "org.talend.test.generated." + container.replace(".jar", "");
    }

    public File createChainPlugin(final File dir, final String plugin) {
        final File target = new File(dir, plugin);
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            final String packageName = toPackage(target.getParentFile().getParentFile().getName()).replace(".", "/");
            final String sourcePackage = "org/talend/test";
            final String fromPack = sourcePackage.replace('/', '.');
            final String toPack = packageName.replace('.', '/');
            final File root = new File(jarLocation(getClass()), sourcePackage);
            ofNullable(root.listFiles()).map(Stream::of).orElseGet(Stream::empty)
                .filter(c -> c.getName().endsWith(".class")).forEach(clazz -> {
                    try (final InputStream is = new FileInputStream(clazz)) {
                        final ClassReader reader = new ClassReader(is);
                        final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
                        reader.accept(new RemappingClassAdapter(writer, new Remapper() {

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
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return target;
    }

    // avoid to load any class and since we have a shade of asm in the classpath
    // just generate the jars directly
    public File createPlugin(final File pluginFolder, final String name, final String... deps) throws IOException {
        final File target = new File(pluginFolder, name);
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            addDependencies(outputStream, deps);

            // write the classes
            final String packageName = toPackage(name).replace(".", "/");
            outputStream.write(createProcessor(outputStream, packageName));
            outputStream.write(createModel(outputStream, packageName));
            outputStream.write(createService(outputStream, packageName, name));
        }
        return target;
    }

    private byte[] createService(final JarOutputStream outputStream, final String packageName, final String name)
        throws IOException {
        final String className = packageName + "/AService.class";
        outputStream.putNextEntry(new ZipEntry(className));
        final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
        writer.visitAnnotation(Type.getDescriptor(Service.class), true).visitEnd();
        writer.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.substring(0, className.length() - ".class".length()), null,
            Type.getInternalName(Object.class), null);
        writer.visitSource(className.replace(".class", ".java"), null);

        addConstructor(writer);

        final MethodVisitor action = writer.visitMethod(ACC_PUBLIC, "doAction",
            "(L" + packageName + "/AModel;)L" + packageName + "/AModel;", null, new String[0]);
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
        writer.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.substring(0, className.length() - ".class".length()), null,
            Type.getInternalName(Object.class), null);
        writer.visitSource(className.replace(".class", ".java"), null);

        addConstructor(writer);

        // no real content (fields/methods) for now

        writer.visitEnd();
        return writer.toByteArray();
    }

    private byte[] createProcessor(final JarOutputStream outputStream, final String packageName) throws IOException {
        final String className = packageName + "/AProcessor.class";
        outputStream.putNextEntry(new ZipEntry(className));
        final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
        final AnnotationVisitor processorAnnotation = writer.visitAnnotation(Type.getDescriptor(Processor.class), true);
        processorAnnotation.visit("family", "comp");
        processorAnnotation.visit("name", "proc");
        processorAnnotation.visitEnd();
        writer.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.substring(0, className.length() - ".class".length()), null,
            Type.getInternalName(Object.class), new String[] { Serializable.class.getName().replace(".", "/") });
        writer.visitSource(className.replace(".class", ".java"), null);

        addConstructor(writer);

        // generate a processor
        final MethodVisitor emitMethod = writer.visitMethod(ACC_PUBLIC, "emit",
            "(L" + packageName + "/AModel;)L" + packageName + "/AModel;", null, new String[0]);
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
        outputStream.write(Stream.of(deps).collect(joining("\n")).getBytes(StandardCharsets.UTF_8));
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
