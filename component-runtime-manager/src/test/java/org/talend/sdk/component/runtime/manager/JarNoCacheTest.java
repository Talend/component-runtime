/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.URLConnection;

public abstract class JarNoCacheTest {

    private static boolean defaultUseCaches = true;

    private static URLConnection conn;

    @BeforeAll
    public static void setup() {
        conn = new URLConnection(null) {

            @Override
            public void connect() throws IOException {
                // No-op stub
            }
        };
        defaultUseCaches = conn.getDefaultUseCaches();
        conn.setDefaultUseCaches(false);
    }

    @AfterAll
    public static void resetUseCaches() {
        conn.setDefaultUseCaches(defaultUseCaches);
    }

}
