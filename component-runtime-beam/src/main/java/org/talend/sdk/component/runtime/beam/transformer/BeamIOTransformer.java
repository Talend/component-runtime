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
import static java.util.stream.Collectors.toSet;
import static org.apache.xbean.asm7.Opcodes.ALOAD;
import static org.apache.xbean.asm7.Opcodes.ARETURN;
import static org.apache.xbean.asm7.Opcodes.ASM7;
import static org.apache.xbean.asm7.Opcodes.DUP;
import static org.apache.xbean.asm7.Opcodes.INVOKESTATIC;
import static org.apache.xbean.asm7.Opcodes.NEW;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.transforms.Combine;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BeamIOTransformer implements ClassFileTransformer {

    private static final boolean DEBUG = Boolean.getBoolean("talend.component.beam.transformers.debug");

    private static final BiConsumer<OutputStream, Object> BYPASS_REPLACE_SERIALIZER = createSerializer();

    private final Collection<String> typesToEnhance;

    public BeamIOTransformer() {
        this(Stream
                .of("org.apache.beam.sdk.coders.Coder", "org.apache.beam.sdk.io.Source",
                        "org.apache.beam.sdk.io.Source$Reader", "org.apache.beam.sdk.io.UnboundedSource$CheckpointMark",
                        "org.apache.beam.sdk.transforms.DoFn", "org.apache.beam.sdk.transforms.PTransform",
                        "org.apache.beam.sdk.transforms.Combine$CombineFn",
                        "org.apache.beam.sdk.transforms.SerializableFunction", "org.apache.beam.sdk.values.TupleTag")
                .collect(toSet()));
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        if (className == null || !ConfigurableClassLoader.class.isInstance(loader)) {
            return classfileBuffer;
        }

        final ConfigurableClassLoader classLoader = ConfigurableClassLoader.class.cast(loader);
        final URLClassLoader tmpLoader = classLoader.createTemporaryCopy(); // cache it: mem is the issue?
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(tmpLoader);
        try {
            final Class<?> tmpClass = loadTempClass(tmpLoader, className);
            if (tmpClass.getClassLoader() != tmpLoader.getParent() && doesHierarchyContain(tmpClass, typesToEnhance)) {
                return rewrite(classLoader, className, classfileBuffer, tmpLoader, tmpClass);
            }
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            if (DEBUG) {
                log.error("Can't load: " + className, e);
            }
        } finally {
            thread.setContextClassLoader(old);
        }
        return classfileBuffer;
    }

    private byte[] rewrite(final ConfigurableClassLoader loader, final String className, final byte[] classfileBuffer,
            final ClassLoader tmpLoader, final Class<?> tmpClass) {
        final String plugin = loader.getId();

        final ClassReader reader = new ClassReader(classfileBuffer);
        final ComponentClassWriter writer =
                new ComponentClassWriter(className.replace('/', '.'), tmpLoader, reader, ClassWriter.COMPUTE_FRAMES);
        final SerializableCoderReplacement serializableCoderReplacement =
                new SerializableCoderReplacement(writer, plugin, tmpClass);
        final ComponentClassVisitor visitor = new ComponentClassVisitor(serializableCoderReplacement, plugin);
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private boolean shouldForceContextualSerialization(final Class<?> tmpClass) {
        try {
            return doesHierarchyContain(tmpClass, typesToEnhance);
        } catch (final NoClassDefFoundError e) {
            return false;
        }
    }

    private Class<?> loadTempClass(final ClassLoader tmpLoader, final String className) throws ClassNotFoundException {
        return tmpLoader.loadClass(className.replace('/', '.'));
    }

    private boolean doesHierarchyContain(final Class<?> clazz, final Collection<String> types) {
        final Class<?> superclass = clazz.getSuperclass();
        if (Stream.of(clazz.getInterfaces()).anyMatch(itf -> types.contains(itf.getName()))) {
            return true;
        }
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

    // we want to ensure java.io.ObjectStreamClass.writeReplaceMethod is disabled
    private static BiConsumer<OutputStream, Object> createSerializer() {
        final Method writeOrdinaryObject;
        final Method setBlockDataMode;
        final Field bout;
        final Field depth;
        final Field subs;
        final Field handles;
        final Method subsLookup;
        final Method handlesLookup;
        final Method writeNull;
        final Method writeClass;
        final Method writeClassDesc;
        final Method writeHandle;
        final Method writeString;
        final Method writeArray;
        final Method writeEnum;
        try {
            writeOrdinaryObject = ObjectOutputStream.class
                    .getDeclaredMethod("writeOrdinaryObject", Object.class, ObjectStreamClass.class, boolean.class);
            bout = ObjectOutputStream.class.getDeclaredField("bout");
            depth = ObjectOutputStream.class.getDeclaredField("depth");
            subs = ObjectOutputStream.class.getDeclaredField("subs");
            handles = ObjectOutputStream.class.getDeclaredField("handles");
            subsLookup = subs.getType().getDeclaredMethod("lookup", Object.class);
            handlesLookup = handles.getType().getDeclaredMethod("lookup", Object.class);
            setBlockDataMode = bout.getType().getDeclaredMethod("setBlockDataMode", boolean.class);
            writeNull = ObjectOutputStream.class.getDeclaredMethod("writeNull");
            writeClass = ObjectOutputStream.class.getDeclaredMethod("writeClass", Class.class, boolean.class);
            writeClassDesc = ObjectOutputStream.class
                    .getDeclaredMethod("writeClassDesc", ObjectStreamClass.class, boolean.class);
            writeHandle = ObjectOutputStream.class.getDeclaredMethod("writeHandle", int.class);
            writeString = ObjectOutputStream.class.getDeclaredMethod("writeString", String.class, boolean.class);
            writeArray = ObjectOutputStream.class
                    .getDeclaredMethod("writeArray", Object.class, ObjectStreamClass.class, boolean.class);
            writeEnum = ObjectOutputStream.class
                    .getDeclaredMethod("writeEnum", Enum.class, ObjectStreamClass.class, boolean.class);
            Stream
                    .of(writeOrdinaryObject, bout, depth, setBlockDataMode, subs, subsLookup, handles, handlesLookup,
                            writeNull, writeClass, writeClassDesc, writeHandle, writeString, writeArray, writeEnum)
                    .forEach(accessible -> {
                        if (!accessible.isAccessible()) {
                            accessible.setAccessible(true);
                        }
                    });
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return (out, obj) -> {
            try (final ObjectOutputStream oos = new ObjectOutputStream(out)) {
                final boolean oldMode = Boolean.class.cast(setBlockDataMode.invoke(bout.get(oos), false));
                depth.set(oos, Integer.class.cast(depth.get(oos)) + 1);
                try {
                    int h;
                    if ((obj = subsLookup.invoke(subs.get(oos), obj)) == null) {
                        writeNull.invoke(oos);
                    } else if ((h = Integer.class.cast(handlesLookup.invoke(handles.get(oos), obj))) != -1) {
                        writeHandle.invoke(oos, h);
                    } else if (Class.class.isInstance(obj)) {
                        writeClass.invoke(oos, obj, false);
                    } else if (ObjectStreamClass.class.isInstance(obj)) {
                        writeClassDesc.invoke(oos, obj, false);
                    } else if (String.class.isInstance(obj)) {
                        writeClassDesc.invoke(oos, obj, false);
                    } else if (obj instanceof String) {
                        writeString.invoke(oos, obj, false);
                    } else if (obj.getClass().isArray()) {
                        writeArray.invoke(oos, obj, ObjectStreamClass.lookup(obj.getClass()), false);
                    } else if (obj instanceof Enum) {
                        writeEnum.invoke(oos, obj, ObjectStreamClass.lookup(obj.getClass()), false);
                    } else if (obj instanceof Serializable) {
                        writeOrdinaryObject.invoke(oos, obj, ObjectStreamClass.lookup(obj.getClass()), false);
                    } else {
                        throw new NotSerializableException(String.valueOf(obj));
                    }
                } finally {
                    depth.set(oos, Integer.class.cast(depth.get(oos)) - 1);
                    setBlockDataMode.invoke(bout.get(oos), oldMode);
                }
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static class SerializableCoderReplacement extends ClassVisitor {

        private final String plugin;

        private final Type accumulatorType;

        private SerializableCoderReplacement(final ClassVisitor delegate, final String plugin, final Class<?> clazz) {
            super(ASM7, delegate);
            this.plugin = plugin;

            Type accumulatorType = null;
            if (Combine.CombineFn.class.isAssignableFrom(clazz)) { // not the best impl but user code should handle it
                try {
                    if (clazz
                            .getMethod("getAccumulatorCoder", CoderRegistry.class, Coder.class)
                            .getDeclaringClass() != clazz) {
                        accumulatorType = Type.getType(clazz.getMethod("createAccumulator").getReturnType());
                    }
                } catch (final NoSuchMethodException e) {
                    // no-op
                }
            }
            this.accumulatorType = accumulatorType;
        }

        @Override
        public void visitEnd() {
            if (accumulatorType != null) {
                final MethodVisitor getAccumulatorCoder = super.visitMethod(Modifier.PUBLIC, "getAccumulatorCoder",
                        "(Lorg/apache/beam/sdk/coders/CoderRegistry;Lorg/apache/beam/sdk/coders/Coder;)"
                                + "Lorg/apache/beam/sdk/coders/Coder;",
                        null, null);
                getAccumulatorCoder.visitLdcInsn(accumulatorType);
                getAccumulatorCoder.visitLdcInsn(plugin);
                getAccumulatorCoder
                        .visitMethodInsn(INVOKESTATIC,
                                "org/talend/sdk/component/runtime/beam/coder/ContextualSerializableCoder", "of",
                                "(Ljava/lang/Class;Ljava/lang/String;)Lorg/apache/beam/sdk/coders/SerializableCoder;",
                                false);
                getAccumulatorCoder.visitInsn(ARETURN);
                getAccumulatorCoder.visitMaxs(-1, -1);
                getAccumulatorCoder.visitEnd();
            }
            super.visitEnd();
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                final String signature, final String[] exceptions) {
            final MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(ASM7, delegate) {

                @Override
                public void visitMethodInsn(final int opcode, final String owner, final String name,
                        final String descriptor, final boolean isInterface) {
                    if ("org/apache/beam/sdk/coders/SerializableCoder".equals(owner) && "of".equals(name)
                            && "(Ljava/lang/Class;)Lorg/apache/beam/sdk/coders/SerializableCoder;".equals(descriptor)) {
                        super.visitLdcInsn(plugin);
                        super.visitMethodInsn(opcode,
                                "org/talend/sdk/component/runtime/beam/coder/ContextualSerializableCoder", "of",
                                "(Ljava/lang/Class;Ljava/lang/String;)Lorg/apache/beam/sdk/coders/SerializableCoder;",
                                false);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }

                @Override
                public void visitMaxs(final int maxStack, final int maxLocals) {
                    super.visitMaxs(-1, -1);
                }
            };
        }
    }

    public static class SerializationWrapper implements Serializable {

        private final String plugin;

        private final byte[] delegateBytes;

        public SerializationWrapper(final Object delegate, final String plugin) {
            this.plugin = plugin;
            this.delegateBytes = serialize(delegate);
            if (DEBUG) {
                try {
                    readResolve();
                } catch (final ObjectStreamException e) {
                    log.debug("Serialization BUG: " + e.getMessage(), e);
                }
            }
        }

        private byte[] serialize(final Object delegate) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (DEBUG) {
                log.debug("serializing {}", delegate);
            }
            BYPASS_REPLACE_SERIALIZER.accept(baos, delegate);
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
            return delegate == null ? null : new SerializationWrapper(delegate, plugin);
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

        private final ClassVisitor writer;

        private final String plugin;

        private boolean hasWriteReplace;

        private ComponentClassVisitor(final ClassVisitor cv, final String plugin) {
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

        private void createSerialisation(final ClassVisitor cw, final String pluginId) {
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
