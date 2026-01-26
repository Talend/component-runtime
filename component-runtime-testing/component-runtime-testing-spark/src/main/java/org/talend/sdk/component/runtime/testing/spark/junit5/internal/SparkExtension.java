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
package org.talend.sdk.component.runtime.testing.spark.junit5.internal;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.internal.TemporaryFolderExtension;
import org.talend.sdk.component.runtime.testing.spark.internal.BaseSpark;
import org.talend.sdk.component.runtime.testing.spark.internal.SparkVersions;
import org.talend.sdk.component.runtime.testing.spark.junit5.SparkInject;
import org.talend.sdk.component.runtime.testing.spark.junit5.WithSpark;

public class SparkExtension extends BaseSpark<SparkExtension>
        implements BeforeAllCallback, AfterAllCallback, JUnit5InjectionSupport {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(SparkExtension.class.getName());

    private final TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    private File root;

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        temporaryFolderExtension.beforeAll(extensionContext);
        root = TemporaryFolder.class
                .cast(temporaryFolderExtension.findInstance(extensionContext, TemporaryFolder.class))
                .getRoot();

        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        store.put(BaseSpark.class.getName(), this);
        AnnotationUtils.findAnnotation(extensionContext.getElement(), WithSpark.class).ifPresent(ws -> {
            withSlaves(ws.slaves());
            withHadoopBase(ws.hadoopBase());
            withHadoopVersion(ws.hadoopVersion());
            withInstallWinUtils(ws.installWinUtils());
            withScalaVersion(of(ws.scalaVersion())
                    .filter(it -> !"auto".equals(it))
                    .orElse(SparkVersions.SPARK_SCALA_VERSION.getValue()));
            withSparkVersion(of(ws.sparkVersion())
                    .filter(it -> !"auto".equals(it))
                    .orElse(SparkVersions.SPARK_VERSION.getValue()));
        });
        final Instances instances = start();
        if (instances.getException() != null) {
            instances.close();
            if (Exception.class.isInstance(instances.getException())) {
                throw Exception.class.cast(instances.getException());
            }
            if (Error.class.isInstance(instances.getException())) {
                throw Error.class.cast(instances.getException());
            }
            throw new IllegalStateException(instances.getException());
        }
        store.put(AutoCloseable.class, instances);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) throws Exception {
        try {
            ofNullable(extensionContext.getStore(NAMESPACE).get(AutoCloseable.class))
                    .map(AutoCloseable.class::cast)
                    .ifPresent(c -> {
                        try {
                            c.close();
                        } catch (final Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
        } finally {
            temporaryFolderExtension.afterAll(extensionContext);
        }
    }

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return SparkInject.class;
    }

    @Override
    protected void fail(final String message) {
        Assertions.fail(message);
    }

    @Override
    protected void assertTrue(final String message, final boolean value) {
        Assertions.assertTrue(value, message);
    }

    @Override
    protected File getRoot() {
        return root;
    }
}
