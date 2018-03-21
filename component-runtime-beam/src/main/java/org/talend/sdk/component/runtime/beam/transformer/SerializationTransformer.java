/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.transformer;

import static java.util.Arrays.asList;
import static org.apache.xbean.asm6.Opcodes.ALOAD;
import static org.apache.xbean.asm6.Opcodes.ARETURN;
import static org.apache.xbean.asm6.Opcodes.ASM6;
import static org.apache.xbean.asm6.Opcodes.DUP;
import static org.apache.xbean.asm6.Opcodes.INVOKESTATIC;
import static org.apache.xbean.asm6.Opcodes.NEW;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.xbean.asm6.ClassReader;
import org.apache.xbean.asm6.ClassVisitor;
import org.apache.xbean.asm6.ClassWriter;
import org.apache.xbean.asm6.MethodVisitor;
import org.apache.xbean.asm6.Type;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializationTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        if (className == null || !ConfigurableClassLoader.class.isInstance(loader)) {
            return classfileBuffer;
        }

        final ConfigurableClassLoader classLoader = ConfigurableClassLoader.class.cast(loader);
        final URLClassLoader tmpLoader = classLoader.createTemporaryCopy();
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(tmpLoader);
        try {
            if (shouldForceContextualSerialization(tmpLoader, className)) {
                return rewrite(loader, className, classfileBuffer, tmpLoader);
            }
        } finally {
            thread.setContextClassLoader(old);
        }
        return classfileBuffer;
    }

    private byte[] rewrite(final ClassLoader loader, final String className, final byte[] classfileBuffer,
            final ClassLoader tmpLoader) {
        final String plugin = ComponentManager
                .instance()
                .find(c -> Stream.of(c).filter(it -> it.getLoader() == loader))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No container with classloader " + loader))
                .getId();

        final ClassReader reader = new ClassReader(classfileBuffer);
        final ComponentClassWriter writer =
                new ComponentClassWriter(className.replace('/', '.'), tmpLoader, reader, ClassWriter.COMPUTE_FRAMES);
        final ComponentClassVisitor visitor = new ComponentClassVisitor(writer, plugin);
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private boolean shouldForceContextualSerialization(final ClassLoader tmpLoader, final String className) {
        try {
            final Class<?> clazz = tmpLoader.loadClass(className.replace('/', '.'));
            if (doesUseCustomSerialization(className, clazz)) {
                return false;
            }

            return doesHierarchyContain(clazz,
                    asList("org.apache.beam.sdk.transforms.DoFn", "org.apache.beam.sdk.io.Source"));
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            return false;
        }
    }

    private boolean doesUseCustomSerialization(final String className, final Class<?> clazz) {
        if (Externalizable.class.isInstance(className)) {
            return true;
        }
        try {
            clazz.getDeclaredMethod("writeReplace", ObjectOutputStream.class);
            return true;
        } catch (final NoSuchMethodException e) {
            // ok, let's check further
        }
        return false;
    }

    private boolean doesHierarchyContain(final Class<?> clazz, final Collection<String> types) {
        final Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || Object.class == superclass) {
            return false;
        }
        if (types.contains(superclass.getName())) {
            return true;
        }
        // for now don't check interfaces since isBeamComponent only relies on classes
        return doesHierarchyContain(superclass, types);
    }

    public static class SerializationWrapper implements Serializable {

        private static final ThreadLocal<Boolean> SKIP = new ThreadLocal<>();

        private final String plugin;

        private final byte[] delegateBytes;

        public SerializationWrapper(final Object delegate, final String plugin) {
            this.plugin = plugin;
            this.delegateBytes = serialize(delegate);
        }

        public static Object replace(final Object delegate, final String plugin) {
            final Boolean skip = SKIP.get();
            if (skip == null || !skip) {
                SKIP.remove();
                return new SerializationWrapper(delegate, plugin);
            }
            return null;
        }

        private byte[] serialize(final Object delegate) {
            SKIP.set(true);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final ObjectOutputStream oos = new ObjectOutputStream(baos) {

                {
                    enableReplaceObject(true);
                }

                @Override // switch it back to the original to avoid to recreate a serialization wrapper
                protected Object replaceObject(final Object obj) throws IOException {
                    return obj == null ? delegate : obj;
                }
            }) {
                oos.writeObject(delegate);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } finally {
                SKIP.remove();
            }
            return baos.toByteArray();
        }

        Object readResolve() throws ObjectStreamException {
            final ContainerFinder containerFinder = ContainerFinder.Instance.get();
            final LightContainer container = containerFinder.find(plugin);
            final ClassLoader classloader = container.classloader();
            final Thread thread = Thread.currentThread();
            final ClassLoader oldClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(classloader);
            try (final ObjectInputStream inputStream =
                    new EnhancedObjectInputStream(new ByteArrayInputStream(delegateBytes), classloader)) {
                return inputStream.readObject();
            } catch (final ClassNotFoundException | IOException e) {
                throw new IllegalStateException(e);
            } finally {
                thread.setContextClassLoader(oldClassLoader);
            }
        }
    }

    // todo: we probably want to rewrite some well known method (@ProcessElement) to set the TCCL properly too
    private static class ComponentClassVisitor extends ClassVisitor {

        private final ComponentClassWriter writer;

        private final String plugin;

        private ComponentClassVisitor(final ComponentClassWriter cv, final String plugin) {
            super(ASM6, cv);
            this.plugin = plugin;
            this.writer = cv;
        }

        @Override
        public void visitEnd() {
            createSerialisation(writer, plugin);
            super.visitEnd();
        }

        private void createSerialisation(final ClassWriter cw, final String pluginId) {
            final MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null,
                    new String[] { Type.getType(ObjectStreamException.class).getInternalName() });

            mv.visitCode();

            final String wrapperType = SerializationWrapper.class.getName().replace('.', '/');
            mv.visitTypeInsn(NEW, wrapperType);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(pluginId);
            mv.visitMethodInsn(INVOKESTATIC, wrapperType, "replace",
                    "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
            mv.visitInsn(ARETURN);

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }

    private static class ComponentClassWriter extends ClassWriter {

        private final String currentClass;

        private final ClassLoader tmpLoader;

        private ComponentClassWriter(final String name, final ClassLoader loader, final ClassReader reader,
                final int flags) {
            super(reader, flags);
            this.tmpLoader = loader;
            this.currentClass = name;
        }

        @Override
        protected String getCommonSuperClass(final String type1, final String type2) {
            Class<?> c;
            final Class<?> d;
            try {
                c = findClass(type1.replace('/', '.'));
                d = findClass(type2.replace('/', '.'));
            } catch (final Exception e) {
                throw new RuntimeException(e.toString());
            } catch (final ClassCircularityError e) {
                return "java/lang/Object";
            }
            if (c.isAssignableFrom(d)) {
                return type1;
            }
            if (d.isAssignableFrom(c)) {
                return type2;
            }
            if (c.isInterface() || d.isInterface()) {
                return "java/lang/Object";
            } else {
                do {
                    c = c.getSuperclass();
                } while (!c.isAssignableFrom(d));
                return c.getName().replace('.', '/');
            }
        }

        private Class<?> findClass(final String className) throws ClassNotFoundException {
            try {
                return currentClass.equals(className) ? Object.class : Class.forName(className, false, tmpLoader);
            } catch (final ClassNotFoundException e) {
                return Class.forName(className, false, getClass().getClassLoader());
            }
        }
    }
}
