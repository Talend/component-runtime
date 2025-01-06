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
package org.talend.sdk.component.runtime.standalone;

import java.lang.reflect.Method;

import org.talend.sdk.component.api.standalone.RunAtDriver;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.LifecycleImpl;

public class DriverRunnerImpl extends LifecycleImpl implements DriverRunner, Delegated {

    private transient Method runAtDriver;

    public DriverRunnerImpl(final String rootName, final String name, final String plugin, final Object delegate) {
        super(delegate, rootName, name, plugin);
    }

    public DriverRunnerImpl() {
        // no-op
    }

    @Override
    public void runAtDriver() {
        if (runAtDriver == null) {
            runAtDriver = findMethods(RunAtDriver.class).findFirst().get();
        }
        doInvoke(runAtDriver);
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }
}
