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

import static org.apache.xbean.asm5.Opcodes.ACC_PRIVATE;
import static org.apache.xbean.asm5.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm5.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm5.Opcodes.ACC_SYNTHETIC;
import static org.apache.xbean.asm5.Opcodes.ALOAD;
import static org.apache.xbean.asm5.Opcodes.ARETURN;
import static org.apache.xbean.asm5.Opcodes.CHECKCAST;
import static org.apache.xbean.asm5.Opcodes.DRETURN;
import static org.apache.xbean.asm5.Opcodes.DUP;
import static org.apache.xbean.asm5.Opcodes.FRETURN;
import static org.apache.xbean.asm5.Opcodes.GETFIELD;
import static org.apache.xbean.asm5.Opcodes.INVOKEINTERFACE;
import static org.apache.xbean.asm5.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm5.Opcodes.INVOKEVIRTUAL;
import static org.apache.xbean.asm5.Opcodes.IRETURN;
import static org.apache.xbean.asm5.Opcodes.LRETURN;
import static org.apache.xbean.asm5.Opcodes.NEW;
import static org.apache.xbean.asm5.Opcodes.PUTFIELD;
import static org.apache.xbean.asm5.Opcodes.RETURN;
import static org.apache.xbean.asm5.Opcodes.V1_8;

import java.beans.Introspector;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.apache.xbean.asm5.ClassWriter;
import org.apache.xbean.asm5.MethodVisitor;
import org.apache.xbean.asm5.Type;
import org.talend.sdk.component.api.processor.data.ObjectMap;

import lombok.AllArgsConstructor;

// highly inspired from openwebbeans proxying code
//
// goal is mainly to add a writeReplace method to ensure services are serializable when not done by the developer.
public class ProxyGenerator implements Serializable {

    public static final String OBJECT_MAP_NAME = Type.getInternalName(ObjectMap.class);

    // internal, used by serialization
    private static final ProxyGenerator SINGLETON = new ProxyGenerator();

    private final int javaVersion;

    public ProxyGenerator() {
        javaVersion = determineJavaVersion();
    }

    private int determineJavaVersion() {
        final String javaVersionProp = System.getProperty("java.version", "1.8");
        if (javaVersionProp.startsWith("1.8")) { // we don't support earlier
            return V1_8;
        }
        // add java 9 test when upgrading asm
        return V1_8; // return higher one
    }

    private void createSerialisation(final ClassWriter cw, final String pluginId, final String key) {
        final MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null,
                new String[] { Type.getType(ObjectStreamException.class).getInternalName() });

        mv.visitCode();

