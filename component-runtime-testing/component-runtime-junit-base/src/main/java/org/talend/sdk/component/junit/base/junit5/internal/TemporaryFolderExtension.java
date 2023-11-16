/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.base.junit5.internal;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.junit.base.junit5.TempFolder;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;

@Deprecated // part of jupiter 5.4 now
public class TemporaryFolderExtension implements BeforeAllCallback, AfterAllCallback, JUnit5InjectionSupport {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TemporaryFolderExtension.class.getName());

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final TemporaryFolder temporaryFolder = new TemporaryFolder();
        context.getStore(NAMESPACE).put(TemporaryFolder.class.getName(), temporaryFolder);
        temporaryFolder.create();
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        TemporaryFolder.class.cast(context.getStore(NAMESPACE).get(TemporaryFolder.class.getName())).delete();
    }

    @Override
    public boolean supports(final Class<?> type) {
        return TemporaryFolder.class == type;
    }

    @Override
    public Object findInstance(final ExtensionContext extensionContext, final Class<?> type) {
        return extensionContext.getStore(NAMESPACE).get(TemporaryFolder.class.getName());
    }

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return TempFolder.class;
    }
}
