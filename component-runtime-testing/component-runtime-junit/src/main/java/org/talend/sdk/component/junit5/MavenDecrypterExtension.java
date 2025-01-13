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
package org.talend.sdk.component.junit5;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.talend.sdk.component.api.DecryptedServer;
import org.talend.sdk.component.base.BaseMavenDecrypter;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.maven.Server;

public class MavenDecrypterExtension implements JUnit5InjectionSupport {

    private final BaseMavenDecrypter decrypter = new BaseMavenDecrypter();

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return DecryptedServer.class;
    }

    @Override
    public boolean supports(final Class<?> type) {
        return Server.class == type;
    }

    @Override
    public Object findInstance(final ExtensionContext extensionContext, final Class<?> type, final Annotation marker)
            throws ParameterResolutionException {
        return decrypter.createInstance(DecryptedServer.class.cast(marker));
    }
}