        mv.visitTypeInsn(NEW, "org/talend/sdk/component/runtime/serialization/SerializableService");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(pluginId);
        mv.visitLdcInsn(key);
        mv.visitMethodInsn(INVOKESPECIAL, "org/talend/sdk/component/runtime/serialization/SerializableService", "<init>",
                "(Ljava/lang/String;Ljava/lang/String;)V", false);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void createConstructor(final ClassWriter cw, final Class<?> classToProxy, final String classFileName,
            final Constructor<?> constructor) {
        try {
            Constructor superDefaultCt;
            String parentClassFileName;
            String[] exceptions = null;
            if (classToProxy.isInterface()) {
                parentClassFileName = Type.getInternalName(Object.class);
                superDefaultCt = Object.class.getConstructor();
            } else {
                parentClassFileName = classFileName;
                if (constructor == null) {
                    superDefaultCt = classToProxy.getConstructor();
                } else {
                    superDefaultCt = constructor;

                    Class<?>[] exceptionTypes = constructor.getExceptionTypes();
                    exceptions = exceptionTypes.length == 0 ? null : new String[exceptionTypes.length];
                    for (int i = 0; i < exceptionTypes.length; i++) {
                        exceptions[i] = Type.getDescriptor(exceptionTypes[i]);
                    }
                }
            }

            String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, exceptions);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            if (constructor != null) {
                for (int i = 1; i <= constructor.getParameterTypes().length; i++) {
                    mv.visitVarInsn(ALOAD, i);
                }
            }
            mv.visitMethodInsn(INVOKESPECIAL, parentClassFileName, "<init>", descriptor, false);

            mv.visitInsn(RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> String getSignedClassProxyName(final Class<T> classToProxy) {
        // avoid java.lang.SecurityException: class's signer information
        // does not match signer information of other classes in the same package
        return "org.talend.generated.proxy.signed." + classToProxy.getName();
    }

    private String fixPreservedPackages(String proxyClassName) {
        proxyClassName = fixPreservedPackage(proxyClassName, "java.");
        proxyClassName = fixPreservedPackage(proxyClassName, "javax.");
        proxyClassName = fixPreservedPackage(proxyClassName, "sun.misc.");

        return proxyClassName;
    }

    private String fixPreservedPackage(final String className, final String forbiddenPackagePrefix) {
        String fixedClassName = className;

        if (className.startsWith(forbiddenPackagePrefix)) {
            fixedClassName = "org.talend.generated.proxy.custom." + className.substring(forbiddenPackagePrefix.length());
        }

        return fixedClassName;
    }

    public Class<?> generateProxy(final ClassLoader loader, final Class<?> classToProxy, final String plugin, final String key) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String proxyClassName = fixPreservedPackages(
                (classToProxy.getSigners() != null ? getSignedClassProxyName(classToProxy) : classToProxy.getName())
                        + "$$TalendServiceProxy");
        final String classFileName = proxyClassName.replace('.', '/');

        final String[] interfaceNames = { Type.getInternalName(Serializable.class) };
        final String superClassName = Type.getInternalName(classToProxy);

        cw.visit(javaVersion, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, classFileName, null, superClassName, interfaceNames);
        cw.visitSource(classFileName + ".java", null);

        createSerialisation(cw, plugin, key);

        createConstructor(cw, classToProxy, superClassName, Stream.of(classToProxy.getDeclaredConstructors()).filter(c -> {
            final int modifiers = c.getModifiers();
            return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
        }).sorted((o1, o2) -> { // prefer public constructor and then the smallest parameter count
            final int mod1 = o1.getModifiers();
            final int mod2 = o2.getModifiers();
            if (Modifier.isProtected(mod1) && !Modifier.isPublic(mod2)) {
                return 1;
            }
            if (Modifier.isProtected(mod2) && !Modifier.isPublic(mod1)) {
                return -1;
            }

            return o1.getParameterCount() - o2.getParameterCount();
        }).findFirst().orElseThrow(
                () -> new IllegalArgumentException(classToProxy + " has no default constructor, put at least a protected one")));

        return Unsafes.defineAndLoadClass(loader, proxyClassName, cw.toByteArray());
    }

    public Class<?> subclass(final ClassLoader loader, final Class<?> classToProxy) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String proxyClassName = fixPreservedPackages(
                (classToProxy.getSigners() != null ? getSignedClassProxyName(classToProxy) : classToProxy.getName()))
                + "$$Subclass";

        final String classFileName = proxyClassName.replace('.', '/');

        final String superClassName = Type.getInternalName(classToProxy);

        cw.visit(javaVersion, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, classFileName, null, superClassName,
                new String[] { Type.getInternalName(GetObjectMap.class) });
        cw.visitSource(classFileName + ".java", null);

        // private ObjectMap this$map; (weird field value to ensure we don't conflict and proxying is obvious)
        cw.visitField(ACC_PRIVATE, "this$map", "Lorg/talend/sdk/component/api/processor/data/ObjectMap;", null, null).visitEnd();

        {
            final MethodVisitor getObjectMap = cw.visitMethod(ACC_PUBLIC, "getObjectMap", "()Lorg/talend/sdk/component/api/processor/data/ObjectMap;", null, null);
            getObjectMap.visitCode();
            getObjectMap.visitVarInsn(ALOAD, 0);
            getObjectMap.visitFieldInsn(GETFIELD, classFileName, "this$map", "Lorg/talend/sdk/component/api/processor/data/ObjectMap;");
            getObjectMap.visitInsn(ARETURN);
            getObjectMap.visitMaxs(1, 1);
            getObjectMap.visitEnd();
        }

        try {
            createObjectMapConstructor(cw, superClassName, classFileName, classToProxy.getConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    classToProxy + " can't be proxied, check your chain. Did you use ObjectMap as intended?");
        }

        proxyGetters(
                cw, classFileName, Stream
                        .of(classToProxy.getMethods()).filter(m -> (m.getName().startsWith("get") || m.getName().startsWith("is"))
                                && m.getParameterCount() == 0 && m.getReturnType() != void.class)
                        .filter(m -> !unproxyableMethod(m)));

        return Unsafes.defineAndLoadClass(loader, proxyClassName, cw.toByteArray());
    }

