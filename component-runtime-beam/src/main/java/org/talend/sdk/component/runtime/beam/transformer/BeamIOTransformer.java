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

import static java.lang.Integer.MIN_VALUE;
import static java.util.Arrays.asList;
import static org.apache.xbean.asm7.Opcodes.ALOAD;
import static org.apache.xbean.asm7.Opcodes.ARETURN;
import static org.apache.xbean.asm7.Opcodes.ASM7;
import static org.apache.xbean.asm7.Opcodes.DUP;
import static org.apache.xbean.asm7.Opcodes.INVOKESTATIC;
import static org.apache.xbean.asm7.Opcodes.NEW;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.LinkedList;

import org.apache.xbean.asm7.ClassReader;
import org.apache.xbean.asm7.ClassVisitor;
import org.apache.xbean.asm7.ClassWriter;
import org.apache.xbean.asm7.Label;
import org.apache.xbean.asm7.MethodVisitor;
import org.apache.xbean.asm7.Type;
import org.apache.xbean.asm7.commons.AdviceAdapter;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeamIOTransformer implements ClassFileTransformer {

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
                return rewrite(classLoader, className, classfileBuffer, tmpLoader);
            }
        } finally {
            thread.setContextClassLoader(old);
        }
        return classfileBuffer;
    }

    private byte[] rewrite(final ConfigurableClassLoader loader, final String className, final byte[] classfileBuffer,
            final ClassLoader tmpLoader) {
        final String plugin = loader.getId();

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
            if (clazz.getClassLoader() == tmpLoader.getParent()) {
                return false;
            }

            return doesHierarchyContain(clazz,
                    asList("org.apache.beam.sdk.transforms.DoFn", "org.apache.beam.sdk.io.Source",
                            "org.apache.beam.sdk.io.Source$Reader", "org.apache.beam.sdk.coders.Coder"));
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            return false;
        }
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

    public static ClassLoader setPluginTccl(final String key) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(ContainerFinder.Instance.get().find(key).classloader());
        return old;
    }

    public static void resetTccl(final ClassLoader loader) {
        Thread.currentThread().setContextClassLoader(loader);
    }

    public static class SerializationWrapper implements Serializable {

        private static class SkipState {

            private final LinkedList<Object> stack = new LinkedList<>();
        }

        private static final ThreadLocal<SkipState> SKIP = new ThreadLocal<>();

        private final String plugin;

        private final byte[] delegateBytes;

        public SerializationWrapper(final Object delegate, final String plugin) {
            this.plugin = plugin;
            this.delegateBytes = serialize(delegate);
        }

        private byte[] serialize(final Object delegate) {
            final SkipState enteringSkipState = SKIP.get();
            if (enteringSkipState == null) {
                SKIP.set(new SkipState());
            }
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
                if (enteringSkipState == null) {
                    SKIP.remove();
                }
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

        public static Object replace(final Object delegate, final String plugin) {
            final SkipState skip = SKIP.get();
            if (skip == null) {
                SKIP.remove();
                return new SerializationWrapper(delegate, plugin);
            }
            if (!skip.stack.contains(delegate)) {
                skip.stack.add(delegate);
                return new SerializationWrapper(delegate, plugin);
            }
            return null;
        }
    }

    private static class TCCLAdviceAdapter extends AdviceAdapter {

        private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

        private static final Type TCCL_HELPER = Type.getType(BeamIOTransformer.class);

        private static final Type STRING_TYPE = Type.getType(String.class);

        private static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);

        private static final Type[] SET_TCCL_ARGS = new Type[] { STRING_TYPE };

        private static final Type[] RESET_TCCL_ARGS = new Type[] { CLASSLOADER_TYPE };

        private static final org.apache.xbean.asm7.commons.Method SET_METHOD =
                new org.apache.xbean.asm7.commons.Method("setPluginTccl", CLASSLOADER_TYPE, SET_TCCL_ARGS);

        private static final org.apache.xbean.asm7.commons.Method RESET_METHOD =
                new org.apache.xbean.asm7.commons.Method("resetTccl", Type.VOID_TYPE, RESET_TCCL_ARGS);

        private final String plugin;

        private final String desc;

        private final Label tryStart = new Label();

        private final Label endLabel = new Label();

        private int ctxLocal;

        private TCCLAdviceAdapter(final MethodVisitor mv, final int access, final String name, final String desc,
                final String plugin) {
            super(ASM7, mv, access, name, desc);
            this.plugin = plugin;
            this.desc = desc;
        }

        @Override
        public void onMethodEnter() {
            push(plugin);
            ctxLocal = newLocal(CLASSLOADER_TYPE);
            invokeStatic(TCCL_HELPER, SET_METHOD);
            storeLocal(ctxLocal);
            visitLabel(tryStart);
        }

        @Override
        public void onMethodExit(final int opCode) {
            if (opCode == ATHROW) {
                return;
            }

            int stateLocal = -1;
            if (opCode != MIN_VALUE) {
                final Type returnType = Type.getReturnType(desc);
                final boolean isVoid = Type.VOID_TYPE.equals(returnType);
                if (!isVoid) {
                    stateLocal = newLocal(returnType);
                    storeLocal(stateLocal);
                }
            } else {
                stateLocal = newLocal(THROWABLE_TYPE);
                storeLocal(stateLocal);
            }

            loadLocal(ctxLocal);
            invokeStatic(TCCL_HELPER, RESET_METHOD);

            if (stateLocal != -1) {
                loadLocal(stateLocal);
            }
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
            visitLabel(endLabel);
            catchException(tryStart, endLabel, THROWABLE_TYPE);
            onMethodExit(MIN_VALUE);
            throwException();
            super.visitMaxs(0, 0);
        }
    }

    // todo: we probably want to rewrite some well known method (@ProcessElement) to set the TCCL properly too
    private static class ComponentClassVisitor extends ClassVisitor {

        private static final String[] OBJECT_STREAM_EXCEPTION =
                { Type.getType(ObjectStreamException.class).getInternalName() };

        private final ComponentClassWriter writer;

        private final String plugin;

        private boolean hasWriteReplace;

        private ComponentClassVisitor(final ComponentClassWriter cv, final String plugin) {
            super(ASM7, cv);
            this.plugin = plugin;
            this.writer = cv;
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                final String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("writeReplace".equals(name)) {
                hasWriteReplace = true;
            }
            if (Modifier.isPublic(access) && !Modifier.isStatic(access)) {
                return new TCCLAdviceAdapter(mv, access, name, desc, plugin);
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            createSerialisation(writer, plugin);
            super.visitEnd();
        }

        private void createSerialisation(final ClassWriter cw, final String pluginId) {
            if (hasWriteReplace) {
                return;
            }
            final MethodVisitor mv = cw
                    .visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null,
                            OBJECT_STREAM_EXCEPTION);

            mv.visitCode();

            final String wrapperType = SerializationWrapper.class.getName().replace('.', '/');
            mv.visitTypeInsn(NEW, wrapperType);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(pluginId);
            mv
                    .visitMethodInsn(INVOKESTATIC, wrapperType, "replace",
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
