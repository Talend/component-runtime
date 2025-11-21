/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.xbean;

import static org.apache.xbean.asm9.ClassWriter.COMPUTE_FRAMES;
import static org.apache.xbean.asm9.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm9.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm9.Opcodes.ALOAD;
import static org.apache.xbean.asm9.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm9.Opcodes.RETURN;
import static org.apache.xbean.asm9.Opcodes.V1_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.xbean.asm9.AnnotationVisitor;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.finder.AnnotationFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;

class NestedJarArchiveTest {

    @Test
    void xbeanNestedScanning(final TestInfo info, @TempDir final File temporaryFolder) throws IOException {
        final File jar = createPlugin(temporaryFolder, info.getTestMethod().get().getName());
        final ConfigurableClassLoader configurableClassLoader = new ConfigurableClassLoader("", new URL[0],
                new URLClassLoader(new URL[] { jar.toURI().toURL() }, Thread.currentThread().getContextClassLoader()),
                n -> true, n -> true, new String[] { "BOOT-INF/lib/com/foo/bar/1.0/bar-1.0.jar" }, new String[0]);
        try (final JarInputStream jis = new JarInputStream(
                configurableClassLoader.getResourceAsStream("BOOT-INF/lib/com/foo/bar/1.0/bar-1.0.jar"))) {
            assertNotNull(jis, "test is wrongly setup, no nested jar, fix the createPlugin() method please");
            final AnnotationFinder finder =
                    new AnnotationFinder(new NestedJarArchive(null, jis, configurableClassLoader));
            final List<Class<?>> annotatedClasses = finder.findAnnotatedClasses(Processor.class);
            assertEquals(1, annotatedClasses.size());
            assertEquals("org.talend.test.generated." + info.getTestMethod().get().getName() + ".Components",
                    annotatedClasses.iterator().next().getName());
        } finally {
            URLClassLoader.class.cast(configurableClassLoader.getParent()).close();
        }
    }

    private File createPlugin(final File pluginFolder, final String name) throws IOException {
        final File target = new File(pluginFolder, name);
        target.getParentFile().mkdirs();
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            outputStream.putNextEntry(new ZipEntry("BOOT-INF/lib/com/foo/bar/1.0/bar-1.0.jar"));
            try (final JarOutputStream nestedStream = new JarOutputStream(outputStream)) {
                final String packageName = "org/talend/test/generated/" + name.replace(".jar", "");
                { // the factory (declaration)
                    final String className = packageName + "/Components.class";
                    nestedStream.putNextEntry(new ZipEntry(className));
                    final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
                    writer
                            .visit(V1_8, ACC_PUBLIC + ACC_SUPER,
                                    className.substring(0, className.length() - ".class".length()), null,
                                    Type.getInternalName(Object.class), null);
                    writer.visitSource(className.replace(".class", ".java"), null);
                    final AnnotationVisitor componentAnnotation =
                            writer.visitAnnotation(Type.getDescriptor(Processor.class), true);
                    componentAnnotation.visit("family", "comp");
                    componentAnnotation.visit("name", "comp");
                    componentAnnotation.visitEnd();

                    final MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    constructor.visitCode();
                    constructor.visitVarInsn(ALOAD, 0);
                    constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                    constructor.visitInsn(RETURN);
                    constructor.visitMaxs(1, 1);
                    constructor.visitEnd();

                    writer.visitEnd();
                    nestedStream.write(writer.toByteArray());
                }
            }
        }
        return target;
    }
}
