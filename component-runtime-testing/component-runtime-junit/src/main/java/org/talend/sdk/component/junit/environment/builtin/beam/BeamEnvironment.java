/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.environment.builtin.beam;

import org.talend.sdk.component.junit.environment.ClassLoaderEnvironment;

public abstract class BeamEnvironment extends ClassLoaderEnvironment {

    @Override
    protected String[] rootDependencies() {
        return new String[] {
                rootDependencyBase() + ':' + System.getProperty("talend.junit.beam.version", Versions.BEAM_VERSION) };
    }

    @Override
    public String getName() {
        return super.getName().replace("RunnerEnvironment", "");
    }

    protected abstract String rootDependencyBase();
}
