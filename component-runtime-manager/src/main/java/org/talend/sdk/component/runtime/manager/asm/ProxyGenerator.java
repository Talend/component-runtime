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
package org.talend.sdk.component.runtime.manager.asm;

import static java.util.stream.Collectors.toList;
import static org.apache.xbean.asm9.ClassReader.SKIP_CODE;
import static org.apache.xbean.asm9.ClassReader.SKIP_DEBUG;
import static org.apache.xbean.asm9.ClassReader.SKIP_FRAMES;
import static org.apache.xbean.asm9.Opcodes.AALOAD;
import static org.apache.xbean.asm9.Opcodes.AASTORE;
import static org.apache.xbean.asm9.Opcodes.ACC_PRIVATE;
import static org.apache.xbean.asm9.Opcodes.ACC_PROTECTED;
import static org.apache.xbean.asm9.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm9.Opcodes.ACC_STATIC;
import static org.apache.xbean.asm9.Opcodes.ACC_SUPER;
import static org.apache.xbean.asm9.Opcodes.ACC_SYNTHETIC;
import static org.apache.xbean.asm9.Opcodes.ACC_VARARGS;
import static org.apache.xbean.asm9.Opcodes.ACONST_NULL;
import static org.apache.xbean.asm9.Opcodes.ALOAD;
import static org.apache.xbean.asm9.Opcodes.ANEWARRAY;
import static org.apache.xbean.asm9.Opcodes.ARETURN;
import static org.apache.xbean.asm9.Opcodes.ASTORE;
import static org.apache.xbean.asm9.Opcodes.ATHROW;
import static org.apache.xbean.asm9.Opcodes.BIPUSH;
import static org.apache.xbean.asm9.Opcodes.CHECKCAST;
import static org.apache.xbean.asm9.Opcodes.DLOAD;
import static org.apache.xbean.asm9.Opcodes.DRETURN;
import static org.apache.xbean.asm9.Opcodes.DUP;
import static org.apache.xbean.asm9.Opcodes.FLOAD;
import static org.apache.xbean.asm9.Opcodes.FRETURN;
import static org.apache.xbean.asm9.Opcodes.GETFIELD;
import static org.apache.xbean.asm9.Opcodes.GETSTATIC;
import static org.apache.xbean.asm9.Opcodes.ICONST_0;
import static org.apache.xbean.asm9.Opcodes.ICONST_1;
import static org.apache.xbean.asm9.Opcodes.ICONST_2;
import static org.apache.xbean.asm9.Opcodes.ICONST_3;
import static org.apache.xbean.asm9.Opcodes.ICONST_4;
import static org.apache.xbean.asm9.Opcodes.ICONST_5;
import static org.apache.xbean.asm9.Opcodes.IFEQ;
import static org.apache.xbean.asm9.Opcodes.ILOAD;
import static org.apache.xbean.asm9.Opcodes.INVOKEINTERFACE;
import static org.apache.xbean.asm9.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm9.Opcodes.INVOKESTATIC;
import static org.apache.xbean.asm9.Opcodes.INVOKEVIRTUAL;
import static org.apache.xbean.asm9.Opcodes.IRETURN;
import static org.apache.xbean.asm9.Opcodes.LLOAD;
import static org.apache.xbean.asm9.Opcodes.LRETURN;
import static org.apache.xbean.asm9.Opcodes.NEW;
import static org.apache.xbean.asm9.Opcodes.POP;
import static org.apache.xbean.asm9.Opcodes.PUTFIELD;
import static org.apache.xbean.asm9.Opcodes.RETURN;
import static org.apache.xbean.asm9.Opcodes.SIPUSH;
import static org.apache.xbean.asm9.Opcodes.V1_8;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.Label;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.shade.commons.EmptyVisitor;
import org.talend.sdk.component.api.service.interceptor.InterceptorHandler;
import org.talend.sdk.component.api.service.interceptor.Intercepts;
import org.talend.sdk.component.runtime.manager.interceptor.InterceptorHandlerFacade;

import lombok.AllArgsConstructor;

// highly inspired from openwebbeans proxying code
//
// goal is mainly to add a writeReplace method to ensure services are serializable when not done by the developer.
public class ProxyGenerator implements Serializable {

