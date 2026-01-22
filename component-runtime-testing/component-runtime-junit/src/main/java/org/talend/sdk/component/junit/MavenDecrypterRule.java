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
package org.talend.sdk.component.junit;

import static java.util.Optional.ofNullable;

import java.util.stream.Stream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.sdk.component.api.DecryptedServer;
import org.talend.sdk.component.base.BaseMavenDecrypter;
import org.talend.sdk.component.maven.Server;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MavenDecrypterRule implements TestRule {

    private final Object instance;

    private final BaseMavenDecrypter decrypter = new BaseMavenDecrypter();;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                doInject(instance, instance.getClass());
                base.evaluate();
            }
        };
    }

    private void doInject(final Object instance, final Class<?> base) {
        Stream.of(base.getDeclaredFields()).filter(f -> f.isAnnotationPresent(DecryptedServer.class)).forEach(f -> {
            final DecryptedServer annotation = f.getAnnotation(DecryptedServer.class);
            final Server server = decrypter.createInstance(annotation);
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            try {
                f.set(instance, server);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
        ofNullable(base.getSuperclass()).filter(s -> s != Object.class).ifPresent(c -> doInject(instance, c));
    }
}