    private void proxyGetters(final ClassWriter cw, final String classFileName, final Stream<Method> getters) {
        getters.forEach(m -> {
            final String key = Introspector
                    .decapitalize(m.getName().substring((m.getName().startsWith("get") ? "get" : "is").length()));

            final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), "()" + Type.getDescriptor(m.getReturnType()), null,
                    null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, classFileName, "this$map", "L" + OBJECT_MAP_NAME + ";");
            mv.visitLdcInsn(key);
            mv.visitMethodInsn(INVOKEINTERFACE, OBJECT_MAP_NAME, "get", "(Ljava/lang/String;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, getCastType(m.getReturnType()));
            if (m.getReturnType().isPrimitive()) {
                mv.visitMethodInsn(INVOKEVIRTUAL, getWrapperType(m.getReturnType()), getPrimitiveMethod(m.getReturnType()),
                        "()" + Type.getDescriptor(m.getReturnType()), false);
            } // else todo: nested support generating/getting nested subclasses etc
            mv.visitInsn(getReturnInsn(m.getReturnType()));
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        });
    }

    private void createObjectMapConstructor(final ClassWriter cw, final String parentClassFileName, final String classFileName,
            final Constructor<?> constructor) {
        final String[] exceptions = new String[constructor.getExceptionTypes().length];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i] = Type.getInternalName(constructor.getExceptionTypes()[i]);
        }

        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(L" + OBJECT_MAP_NAME + ";)V", null, exceptions);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, parentClassFileName, "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, classFileName, "this$map", "L" + OBJECT_MAP_NAME + ";");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private boolean unproxyableMethod(final Method delegatedMethod) {
        final int modifiers = delegatedMethod.getModifiers();

        return (modifiers & (Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL | Modifier.NATIVE)) > 0
                || "finalize".equals(delegatedMethod.getName()) || delegatedMethod.isBridge();
    }

    private String getWrapperType(final Class<?> type) {
        if (Integer.TYPE.equals(type)) {
            return Integer.class.getCanonicalName().replace('.', '/');
        } else if (Boolean.TYPE.equals(type)) {
            return Boolean.class.getCanonicalName().replace('.', '/');
        } else if (Character.TYPE.equals(type)) {
            return Character.class.getCanonicalName().replace('.', '/');
        } else if (Byte.TYPE.equals(type)) {
            return Byte.class.getCanonicalName().replace('.', '/');
        } else if (Short.TYPE.equals(type)) {
            return Short.class.getCanonicalName().replace('.', '/');
        } else if (Float.TYPE.equals(type)) {
            return Float.class.getCanonicalName().replace('.', '/');
        } else if (Long.TYPE.equals(type)) {
            return Long.class.getCanonicalName().replace('.', '/');
        } else if (Double.TYPE.equals(type)) {
            return Double.class.getCanonicalName().replace('.', '/');
        } else if (Void.TYPE.equals(type)) {
            return Void.class.getCanonicalName().replace('.', '/');
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    private int getReturnInsn(final Class<?> type) {
        if (type.isPrimitive()) {
            if (Void.TYPE.equals(type)) {
                return RETURN;
            }
            if (Integer.TYPE.equals(type)) {
                return IRETURN;
            }
            if (Boolean.TYPE.equals(type)) {
                return IRETURN;
            }
            if (Character.TYPE.equals(type)) {
                return IRETURN;
            }
            if (Byte.TYPE.equals(type)) {
                return IRETURN;
            }
            if (Short.TYPE.equals(type)) {
                return IRETURN;
            }
            if (Float.TYPE.equals(type)) {
                return FRETURN;
            }
            if (Long.TYPE.equals(type)) {
                return LRETURN;
            }
            if (Double.TYPE.equals(type)) {
                return DRETURN;
            }
        }

        return ARETURN;
    }

    private String getCastType(final Class<?> returnType) {
        if (returnType.isPrimitive()) {
            return getWrapperType(returnType);
        }
        return Type.getInternalName(returnType);
    }

    private String getPrimitiveMethod(Class<?> type) {
        if (Integer.TYPE.equals(type)) {
            return "intValue";
        } else if (Boolean.TYPE.equals(type)) {
            return "booleanValue";
        } else if (Character.TYPE.equals(type)) {
            return "charValue";
        } else if (Byte.TYPE.equals(type)) {
            return "byteValue";
        } else if (Short.TYPE.equals(type)) {
            return "shortValue";
        } else if (Float.TYPE.equals(type)) {
            return "floatValue";
        } else if (Long.TYPE.equals(type)) {
            return "longValue";
        } else if (Double.TYPE.equals(type)) {
            return "doubleValue";
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    Object writeReplace() throws ObjectStreamException {
        return new Replacer();
    }

    @AllArgsConstructor
    private static class Replacer implements Serializable {

        Object readResolve() throws ObjectStreamException {
            return ProxyGenerator.SINGLETON;
        }
    }

    public interface GetObjectMap {

        ObjectMap getObjectMap();
    }
}