    private static final String FIELD_INTERCEPTOR_HANDLER = "tacokitIntDecHandler";

    private static final String FIELD_INTERCEPTED_METHODS = "tacokitIntDecMethods";

    // internal, used by serialization
    private static final ProxyGenerator SINGLETON = new ProxyGenerator();

    private final int javaVersion;

    public ProxyGenerator() {
        javaVersion = determineDefaultJavaVersion();
    }

    private int determineDefaultJavaVersion() {
        final String javaVersionProp = System.getProperty("java.version", "1.8");
        if (javaVersionProp.startsWith("1.8")) { // we don't support earlier
            return V1_8;
        }
        // add java 9 test when upgrading asm
        return V1_8; // return higher one
    }

    private void createSerialisation(final ClassWriter cw, final String pluginId, final String key) {
        final MethodVisitor mv = cw
                .visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null,
                        new String[] { Type.getType(ObjectStreamException.class).getInternalName() });

        mv.visitCode();

        mv.visitTypeInsn(NEW, "org/talend/sdk/component/runtime/serialization/SerializableService");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(pluginId);
        mv.visitLdcInsn(key);
        mv
                .visitMethodInsn(INVOKESPECIAL, "org/talend/sdk/component/runtime/serialization/SerializableService",
                        "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private String createConstructor(final ClassWriter cw, final Class<?> classToProxy, final String classFileName,
            final String proxyClassFileName, final Constructor<?> constructor, final boolean withInterceptors) {
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
                        exceptions[i] = Type.getInternalName(exceptionTypes[i]);
                    }
                }
            }

            final String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, exceptions);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            if (constructor != null) {
                for (int i = 1; i <= constructor.getParameterTypes().length; i++) {
                    mv.visitVarInsn(ALOAD, i);
                }
            }
            mv.visitMethodInsn(INVOKESPECIAL, parentClassFileName, "<init>", descriptor, false);

            if (withInterceptors) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(ACONST_NULL);
                mv
                        .visitFieldInsn(PUTFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER,
                                Type.getDescriptor(InterceptorHandler.class));
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();

            return parentClassFileName;
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private void delegateMethod(final ClassWriter cw, final Method method, final String proxyClassFileName,
            final int methodIndex) {
        final Class<?> returnType = method.getReturnType();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        final int modifiers = method.getModifiers();
        if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalStateException("It's not possible to proxy a final or static method: "
                    + method.getDeclaringClass().getName() + " " + method.getName());
        }

        // push the method definition
        int modifier = modifiers & (ACC_PUBLIC | ACC_PROTECTED | ACC_VARARGS);

        MethodVisitor mv = cw.visitMethod(modifier, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        // push try/catch block, to catch declared exceptions, and to catch java.lang.Throwable
        final Label l0 = new Label();
        final Label l1 = new Label();
        final Label l2 = new Label();

        if (exceptionTypes.length > 0) {
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
        }

        // push try code
        mv.visitLabel(l0);
        final String classNameToOverride = method.getDeclaringClass().getName().replace('.', '/');
        mv.visitLdcInsn(Type.getType("L" + classNameToOverride + ";"));

        // the following code generates the bytecode for this line of Java:
        // Method method = <proxy>.class.getMethod("add", new Class[] { <array of function argument classes> });

        // get the method name to invoke, and push to stack
        mv.visitLdcInsn(method.getName());

        // create the Class[]
        createArrayDefinition(mv, parameterTypes.length, Class.class);

        int length = 1;

        // push parameters into array
        for (int i = 0; i < parameterTypes.length; i++) {
            // keep copy of array on stack
            mv.visitInsn(DUP);

            Class<?> parameterType = parameterTypes[i];

            // push number onto stack
            pushIntOntoStack(mv, i);

            if (parameterType.isPrimitive()) {
                String wrapperType = getWrapperType(parameterType);
                mv.visitFieldInsn(GETSTATIC, wrapperType, "TYPE", "Ljava/lang/Class;");
            } else {
                mv.visitLdcInsn(Type.getType(parameterType));
            }

            mv.visitInsn(AASTORE);

            if (Long.TYPE.equals(parameterType) || Double.TYPE.equals(parameterType)) {
                length += 2;
            } else {
                length++;
            }
        }

        // the following code generates bytecode equivalent to:
        // return ((<returntype>) invocationHandler.invoke(this, {methodIndex}, new Object[] { <function arguments
        // }))[.<primitive>Value()];

        final Label l4 = new Label();
        mv.visitLabel(l4);

        mv.visitVarInsn(ALOAD, 0);

        // get the invocationHandler field from this class
        mv
                .visitFieldInsn(GETFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER,
                        Type.getDescriptor(InterceptorHandler.class));

        // add the Method from the static array as first parameter
        mv.visitFieldInsn(GETSTATIC, proxyClassFileName, FIELD_INTERCEPTED_METHODS, Type.getDescriptor(Method[].class));

        // push the methodIndex of the current method
        if (methodIndex < 128) {
            mv.visitIntInsn(BIPUSH, methodIndex);
        } else if (methodIndex < 32267) {
            // for methods > 127 we need to push a short number as index
            mv.visitIntInsn(SIPUSH, methodIndex);
        } else {
            throw new IllegalStateException("Sorry, we only support Classes with 2^15 methods...");
        }

        // and now load the Method from the array
        mv.visitInsn(AALOAD);

        // prepare the parameter array as Object[] and store it on the stack
        pushMethodParameterArray(mv, parameterTypes);

        // invoke the invocationHandler
        mv
                .visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(InterceptorHandler.class), "invoke",
                        "(Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", true);

        // cast the result
        mv.visitTypeInsn(CHECKCAST, getCastType(returnType));

        if (returnType.isPrimitive() && (!Void.TYPE.equals(returnType))) {
            // get the primitive value
            mv
                    .visitMethodInsn(INVOKEVIRTUAL, getWrapperType(returnType), getPrimitiveMethod(returnType),
                            "()" + Type.getDescriptor(returnType), false);
        }

        // push return
        mv.visitLabel(l1);
        if (!Void.TYPE.equals(returnType)) {
            mv.visitInsn(getReturnInsn(returnType));
        } else {
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
        }

        // catch InvocationTargetException
        if (exceptionTypes.length > 0) {
            mv.visitLabel(l2);
            mv.visitVarInsn(ASTORE, length);

            Label l5 = new Label();
            mv.visitLabel(l5);

            for (int i = 0; i < exceptionTypes.length; i++) {
                Class<?> exceptionType = exceptionTypes[i];

                mv.visitLdcInsn(Type.getType("L" + exceptionType.getCanonicalName().replace('.', '/') + ";"));
                mv.visitVarInsn(ALOAD, length);
                mv
                        .visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                                "()Ljava/lang/Throwable;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);

                final Label l6 = new Label();
                mv.visitJumpInsn(IFEQ, l6);

                final Label l7 = new Label();
                mv.visitLabel(l7);

                mv.visitVarInsn(ALOAD, length);
                mv
                        .visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                                "()Ljava/lang/Throwable;", false);
                mv.visitTypeInsn(CHECKCAST, getCastType(exceptionType));
                mv.visitInsn(ATHROW);
                mv.visitLabel(l6);

                if (i == (exceptionTypes.length - 1)) {
                    mv.visitTypeInsn(NEW, "java/lang/reflect/UndeclaredThrowableException");
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ALOAD, length);
                    mv
                            .visitMethodInsn(INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>",
                                    "(Ljava/lang/Throwable;)V", false);
                    mv.visitInsn(ATHROW);
                }
            }
        }

        // finish this method
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void pushMethodParameterArray(final MethodVisitor mv, final Class<?>[] parameterTypes) {
        // need to construct the array of objects passed in
        // create the Object[]
        createArrayDefinition(mv, parameterTypes.length, Object.class);

        int index = 1;
        // push parameters into array
        for (int i = 0; i < parameterTypes.length; i++) {
            // keep copy of array on stack
            mv.visitInsn(DUP);

            final Class<?> parameterType = parameterTypes[i];

            // push number onto stack
            pushIntOntoStack(mv, i);

            if (parameterType.isPrimitive()) {
                final String wrapperType = getWrapperType(parameterType);
                mv.visitVarInsn(getVarInsn(parameterType), index);

                mv
                        .visitMethodInsn(INVOKESTATIC, wrapperType, "valueOf",
                                "(" + Type.getDescriptor(parameterType) + ")L" + wrapperType + ";", false);
                mv.visitInsn(AASTORE);

                if (Long.TYPE.equals(parameterType) || Double.TYPE.equals(parameterType)) {
                    index += 2;
                } else {
                    index++;
                }
            } else {
                mv.visitVarInsn(ALOAD, index);
                mv.visitInsn(AASTORE);
                index++;
            }
        }
    }

    private int getVarInsn(final Class<?> type) {
        if (Integer.TYPE.equals(type)) {
            return ILOAD;
        } else if (Boolean.TYPE.equals(type)) {
            return ILOAD;
        } else if (Character.TYPE.equals(type)) {
            return ILOAD;
        } else if (Byte.TYPE.equals(type)) {
            return ILOAD;
        } else if (Short.TYPE.equals(type)) {
            return ILOAD;
        } else if (Float.TYPE.equals(type)) {
            return FLOAD;
        } else if (Long.TYPE.equals(type)) {
            return LLOAD;
        } else if (Double.TYPE.equals(type)) {
            return DLOAD;
        }
        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    private void pushIntOntoStack(final MethodVisitor mv, final int i) {
        if (i == 0) {
            mv.visitInsn(ICONST_0);
        } else if (i == 1) {
            mv.visitInsn(ICONST_1);
        } else if (i == 2) {
            mv.visitInsn(ICONST_2);
        } else if (i == 3) {
            mv.visitInsn(ICONST_3);
        } else if (i == 4) {
            mv.visitInsn(ICONST_4);
        } else if (i == 5) {
            mv.visitInsn(ICONST_5);
        } else if (i > 5 && i <= 255) {
            mv.visitIntInsn(BIPUSH, i);
        } else {
            mv.visitIntInsn(SIPUSH, i);
        }
    }

    private void createArrayDefinition(final MethodVisitor mv, final int size, final Class<?> type) {
        if (size < 0) {
            throw new IllegalStateException("Array size cannot be less than zero");
        }

        pushIntOntoStack(mv, size);

        mv.visitTypeInsn(ANEWARRAY, type.getCanonicalName().replace('.', '/'));
    }

    private <T> String getSignedClassProxyName(final Class<T> classToProxy) {
        // avoid java.lang.SecurityException: class's signer information
        // does not match signer information of other classes in the same package
        return "org.talend.generated.proxy.signed." + classToProxy.getName();
    }

    private String fixPreservedPackages(final String name) {
        String proxyClassName = name;
        proxyClassName = fixPreservedPackage(proxyClassName, "java.");
        proxyClassName = fixPreservedPackage(proxyClassName, "javax.");
        proxyClassName = fixPreservedPackage(proxyClassName, "sun.misc.");
        return proxyClassName;
    }

    private String fixPreservedPackage(final String className, final String forbiddenPackagePrefix) {
        String fixedClassName = className;

        if (className.startsWith(forbiddenPackagePrefix)) {
            fixedClassName =
                    "org.talend.generated.proxy.custom." + className.substring(forbiddenPackagePrefix.length());
        }

        return fixedClassName;
    }

    private int findJavaVersion(final Class<?> from) {
        final String resource = from.getName().replace('.', '/') + ".class";
        try (final InputStream stream = from.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return javaVersion;
            }
            final ClassReader reader = new ClassReader(stream);
            final VersionVisitor visitor = new VersionVisitor();
            reader.accept(visitor, SKIP_DEBUG + SKIP_CODE + SKIP_FRAMES);
            if (visitor.version != 0) {
                return visitor.version;
            }
        } catch (final Exception e) {
            // no-op
        }
        // mainly for JVM classes - outside the classloader, find to fallback on the JVM
        // version
        return javaVersion;
    }

    public boolean hasInterceptors(final Class<?> type) {
        return Stream
                .concat(Stream.of(type.getAnnotations()),
                        Stream.of(type.getMethods()).flatMap(m -> Stream.of(m.getAnnotations())))
                .anyMatch(this::isInterceptor);
    }

    private boolean isInterceptor(final Annotation a) {
        return a.annotationType().isAnnotationPresent(Intercepts.class);
    }

    public boolean isSerializable(final Class<?> type) {
        if (Serializable.class.isAssignableFrom(type)) {
            return true;
        }
        try {
            type.getMethod("writeReplace");
            return true;
        } catch (final NoSuchMethodException e) {
            // no-op, let's try to generate a proxy
        }
        return false;
    }

    public Class<?> generateProxy(final ClassLoader loader, final Class<?> classToProxy, final String plugin,
            final String key) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String proxyClassName = fixPreservedPackages(
                (classToProxy.getSigners() != null ? getSignedClassProxyName(classToProxy) : classToProxy.getName())
                        + "$$TalendServiceProxy");
        final String classFileName = proxyClassName.replace('.', '/');

        final String[] interfaceNames = { Type.getInternalName(Serializable.class) };
        final String superClassName = Type.getInternalName(classToProxy);

        cw
                .visit(findJavaVersion(classToProxy), ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, classFileName, null,
                        superClassName, interfaceNames);
        cw.visitSource(classFileName + ".java", null);

        if (!Serializable.class.isAssignableFrom(classToProxy)) {
            try {
                classToProxy.getMethod("writeReplace");
            } catch (final NoSuchMethodException e) {
                createSerialisation(cw, plugin, key);
            }
        }

        final boolean hasInterceptors = hasInterceptors(classToProxy);
        if (hasInterceptors) {
            cw
                    .visitField(ACC_PRIVATE, FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class),
                            null, null)
                    .visitEnd();
            cw
                    .visitField(ACC_PRIVATE | ACC_STATIC, FIELD_INTERCEPTED_METHODS, Type.getDescriptor(Method[].class),
                            null, null)
                    .visitEnd();
        }

        createConstructor(cw, classToProxy, superClassName, classFileName,
                Stream.of(classToProxy.getDeclaredConstructors()).filter(c -> {
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
                })
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                classToProxy + " has no default constructor, put at least a protected one")),
                hasInterceptors);

        final Method[] interceptedMethods;
        if (hasInterceptors) {
            final Collection<Annotation> globalInterceptors =
                    Stream.of(classToProxy.getAnnotations()).filter(this::isInterceptor).collect(toList());
            final AtomicInteger methodIndex = new AtomicInteger();
            interceptedMethods = Stream
                    .of(classToProxy.getMethods())
                    .filter(m -> !"<init>".equals(m.getName()) && (!globalInterceptors.isEmpty()
                            || Stream.of(m.getAnnotations()).anyMatch(this::isInterceptor)))
                    .peek(method -> delegateMethod(cw, method, classFileName, methodIndex.getAndIncrement()))
                    .toArray(Method[]::new);
        } else {
            interceptedMethods = null;
        }

        final Class<Object> objectClass = Unsafes.defineAndLoadClass(loader, proxyClassName, cw.toByteArray());
        if (hasInterceptors) {
            try {
                final Field interceptedMethodsField = objectClass.getDeclaredField(FIELD_INTERCEPTED_METHODS);
                interceptedMethodsField.setAccessible(true);
                interceptedMethodsField.set(null, interceptedMethods);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return objectClass;
    }

    public void initialize(final Object proxy, final InterceptorHandler handler) {
        try {
            final Field invocationHandlerField = proxy.getClass().getDeclaredField(FIELD_INTERCEPTOR_HANDLER);
            invocationHandlerField.setAccessible(true);
            invocationHandlerField.set(proxy, handler);
        } catch (final IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public InterceptorHandlerFacade getHandler(final Object instance) {
        try {
            final Field invocationHandlerField = instance.getClass().getDeclaredField(FIELD_INTERCEPTOR_HANDLER);
            if (!invocationHandlerField.isAccessible()) {
                invocationHandlerField.setAccessible(true);
            }
            return InterceptorHandlerFacade.class.cast(invocationHandlerField.get(instance));
        } catch (final IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
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

    private String getPrimitiveMethod(final Class<?> type) {
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

    private static class VersionVisitor extends EmptyVisitor {

        private int version;

        @Override
        public void visit(final int version, final int access, final String name, final String signature,
                final String superName, final String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.version = version;
        }
    }
}
