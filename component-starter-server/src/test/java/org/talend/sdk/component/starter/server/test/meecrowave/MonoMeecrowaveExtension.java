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
package org.talend.sdk.component.starter.server.test.meecrowave;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.meecrowave.testing.Injector;
import org.apache.meecrowave.testing.MonoBase;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// until Meecrowave 1.2.1 is released -- MEECROWAVE-90
public class MonoMeecrowaveExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final MonoBase BASE = new MonoBase();

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(org.apache.meecrowave.junit5.MonoMeecrowaveExtension.class.getName());

    @Override
    public void beforeAll(final ExtensionContext context) {
        context.getStore(NAMESPACE).put(MonoBase.Instance.class.getName(), BASE.startIfNeeded());
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        context.getStore(NAMESPACE).put(CreationalContext.class.getName(),
                Injector.inject(context.getTestInstance().orElse(null)));
        Injector.injectConfig(MonoBase.Instance.class
                .cast(context.getStore(NAMESPACE).get(MonoBase.Instance.class.getName()))
                .getConfiguration(), context.getTestInstance().orElse(null));
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        CreationalContext.class.cast(context.getStore(NAMESPACE).get(CreationalContext.class.getName())).release();
    }
}
