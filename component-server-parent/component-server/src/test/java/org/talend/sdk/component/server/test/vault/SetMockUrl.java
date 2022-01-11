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
package org.talend.sdk.component.server.test.vault;

import org.apache.catalina.startup.Tomcat;
import org.apache.meecrowave.Meecrowave;

import lombok.Setter;

public class SetMockUrl implements Meecrowave.MeecrowaveAwareInstanceCustomizer {

    @Setter
    private Meecrowave meecrowave;

    @Override
    public void accept(final Tomcat tomcat) {
        final int port = meecrowave.getConfiguration().getHttpPort();
        System.setProperty("talend.vault.cache.vault.auth.roleId", "Test-Role");
        System.setProperty("talend.vault.cache.vault.auth.secretId", "Test-Secret");
        System.setProperty("talend.vault.cache.vault.url", "http://localhost:" + port);
        System.setProperty("talend.vault.cache.vault.auth.endpoint", "/api/v1/mock/vault/login");
        System
                .setProperty("talend.vault.cache.vault.decrypt.endpoint",
                        "/api/v1/mock/vault/decrypt/{x-talend-tenant-id}");
    }
}
